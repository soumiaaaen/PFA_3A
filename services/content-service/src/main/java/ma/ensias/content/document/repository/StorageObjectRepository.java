package ma.ensias.content.document.repository;

import ma.ensias.content.document.entity.StorageObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StorageObjectRepository extends JpaRepository<StorageObject, UUID> {
}
