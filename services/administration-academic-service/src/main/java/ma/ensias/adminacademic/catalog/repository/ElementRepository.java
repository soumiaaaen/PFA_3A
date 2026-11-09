package ma.ensias.adminacademic.catalog.repository;

import ma.ensias.adminacademic.catalog.entity.Element;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ElementRepository extends JpaRepository<Element, UUID> {

    boolean existsByCodeIgnoreCase(String code);
}