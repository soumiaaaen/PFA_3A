package ma.ensias.content.internal;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.audit.AuditService;
import ma.ensias.content.common.exception.ConflictException;
import ma.ensias.content.common.exception.NotFoundException;
import ma.ensias.content.document.entity.*;
import ma.ensias.content.document.repository.DocumentAclRepository;
import ma.ensias.content.document.repository.DocumentRepository;
import ma.ensias.content.document.repository.DocumentVersionRepository;
import ma.ensias.content.document.service.DocumentService.Download;
import ma.ensias.content.document.service.Visibility;
import ma.ensias.content.document.service.VisibilityFactory;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import ma.ensias.content.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API interne utilisée par l'ai-service (Fatima) : l'IA n'indexe et ne cite QUE des documents publiés,
 * et vérifie les droits ACTUELS de l'étudiant avant chaque recherche (ENF-10).
 */
@Service
@RequiredArgsConstructor
public class InternalDocumentService {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final DocumentAclRepository acls;
    private final VisibilityFactory visibility;
    private final StorageService storage;
    private final AuditService audit;

    public record AccessResponse(UUID documentId, UUID userId, boolean allowed, int aclVersion, DocumentStatus status) {
    }

    public record AccessibleDocumentsResponse(UUID userId, List<UUID> documentIds) {
    }

    @Transactional(readOnly = true)
    public DocumentResponse describe(UUID documentId) {
        Document d = find(documentId);
        DocumentVersion current = d.getCurrentVersionId() == null ? null
                : versions.findById(d.getCurrentVersionId()).orElse(null);
        return DocumentResponse.from(d, current, groupIds(d));
    }

    @Transactional(readOnly = true)
    public AccessResponse access(UUID documentId, UUID userId) {
        Document d = find(documentId);
        boolean allowed = d.getStatus() == DocumentStatus.PUBLIE
                && visibility.forUser(userId).map(v -> v.canSee(d)).orElse(false);
        return new AccessResponse(d.getId(), userId, allowed, d.getAclVersion(), d.getStatus());
    }

    @Transactional(readOnly = true)
    public AccessibleDocumentsResponse accessibleDocuments(UUID userId) {
        Optional<Visibility> v = visibility.forUser(userId);
        List<UUID> ids = v.isEmpty() ? List.of()
                : documents.findByStatus(DocumentStatus.PUBLIE).stream()
                    .filter(d -> v.get().canSee(d))
                    .map(Document::getId)
                    .toList();
        return new AccessibleDocumentsResponse(userId, ids);
    }

    /** Contenu d'une version, pour l'indexation. Seulement pour un document publié. */
    @Transactional
    public Download content(UUID documentId, UUID versionId) {
        Document d = find(documentId);
        if (d.getStatus() != DocumentStatus.PUBLIE) {
            throw new ConflictException("Document non publié : il ne doit pas être indexé");
        }
        DocumentVersion v = versions.findById(versionId)
                .filter(x -> x.getDocument().getId().equals(documentId))
                .orElseThrow(() -> new NotFoundException("Version introuvable pour ce document"));
        StorageObject o = v.getStorageObject();
        audit.record("CONTENT_READ_BY_SERVICE", d.getId(), "version=" + v.getVersionNumber());
        return new Download(storage.get(o.getObjectKey()), o.getOriginalFilename(), o.getMimeType(), o.getSizeBytes());
    }

    private Document find(UUID id) {
        return documents.findById(id).orElseThrow(() -> new NotFoundException("Document introuvable"));
    }

    private List<UUID> groupIds(Document d) {
        return acls.findByDocumentId(d.getId()).stream().map(DocumentAcl::getGroupId).toList();
    }
}
