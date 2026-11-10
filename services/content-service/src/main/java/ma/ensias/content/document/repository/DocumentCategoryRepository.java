package ma.ensias.content.document.repository;

import ma.ensias.content.document.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, String> {

    List<DocumentCategory> findAllByOrderBySortOrderAsc();
}
