package ma.ensias.adminacademic.people.repository;

import ma.ensias.adminacademic.people.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    boolean existsByStudentNumberIgnoreCase(String studentNumber);
}