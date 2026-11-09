package ma.ensias.adminacademic.academic.repository;

import ma.ensias.adminacademic.academic.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, UUID> {

    boolean existsByProgramIdAndAcademicYearIdAndStudyLevelAndNameIgnoreCase(
            UUID programId, UUID academicYearId, Integer studyLevel, String name);

    List<StudentGroup> findByProgramIdAndAcademicYearIdOrderByStudyLevelAscNameAsc(
            UUID programId, UUID academicYearId);
}