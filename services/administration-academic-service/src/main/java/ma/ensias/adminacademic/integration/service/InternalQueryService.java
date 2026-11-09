package ma.ensias.adminacademic.integration.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.repository.PeriodRepository;
import ma.ensias.adminacademic.academic.repository.StudentGroupRepository;
import ma.ensias.adminacademic.assignment.repository.TeachingAssignmentRepository;
import ma.ensias.adminacademic.catalog.repository.ElementRepository;
import ma.ensias.adminacademic.common.exception.NotFoundException;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import ma.ensias.adminacademic.integration.web.InternalViews.*;
import ma.ensias.adminacademic.people.repository.GroupEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalQueryService {

    private final UserRepository userRepository;
    private final PeriodRepository periodRepository;
    private final ElementRepository elementRepository;
    private final StudentGroupRepository groupRepository;
    private final TeachingAssignmentRepository assignmentRepository;
    private final GroupEnrollmentRepository enrollmentRepository;

    public UserView user(UUID id) {
        return userRepository.findById(id).map(UserView::from)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
    }

    public PeriodView period(UUID id) {
        return periodRepository.findById(id).map(PeriodView::from)
                .orElseThrow(() -> new NotFoundException("Période introuvable : " + id));
    }

    public ElementView element(UUID id) {
        return elementRepository.findById(id).map(ElementView::from)
                .orElseThrow(() -> new NotFoundException("Élément introuvable : " + id));
    }

    public GroupView group(UUID id) {
        return groupRepository.findById(id).map(GroupView::from)
                .orElseThrow(() -> new NotFoundException("Groupe introuvable : " + id));
    }

    public List<AssignmentView> assignments(UUID periodId, UUID teacherId, UUID groupId, UUID elementId) {
        return assignmentRepository.findByPeriodIdAndActiveTrue(periodId).stream()
                .filter(a -> teacherId == null || a.getTeacher().getId().equals(teacherId))
                .filter(a -> groupId == null || a.getStudentGroup().getId().equals(groupId))
                .filter(a -> elementId == null || a.getElement().getId().equals(elementId))
                .map(AssignmentView::from)
                .toList();
    }

    public AssignmentCheckView checkAssignment(UUID elementId, UUID teacherId, UUID groupId) {
        List<AssignmentView> found = assignmentRepository
                .findByElementIdAndTeacherIdAndStudentGroupIdAndActiveTrue(elementId, teacherId, groupId)
                .stream().map(AssignmentView::from).toList();
        return new AssignmentCheckView(!found.isEmpty(), found);
    }

    // Les étudiants d'un groupe (absences, notifications)
    public List<UUID> groupStudentIds(UUID groupId) {
        return enrollmentRepository.findByStudentGroupIdOrderByStudentStudentNumber(groupId).stream()
                .map(e -> e.getStudent().getUserId())
                .toList();
    }

    // Les groupes d'un étudiant (emploi du temps, documents autorisés)
    public List<GroupView> studentGroups(UUID studentUserId) {
        return enrollmentRepository.findByStudentUserId(studentUserId).stream()
                .map(e -> GroupView.from(e.getStudentGroup()))
                .toList();
    }
}