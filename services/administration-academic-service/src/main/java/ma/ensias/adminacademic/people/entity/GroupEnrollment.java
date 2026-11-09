package ma.ensias.adminacademic.people.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.ensias.adminacademic.academic.entity.AcademicYear;
import ma.ensias.adminacademic.academic.entity.StudentGroup;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_enrollment")
@Getter
@Setter
@NoArgsConstructor
public class GroupEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private StudentGroup studentGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @PrePersist
    void onCreate() {
        enrolledAt = Instant.now();
    }
}