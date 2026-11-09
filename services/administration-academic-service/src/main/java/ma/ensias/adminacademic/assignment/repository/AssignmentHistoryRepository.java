package ma.ensias.adminacademic.assignment.repository;

import ma.ensias.adminacademic.assignment.entity.AssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentHistoryRepository extends JpaRepository<AssignmentHistory, UUID> {

    List<AssignmentHistory> findByAssignmentIdOrderByChangedAtAsc(UUID assignmentId);
}