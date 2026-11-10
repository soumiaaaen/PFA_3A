package ma.ensias.content.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_category")
@Getter
@Setter
@NoArgsConstructor
public class DocumentCategory {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
