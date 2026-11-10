package ma.ensias.content.document.repository;

import ma.ensias.content.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    @Query("select coalesce(max(v.versionNumber), 0) from DocumentVersion v where v.document.id = :documentId")
    int findMaxVersionNumber(UUID documentId);
}
