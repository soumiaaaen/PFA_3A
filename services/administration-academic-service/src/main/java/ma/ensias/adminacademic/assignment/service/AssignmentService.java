package ma.ensias.adminacademic.assignment.service;
import ma.ensias.adminacademic.audit.service.AuditService;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.entity.Period;
import ma.ensias.adminacademic.academic.entity.StudentGroup;
import ma.ensias.adminacademic.academic.repository.StudentGroupRepository;
import ma.ensias.adminacademic.assignment.entity.AssignmentHistory;
import ma.ensias.adminacademic.assignment.entity.HistoryAction;
import ma.ensias.adminacademic.assignment.entity.TeachingAssignment;
import ma.ensias.adminacademic.assignment.repository.AssignmentHistoryRepository;
import ma.ensias.adminacademic.assignment.repository.TeachingAssignmentRepository;
import ma.ensias.adminacademic.assignment.web.dto.*;
import ma.ensias.adminacademic.catalog.entity.Element;
import ma.ensias.adminacademic.catalog.entity.TeachingModule;
import ma.ensias.adminacademic.catalog.repository.ElementRepository;
import ma.ensias.adminacademic.common.exception.BadRequestException;
import ma.ensias.adminacademic.common.exception.ConflictException;
import ma.ensias.adminacademic.common.exception.NotFoundException;
import ma.ensias.adminacademic.common.security.ScopeService;
import ma.ensias.adminacademic.identity.entity.RoleCode;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final TeachingAssignmentRepository assignmentRepository;
    private final AssignmentHistoryRepository historyRepository;
    private final ElementRepository elementRepository;
    private final StudentGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ScopeService scopeService;
        private final AuditService auditService;

    @Transactional
    public AssignmentResponse create(CreateAssignmentRequest req) {
        Element element = elementRepository.findById(req.elementId())
                .orElseThrow(() -> new NotFoundException("Élément introuvable : " + req.elementId()));
        TeachingModule module = element.getModule();
        scopeService.checkCanManageProgram(module.getProgram().getHeadUserId());

        StudentGroup group = groupRepository.findById(req.groupId())
                .orElseThrow(() -> new NotFoundException("Groupe introuvable : " + req.groupId()));
        Period period = element.getPeriod();
        checkGroupMatches(module, period, group);

        User teacher = getTeacher(req.teacherUserId());

        if (assignmentRepository.existsByElementIdAndTeacherIdAndStudentGroupIdAndActivityTypeAndActiveTrue(
                element.getId(), teacher.getId(), group.getId(), req.activityType())) {
            throw new ConflictException("Cette affectation existe déjà");
        }

        TeachingAssignment a = new TeachingAssignment();
        a.setElement(element);
        a.setTeacher(teacher);
        a.setStudentGroup(group);
        a.setPeriod(period);
        a.setActivityType(req.activityType());
        a.setTargetHours(req.targetHours());
        a = assignmentRepository.save(a);

        record(a, HistoryAction.CREATED, null, teacher.getId(), null, req.targetHours());
        return AssignmentResponse.from(a);
    }

    @Transactional
    public AssignmentResponse update(UUID id, UpdateAssignmentRequest req) {
        TeachingAssignment a = getActive(id);
        scopeService.checkCanManageProgram(a.getElement().getModule().getProgram().getHeadUserId());

        User newTeacher = getTeacher(req.teacherUserId());
        UUID oldTeacherId = a.getTeacher().getId();
        BigDecimal oldHours = a.getTargetHours();

        if (!oldTeacherId.equals(newTeacher.getId())
                && assignmentRepository.existsByElementIdAndTeacherIdAndStudentGroupIdAndActivityTypeAndActiveTrue(
                        a.getElement().getId(), newTeacher.getId(), a.getStudentGroup().getId(), a.getActivityType())) {
            throw new ConflictException("Cet enseignant a déjà cette affectation");
        }

        a.setTeacher(newTeacher);
        a.setTargetHours(req.targetHours());
        record(a, HistoryAction.UPDATED, oldTeacherId, newTeacher.getId(), oldHours, req.targetHours());
        return AssignmentResponse.from(a);
    }

    @Transactional
    public void deactivate(UUID id) {
        TeachingAssignment a = getActive(id);
        scopeService.checkCanManageProgram(a.getElement().getModule().getProgram().getHeadUserId());
        a.setActive(false);
        record(a, HistoryAction.DEACTIVATED, a.getTeacher().getId(), null, a.getTargetHours(), null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(UUID periodId, UUID teacherId, UUID groupId, UUID elementId) {
        // Un enseignant ne voit que ses propres affectations
        UUID teacherFilter = isTeacherOnly() ? scopeService.currentUserId() : teacherId;

        return assignmentRepository.findByPeriodIdAndActiveTrue(periodId).stream()
                .filter(a -> teacherFilter == null || a.getTeacher().getId().equals(teacherFilter))
                .filter(a -> groupId == null || a.getStudentGroup().getId().equals(groupId))
                .filter(a -> elementId == null || a.getElement().getId().equals(elementId))
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(UUID id) {
        if (!assignmentRepository.existsById(id)) {
            throw new NotFoundException("Affectation introuvable : " + id);
        }
        return historyRepository.findByAssignmentIdOrderByChangedAtAsc(id).stream()
                .map(HistoryResponse::from).toList();
    }

    // ---------- Règles et outils privés ----------

    private void checkGroupMatches(TeachingModule module, Period period, StudentGroup group) {
        if (!group.getProgram().getId().equals(module.getProgram().getId())) {
            throw new BadRequestException("Le groupe n'appartient pas à la filière du module");
        }
        if (!group.getStudyLevel().equals(module.getStudyLevel())) {
            throw new BadRequestException("Le niveau du groupe ne correspond pas à celui du module");
        }
        if (!group.getAcademicYear().getId().equals(period.getSemester().getAcademicYear().getId())) {
            throw new BadRequestException("Le groupe et la période ne sont pas de la même année universitaire");
        }
    }

    private User getTeacher(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + userId));
        if (!user.isActive()) {
            throw new BadRequestException("Le compte de l'enseignant est désactivé");
        }
        boolean isTeacher = user.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.ENSEIGNANT);
        if (!isTeacher) {
            throw new BadRequestException("L'utilisateur doit avoir le rôle ENSEIGNANT");
        }
        return user;
    }

    private TeachingAssignment getActive(UUID id) {
        TeachingAssignment a = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Affectation introuvable : " + id));
        if (!a.isActive()) {
            throw new BadRequestException("Cette affectation est désactivée");
        }
        return a;
    }

    private boolean isTeacherOnly() {
        return !scopeService.hasRole("ADMIN")
                && !scopeService.hasRole("CHEF_FILIERE")
                && !scopeService.hasRole("CHEF_DEPARTEMENT");
    }

        private void record(TeachingAssignment a, HistoryAction action,
                        UUID oldTeacher, UUID newTeacher, BigDecimal oldHours, BigDecimal newHours) {
        AssignmentHistory h = new AssignmentHistory();
        h.setAssignment(a);
        h.setAction(action);
        h.setOldTeacherUserId(oldTeacher);
        h.setNewTeacherUserId(newTeacher);
        h.setOldTargetHours(oldHours);
        h.setNewTargetHours(newHours);
        h.setChangedBy(scopeService.currentUserId());
        historyRepository.save(h);

        auditService.record("ASSIGNMENT_" + action, "ASSIGNMENT", a.getId(),
                "element=" + a.getElement().getCode()
                        + ", group=" + a.getStudentGroup().getName()
                        + ", teacher=" + newTeacher + ", hours=" + newHours);
    }
}