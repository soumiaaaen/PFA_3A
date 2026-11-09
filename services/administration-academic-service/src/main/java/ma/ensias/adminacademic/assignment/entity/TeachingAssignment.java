package ma.ensias.adminacademic.assignment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.ensias.adminacademic.academic.entity.Period;
import ma.ensias.adminacademic.academic.entity.StudentGroup;
import ma.ensias.adminacademic.catalog.entity.Element;
import ma.ensias.adminacademic.identity.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teaching_assignment")
@Getter
@Setter
@NoArgsConstructor
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_id")
    private Element element;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_user_id")
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private StudentGroup studentGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id")
    private Period period;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 2)
    private ActivityType activityType;

    @Column(name = "target_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal targetHours;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}