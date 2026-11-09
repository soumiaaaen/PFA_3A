package ma.ensias.adminacademic.academic.repository;

import ma.ensias.adminacademic.academic.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SemesterRepository extends JpaRepository<Semester, UUID> {

    boolean existsByAcademicYearIdAndNumber(UUID academicYearId, Integer number);

    List<Semester> findByAcademicYearIdOrderByNumber(UUID academicYearId);
}