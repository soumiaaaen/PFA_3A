package ma.ensias.adminacademic.academic.repository;

import ma.ensias.adminacademic.academic.entity.Period;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PeriodRepository extends JpaRepository<Period, UUID> {
}