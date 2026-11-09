package ma.ensias.adminacademic.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.ensias.adminacademic.academic.entity.Program;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "module")
@Getter
@Setter
@NoArgsConstructor
public class TeachingModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "study_level", nullable = false)
    private Integer studyLevel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Column(precision = 4, scale = 2)
    private BigDecimal coefficient;

    @OneToMany(mappedBy = "module")
    @OrderBy("code")
    private List<Element> elements = new ArrayList<>();
}