package ma.ensias.content.document.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentAcl;
import ma.ensias.content.document.entity.DocumentStatus;
import ma.ensias.content.document.entity.DocumentVersion;
import ma.ensias.content.document.repository.DocumentAclRepository;
import ma.ensias.content.document.repository.DocumentRepository;
import ma.ensias.content.document.repository.DocumentVersionRepository;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Liste des documents filtrée selon les droits de l'utilisateur connecté.
 * Les règles sont dans Visibility (chargées une fois par requête).
 */
@Service
@RequiredArgsConstructor
public class DocumentQueryService {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final DocumentAclRepository acls;
    private final VisibilityFactory visibility;

    public record Filters(UUID elementId, UUID programId, UUID periodId, String category,
                          DocumentStatus status, String q) {
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Filters f) {
        Visibility v = visibility.forCurrentUser();
        return documents.findAll(spec(f), Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                // Les archives n'apparaissent que si on les demande explicitement
                .filter(d -> f.status() == DocumentStatus.ARCHIVE || d.getStatus() != DocumentStatus.ARCHIVE)
                .filter(v::canSee)
                .map(this::toResponse)
                .toList();
    }

    private Specification<Document> spec(Filters f) {
        List<Specification<Document>> parts = new ArrayList<>();
        if (f.elementId() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("elementId"), f.elementId()));
        }
        if (f.programId() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("programId"), f.programId()));
        }
        if (f.periodId() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("periodId"), f.periodId()));
        }
        if (f.category() != null && !f.category().isBlank()) {
            parts.add((r, q, cb) -> cb.equal(r.get("category").get("code"), f.category().toUpperCase()));
        }
        if (f.status() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("status"), f.status()));
        }
        if (f.q() != null && !f.q().isBlank()) {
            String like = "%" + f.q().strip().toLowerCase() + "%";
            parts.add((r, q, cb) -> cb.like(cb.lower(r.<String>get("title")), like));
        }
        return Specification.allOf(parts);
    }

    private DocumentResponse toResponse(Document d) {
        DocumentVersion current = d.getCurrentVersionId() == null ? null
                : versions.findById(d.getCurrentVersionId()).orElse(null);
        List<UUID> groups = acls.findByDocumentId(d.getId()).stream().map(DocumentAcl::getGroupId).toList();
        return DocumentResponse.from(d, current, groups);
    }
}
