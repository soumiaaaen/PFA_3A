package ma.ensias.adminacademic.people.repository;

import ma.ensias.adminacademic.people.entity.GroupEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupEnrollmentRepository extends JpaRepository<GroupEnrollment, UUID> {

    boolean existsByStudentUserIdAndAcademicYearId(UUID studentUserId, UUID academicYearId);

    List<GroupEnrollment> findByStudentGroupIdOrderByStudentStudentNumber(UUID groupId);

    List<GroupEnrollment> findByStudentUserId(UUID studentUserId);

    Optional<GroupEnrollment> findByStudentGroupIdAndStudentUserId(UUID groupId, UUID studentUserId);
}