package ma.ensias.adminacademic.academic.repository;

import ma.ensias.adminacademic.academic.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<Program> findAllByOrderByCode();

    List<Program> findByDepartmentIdOrderByCode(UUID departmentId);
}