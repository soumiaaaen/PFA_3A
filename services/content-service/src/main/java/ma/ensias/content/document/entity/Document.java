package ma.ensias.content.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Document pédagogique (cours, TD, TP, annale) rattaché à un élément. */
@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Références vers l'administration-academic-service (UUID, sans clé étrangère)
    @Column(name = "element_id", nullable = false)
    private UUID elementId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_code")
    private DocumentCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.BROUILLON;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    /** Augmente à chaque changement de droits ; envoyé dans les événements (idempotence côté IA). */
    @Column(name = "acl_version", nullable = false)
    private int aclVersion;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
