package ma.ensias.adminacademic.assignment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_history")
@Getter
@Setter
@NoArgsConstructor
public class AssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private TeachingAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HistoryAction action;

    @Column(name = "old_teacher_user_id")
    private UUID oldTeacherUserId;

    @Column(name = "new_teacher_user_id")
    private UUID newTeacherUserId;

    @Column(name = "old_target_hours", precision = 6, scale = 2)
    private BigDecimal oldTargetHours;

    @Column(name = "new_target_hours", precision = 6, scale = 2)
    private BigDecimal newTargetHours;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        changedAt = Instant.now();
    }
}