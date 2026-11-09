package ma.ensias.adminacademic.academic.repository;

import ma.ensias.adminacademic.academic.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<Department> findAllByOrderByCode();
}