package ma.ensias.content.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensias.content.audit.AuditService;
import ma.ensias.content.common.exception.BadRequestException;
import ma.ensias.content.common.exception.ConflictException;
import ma.ensias.content.common.exception.NotFoundException;
import ma.ensias.content.common.security.CurrentUser;
import ma.ensias.content.document.entity.*;
import ma.ensias.content.document.repository.*;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import ma.ensias.content.document.web.dto.DocumentDtos.UpdateDocumentRequest;
import ma.ensias.content.outbox.OutboxService;
import ma.ensias.content.storage.StorageService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Cycle de vie (conception §2.2) : métadonnées, groupes autorisés,
 * publier / dépublier / archiver / supprimer un brouillon.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicationService {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final DocumentAclRepository acls;
    private final DocumentCategoryRepository categories;
    private final StorageObjectRepository storageObjects;
    private final StorageService storage;
    private final AccessPolicy access;
    private final OutboxService outbox;
    private final AuditService audit;
    private final CurrentUser user;

    // ---------------------------------------------------------------- métadonnées

    @Transactional
    public DocumentResponse update(UUID id, UpdateDocumentRequest req) {
        Document d = findVisible(id);
        requireNotArchived(d);
        if (!access.canEditContent(d)) {
            throw new AccessDeniedException("Vous ne pouvez pas modifier ce document");
        }
        if (req.title() != null) {
            if (req.title().isBlank()) {
                throw new BadRequestException("Le titre ne peut pas être vide");
            }
            d.setTitle(req.title().strip());
        }
        if (req.description() != null) {
            d.setDescription(req.description().isBlank() ? null : req.description().strip());
        }
        if (req.category() != null) {
            d.setCategory(categories.findById(req.category().toUpperCase())
                    .orElseThrow(() -> new BadRequestException("Catégorie inconnue : " + req.category())));
        }
        audit.record("METADATA_UPDATED", d.getId(), "titre=" + d.getTitle() + ", categorie=" + d.getCategory().getCode());
        return response(d);
    }

    // ---------------------------------------------------------------- groupes autorisés

    @Transactional
    public DocumentResponse setGroups(UUID id, List<UUID> requested) {
        Document d = findVisible(id);
        requireNotArchived(d);
        if (!access.canPublish(d)) {
            throw new AccessDeniedException("Vous ne pouvez pas changer les groupes de ce document");
        }
        List<UUID> wanted = List.copyOf(new LinkedHashSet<>(requested));
        List<UUID> allowed = access.allowedGroups(d);
        List<UUID> refused = wanted.stream().filter(g -> !allowed.contains(g)).toList();
        if (!refused.isEmpty()) {
            throw new BadRequestException("Groupes non autorisés pour cet élément : " + refused);
        }
        if (d.getStatus() == DocumentStatus.PUBLIE && wanted.isEmpty()) {
            throw new BadRequestException("Un document publié doit garder au moins un groupe (sinon, dépubliez-le)");
        }

        List<UUID> before = groupIds(d);
        List<UUID> added = wanted.stream().filter(g -> !before.contains(g)).toList();
        List<UUID> removed = before.stream().filter(g -> !wanted.contains(g)).toList();
        if (added.isEmpty() && removed.isEmpty()) {
            return response(d);
        }
        replaceAcl(d, wanted);
        d.setAclVersion(d.getAclVersion() + 1);

        if (d.getStatus() == DocumentStatus.PUBLIE) {
            if (!removed.isEmpty()) {
                outbox.accessRevoked(d, "GROUPS_REMOVED", removed, wanted);
            }
            if (!added.isEmpty()) {
                outbox.documentPublished(d, currentVersion(d), wanted, user.id());
            }
        }
        audit.record("GROUPS_UPDATED", d.getId(), "ajoutes=" + added + ", retires=" + removed);
        return response(d);
    }

    // ---------------------------------------------------------------- publication

    @Transactional
    public DocumentResponse publish(UUID id) {
        Document d = findVisible(id);
        if (!access.canPublish(d)) {
            throw new AccessDeniedException("Vous ne pouvez pas publier ce document");
        }
        if (d.getStatus() != DocumentStatus.BROUILLON) {
            throw new ConflictException("Seul un brouillon peut être publié (statut actuel : " + d.getStatus() + ")");
        }
        List<UUID> groups = groupIds(d);
        if (groups.isEmpty()) {
            // Par défaut : les groupes que l'utilisateur peut autoriser (ses groupes pour cet élément)
            groups = access.allowedGroups(d);
            if (groups.isEmpty()) {
                throw new ConflictException("Aucun groupe à qui publier : choisissez d'abord les groupes");
            }
            replaceAcl(d, groups);
        }
        d.setStatus(DocumentStatus.PUBLIE);
        d.setPublishedAt(Instant.now());
        d.setAclVersion(d.getAclVersion() + 1);
        outbox.documentPublished(d, currentVersion(d), groups, user.id());
        audit.record("PUBLISHED", d.getId(), "groupes=" + groups);
        return response(d);
    }

    @Transactional
    public DocumentResponse unpublish(UUID id) {
        Document d = findVisible(id);
        if (!access.canPublish(d)) {
            throw new AccessDeniedException("Vous ne pouvez pas dépublier ce document");
        }
        if (d.getStatus() != DocumentStatus.PUBLIE) {
            throw new ConflictException("Le document n'est pas publié");
        }
        d.setStatus(DocumentStatus.BROUILLON);
        d.setAclVersion(d.getAclVersion() + 1);
        outbox.accessRevoked(d, "UNPUBLISHED", groupIds(d), List.of());
        audit.record("UNPUBLISHED", d.getId(), null);
        return response(d);
    }

    @Transactional
    public DocumentResponse archive(UUID id) {
        Document d = findVisible(id);
        if (!access.canArchive(d)) {
            throw new AccessDeniedException("Vous ne pouvez pas archiver ce document");
        }
        requireNotArchived(d);
        boolean wasPublished = d.getStatus() == DocumentStatus.PUBLIE;
        d.setStatus(DocumentStatus.ARCHIVE);
        d.setArchivedAt(Instant.now());
        d.setAclVersion(d.getAclVersion() + 1);
        if (wasPublished) {
            outbox.accessRevoked(d, "ARCHIVED", groupIds(d), List.of());
        }
        audit.record("ARCHIVED", d.getId(), null);
        return response(d);
    }

    /** Décision 4 : suppression définitive seulement d'un brouillon jamais publié, par son auteur. */
    @Transactional
    public void deleteDraft(UUID id) {
        Document d = findVisible(id);
        if (!d.getOwnerUserId().equals(user.id())) {
            throw new AccessDeniedException("Seul l'auteur peut supprimer son brouillon");
        }
        if (d.getStatus() != DocumentStatus.BROUILLON || d.getPublishedAt() != null) {
            throw new ConflictException("Un document déjà publié ne peut pas être supprimé : archivez-le");
        }
        List<DocumentVersion> all = versions.findByDocumentIdOrderByVersionNumberDesc(id);
        List<StorageObject> objects = all.stream().map(DocumentVersion::getStorageObject).toList();
        List<String> keys = objects.stream().map(StorageObject::getObjectKey).toList();

        d.setCurrentVersionId(null);
        acls.deleteAll(acls.findByDocumentId(id));
        versions.deleteAll(all);
        storageObjects.deleteAll(objects);
        documents.delete(d);
        audit.record("DRAFT_DELETED", id, "titre=" + d.getTitle() + ", versions=" + all.size());

        // Les fichiers MinIO sont supprimés seulement si la transaction réussit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String key : keys) {
                    try {
                        storage.delete(key);
                    } catch (RuntimeException e) {
                        log.warn("Fichier orphelin dans MinIO : {}", key);
                    }
                }
            }
        });
    }

    // ---------------------------------------------------------------- outils

    private void replaceAcl(Document d, List<UUID> groupIds) {
        acls.deleteAll(acls.findByDocumentId(d.getId()));
        acls.flush();
        List<DocumentAcl> rows = new ArrayList<>();
        for (UUID g : groupIds) {
            DocumentAcl a = new DocumentAcl();
            a.setDocument(d);
            a.setGroupId(g);
            a.setGrantedBy(user.id());
            rows.add(a);
        }
        acls.saveAll(rows);
    }

    private List<UUID> groupIds(Document d) {
        return acls.findByDocumentId(d.getId()).stream().map(DocumentAcl::getGroupId).toList();
    }

    private DocumentVersion currentVersion(Document d) {
        return versions.findById(d.getCurrentVersionId())
                .orElseThrow(() -> new ConflictException("Document sans fichier"));
    }

    private void requireNotArchived(Document d) {
        if (d.getStatus() == DocumentStatus.ARCHIVE) {
            throw new ConflictException("Document archivé : aucune modification possible");
        }
    }

    private Document findVisible(UUID id) {
        Document d = documents.findById(id).orElseThrow(() -> new NotFoundException("Document introuvable"));
        if (!access.canView(d)) {
            throw new NotFoundException("Document introuvable");
        }
        return d;
    }

    private DocumentResponse response(Document d) {
        DocumentVersion current = d.getCurrentVersionId() == null ? null
                : versions.findById(d.getCurrentVersionId()).orElse(null);
        return DocumentResponse.from(d, current, groupIds(d));
    }
}
