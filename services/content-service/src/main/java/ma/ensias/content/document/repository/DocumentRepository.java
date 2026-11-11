package ma.ensias.content.document.repository;

import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    List<Document> findByStatus(DocumentStatus status);
}
