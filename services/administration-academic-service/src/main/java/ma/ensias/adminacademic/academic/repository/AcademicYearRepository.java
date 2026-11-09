package ma.ensias.adminacademic.academic.repository;

import ma.ensias.adminacademic.academic.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    boolean existsByLabel(String label);

    List<AcademicYear> findAllByOrderByStartDateDesc();
}