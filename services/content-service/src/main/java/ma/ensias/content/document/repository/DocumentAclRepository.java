package ma.ensias.content.document.repository;

import ma.ensias.content.document.entity.DocumentAcl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentAclRepository extends JpaRepository<DocumentAcl, UUID> {

    List<DocumentAcl> findByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    /** Ids des documents visibles par au moins un des groupes donnés. */
    @Query("select distinct a.document.id from DocumentAcl a where a.groupId in :groupIds")
    List<UUID> findDocumentIdsByGroupIds(Collection<UUID> groupIds);
}
