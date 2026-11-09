package ma.ensias.adminacademic.people.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.entity.StudentGroup;
import ma.ensias.adminacademic.academic.repository.StudentGroupRepository;
import ma.ensias.adminacademic.common.exception.BadRequestException;
import ma.ensias.adminacademic.common.exception.ConflictException;
import ma.ensias.adminacademic.common.exception.NotFoundException;
import ma.ensias.adminacademic.common.security.ScopeService;
import ma.ensias.adminacademic.identity.entity.RoleCode;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import ma.ensias.adminacademic.people.entity.GroupEnrollment;
import ma.ensias.adminacademic.people.entity.Student;
import ma.ensias.adminacademic.people.repository.GroupEnrollmentRepository;
import ma.ensias.adminacademic.people.repository.StudentRepository;
import ma.ensias.adminacademic.people.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ma.ensias.adminacademic.audit.service.AuditService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final GroupEnrollmentRepository enrollmentRepository;
    private final StudentGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ScopeService scopeService;
        private final AuditService auditService;

    // ---------- Profils étudiants ----------

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest req) {
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + req.userId()));
        boolean isStudent = user.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.ETUDIANT);
        if (!isStudent) {
            throw new BadRequestException("L'utilisateur doit avoir le rôle ETUDIANT");
        }
        if (studentRepository.existsById(user.getId())) {
            throw new ConflictException("Cet utilisateur a déjà un profil étudiant");
        }
        String number = req.studentNumber().trim().toUpperCase();
        if (studentRepository.existsByStudentNumberIgnoreCase(number)) {
            throw new ConflictException("Le numéro d'étudiant " + number + " est déjà utilisé");
        }
        Student s = new Student();
        s.setUser(user);
        s.setStudentNumber(number);
        return StudentResponse.from(studentRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents() {
        return studentRepository.findAll().stream().map(StudentResponse::from).toList();
    }

    // ---------- Inscriptions dans les groupes ----------

    @Transactional
    public EnrollmentResponse enroll(UUID groupId, EnrollStudentRequest req) {
        StudentGroup group = getGroup(groupId);
        scopeService.checkCanManageProgram(group.getProgram().getHeadUserId());

        Student student = studentRepository.findById(req.studentUserId())
                .orElseThrow(() -> new NotFoundException("Profil étudiant introuvable : " + req.studentUserId()));

        if (enrollmentRepository.existsByStudentUserIdAndAcademicYearId(
                student.getUserId(), group.getAcademicYear().getId())) {
            throw new ConflictException("Cet étudiant est déjà inscrit dans un groupe pour "
                    + group.getAcademicYear().getLabel());
        }
        GroupEnrollment e = new GroupEnrollment();
        e.setStudent(student);
        e.setStudentGroup(group);
        e.setAcademicYear(group.getAcademicYear());
                GroupEnrollment saved = enrollmentRepository.save(e);
        auditService.record("STUDENT_ENROLLED", "GROUP", group.getId(),
                "student=" + student.getStudentNumber());
        return EnrollmentResponse.from(saved);
    }

    @Transactional
    public void unenroll(UUID groupId, UUID studentUserId) {
        StudentGroup group = getGroup(groupId);
        scopeService.checkCanManageProgram(group.getProgram().getHeadUserId());
        GroupEnrollment e = enrollmentRepository.findByStudentGroupIdAndStudentUserId(groupId, studentUserId)
                .orElseThrow(() -> new NotFoundException("Cet étudiant n'est pas inscrit dans ce groupe"));
        enrollmentRepository.delete(e);
                auditService.record("STUDENT_UNENROLLED", "GROUP", groupId,
                "student=" + e.getStudent().getStudentNumber());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listGroupStudents(UUID groupId) {
        getGroup(groupId);
        return enrollmentRepository.findByStudentGroupIdOrderByStudentStudentNumber(groupId).stream()
                .map(EnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> myGroups() {
        return enrollmentRepository.findByStudentUserId(scopeService.currentUserId()).stream()
                .map(EnrollmentResponse::from).toList();
    }

    private StudentGroup getGroup(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Groupe introuvable : " + id));
    }
}