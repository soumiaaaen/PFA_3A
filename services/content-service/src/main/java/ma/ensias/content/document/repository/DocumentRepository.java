package ma.ensias.content.document.repository;

import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByElementIdInOrderByUpdatedAtDesc(Collection<UUID> elementIds);

    List<Document> findByProgramIdInOrderByUpdatedAtDesc(Collection<UUID> programIds);

    List<Document> findByStatusAndIdInOrderByPublishedAtDesc(DocumentStatus status, Collection<UUID> ids);
}
