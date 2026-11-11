package ma.ensias.content.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensias.content.audit.AuditService;
import ma.ensias.content.common.config.StorageProperties;
import ma.ensias.content.common.exception.BadRequestException;
import ma.ensias.content.common.exception.ConflictException;
import ma.ensias.content.common.exception.NotFoundException;
import ma.ensias.content.common.security.CurrentUser;
import ma.ensias.content.document.entity.*;
import ma.ensias.content.document.repository.*;
import ma.ensias.content.document.service.FileInspector.InspectedFile;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import ma.ensias.content.document.web.dto.DocumentDtos.VersionResponse;
import ma.ensias.content.integration.AdminAcademicClient;
import ma.ensias.content.integration.AdminAcademicViews.ElementView;
import ma.ensias.content.outbox.OutboxService;
import ma.ensias.content.storage.StorageService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/** Dépôt, versions et téléchargement des documents (étape 5). */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final DocumentCategoryRepository categories;
    private final DocumentAclRepository acls;
    private final OutboxService outbox;
    private final StorageObjectRepository storageObjects;
    private final StorageService storage;
    private final StorageProperties storageProps;
    private final FileInspector inspector;
    private final AccessPolicy access;
    private final AdminAcademicClient adminAcademic;
    private final AuditService audit;
    private final CurrentUser user;

    /** Fichier prêt à être renvoyé au navigateur. */
    public record Download(InputStream content, String filename, String mimeType, long size) {
    }

    // ---------------------------------------------------------------- dépôt

    @Transactional
    public DocumentResponse create(UUID elementId, String categoryCode, String title, String description,
                                   MultipartFile file) {
        if (!user.hasRole("ENSEIGNANT")) {
            throw new AccessDeniedException("Seul un enseignant peut déposer un document");
        }
        ElementView element = adminAcademic.element(elementId);
        UUID periodId = element.period().id();
        if (!access.isAssignedTeacher(elementId, periodId)) {
            throw new AccessDeniedException("Vous n'êtes pas affecté(e) à l'élément " + element.code());
        }
        DocumentCategory category = categories.findById(categoryCode.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Catégorie inconnue : " + categoryCode));

        Document d = new Document();
        d.setElementId(elementId);
        d.setProgramId(element.programId());
        d.setPeriodId(periodId);
        d.setCategory(category);
        d.setTitle(title.strip());
        d.setDescription(description == null || description.isBlank() ? null : description.strip());
        d.setOwnerUserId(user.id());
        documents.saveAndFlush(d);

        DocumentVersion v = storeVersion(d, file, null);
        audit.record("DOCUMENT_CREATED", d.getId(),
                "titre=" + d.getTitle() + ", element=" + element.code() + ", categorie=" + category.getCode());
        return DocumentResponse.from(d, v, List.of());
    }

    @Transactional
    public VersionResponse addVersion(UUID documentId, MultipartFile file, String comment) {
        Document d = findVisible(documentId);
        if (d.getStatus() == DocumentStatus.ARCHIVE) {
            throw new ConflictException("Document archivé : nouvelle version impossible");
        }
        if (!access.canEditContent(d)) {
            throw new AccessDeniedException("Vous ne pouvez pas modifier ce document");
        }
        DocumentVersion v = storeVersion(d, file, comment);
        audit.record("VERSION_ADDED", d.getId(), "version=" + v.getVersionNumber());
        if (d.getStatus() == DocumentStatus.PUBLIE) {
            // Nouvelle version d'un document publié : l'IA doit réindexer (EVENTS.md)
            List<UUID> groups = acls.findByDocumentId(d.getId()).stream().map(DocumentAcl::getGroupId).toList();
            outbox.documentPublished(d, v, groups, user.id());
        }
        return VersionResponse.from(v);
    }

    /** Vérifie le fichier, l'envoie dans MinIO, puis l'enregistre en base. */
    private DocumentVersion storeVersion(Document d, MultipartFile file, String comment) {
        InspectedFile f = inspector.inspect(file);
        try {
            int number = versions.findMaxVersionNumber(d.getId()) + 1;
            String key = "documents/" + d.getId() + "/v" + number + "/" + UUID.randomUUID() + "." + f.type().extension;

            try (InputStream in = Files.newInputStream(f.tempFile())) {
                storage.put(key, in, f.size(), f.type().mimeType);
            }
            deleteObjectIfRollback(key);

            StorageObject o = new StorageObject();
            o.setBucket(storageProps.bucket());
            o.setObjectKey(key);
            o.setOriginalFilename(f.originalFilename());
            o.setMimeType(f.type().mimeType);
            o.setSizeBytes(f.size());
            o.setSha256(f.sha256());
            storageObjects.save(o);

            DocumentVersion v = new DocumentVersion();
            v.setDocument(d);
            v.setVersionNumber(number);
            v.setStorageObject(o);
            v.setComment(comment == null || comment.isBlank() ? null : comment.strip());
            v.setUploadedBy(user.id());
            versions.save(v);

            d.setCurrentVersionId(v.getId());
            return v;
        } catch (IOException e) {
            throw new BadRequestException("Lecture du fichier impossible");
        } finally {
            inspector.deleteQuietly(f.tempFile());
        }
    }

    /** Si la transaction échoue après l'envoi dans MinIO, on supprime le fichier envoyé (pas de fichier orphelin). */
    private void deleteObjectIfRollback(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storage.delete(key);
                    } catch (RuntimeException e) {
                        log.warn("Fichier orphelin dans MinIO : {}", key);
                    }
                }
            }
        });
    }

    // ---------------------------------------------------------------- lecture

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID documentId) {
        Document d = findVisible(documentId);
        DocumentVersion current = d.getCurrentVersionId() == null ? null
                : versions.findById(d.getCurrentVersionId()).orElse(null);
        List<UUID> groups = acls.findByDocumentId(d.getId()).stream().map(DocumentAcl::getGroupId).toList();
        return DocumentResponse.from(d, current, groups);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> versions(UUID documentId) {
        Document d = findVisible(documentId);
        if (!access.canSeeEverything(d)) {
            throw new AccessDeniedException("L'historique des versions est réservé aux enseignants et responsables");
        }
        return versions.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(VersionResponse::from).toList();
    }

    @Transactional
    public Download downloadCurrent(UUID documentId) {
        Document d = findVisible(documentId);
        if (d.getCurrentVersionId() == null) {
            throw new NotFoundException("Document sans fichier");
        }
        DocumentVersion v = versions.findById(d.getCurrentVersionId())
                .orElseThrow(() -> new NotFoundException("Version introuvable"));
        return download(d, v);
    }

    @Transactional
    public Download downloadVersion(UUID documentId, int versionNumber) {
        Document d = findVisible(documentId);
        if (!access.canSeeEverything(d)) {
            throw new AccessDeniedException("Les anciennes versions sont réservées aux enseignants et responsables");
        }
        DocumentVersion v = versions.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .filter(x -> x.getVersionNumber() == versionNumber).findFirst()
                .orElseThrow(() -> new NotFoundException("Version " + versionNumber + " introuvable"));
        return download(d, v);
    }

    private Download download(Document d, DocumentVersion v) {
        StorageObject o = v.getStorageObject();
        audit.record("DOWNLOADED", d.getId(), "version=" + v.getVersionNumber());
        return new Download(storage.get(o.getObjectKey()), o.getOriginalFilename(), o.getMimeType(), o.getSizeBytes());
    }

    /** Document que l'utilisateur a le droit de voir ; sinon 404 (décision 3 : ne pas révéler son existence). */
    private Document findVisible(UUID documentId) {
        Document d = documents.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document introuvable"));
        if (!access.canView(d)) {
            throw new NotFoundException("Document introuvable");
        }
        return d;
    }
}
