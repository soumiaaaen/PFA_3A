package ma.ensias.content.document.web.dto;

import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentStatus;
import ma.ensias.content.document.entity.DocumentVersion;
import ma.ensias.content.document.entity.StorageObject;

import java.time.Instant;
import java.util.UUID;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record VersionResponse(UUID id, int versionNumber, String originalFilename, String mimeType,
                                  long sizeBytes, String sha256, String comment, UUID uploadedBy,
                                  Instant uploadedAt) {
        public static VersionResponse from(DocumentVersion v) {
            StorageObject o = v.getStorageObject();
            return new VersionResponse(v.getId(), v.getVersionNumber(), o.getOriginalFilename(), o.getMimeType(),
                    o.getSizeBytes(), o.getSha256(), v.getComment(), v.getUploadedBy(), v.getUploadedAt());
        }
    }

    public record DocumentResponse(UUID id, UUID elementId, UUID programId, UUID periodId,
                                   String category, String title, String description,
                                   DocumentStatus status, UUID ownerUserId,
                                   Instant createdAt, Instant updatedAt, Instant publishedAt,
                                   VersionResponse currentVersion) {
        public static DocumentResponse from(Document d, DocumentVersion current) {
            return new DocumentResponse(d.getId(), d.getElementId(), d.getProgramId(), d.getPeriodId(),
                    d.getCategory().getCode(), d.getTitle(), d.getDescription(), d.getStatus(),
                    d.getOwnerUserId(), d.getCreatedAt(), d.getUpdatedAt(), d.getPublishedAt(),
                    current == null ? null : VersionResponse.from(current));
        }
    }
}
