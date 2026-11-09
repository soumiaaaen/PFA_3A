package ma.ensias.adminacademic.assignment.repository;

import ma.ensias.adminacademic.assignment.entity.ActivityType;
import ma.ensias.adminacademic.assignment.entity.TeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, UUID> {

    boolean existsByElementIdAndTeacherIdAndStudentGroupIdAndActivityTypeAndActiveTrue(
            UUID elementId, UUID teacherId, UUID groupId, ActivityType activityType);

    List<TeachingAssignment> findByPeriodIdAndActiveTrue(UUID periodId);
        List<TeachingAssignment> findByElementIdAndTeacherIdAndStudentGroupIdAndActiveTrue(
            UUID elementId, UUID teacherId, UUID groupId);
}