package ma.ensias.adminacademic.catalog.repository;

import ma.ensias.adminacademic.catalog.entity.TeachingModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeachingModuleRepository extends JpaRepository<TeachingModule, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<TeachingModule> findByProgramIdOrderByStudyLevelAscCodeAsc(UUID programId);
}