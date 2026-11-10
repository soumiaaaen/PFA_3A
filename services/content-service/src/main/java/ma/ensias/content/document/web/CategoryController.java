package ma.ensias.content.document.web;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.document.repository.DocumentCategoryRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/document-categories")
@RequiredArgsConstructor
public class CategoryController {

    private final DocumentCategoryRepository repository;

    public record CategoryResponse(String code, String label) {
    }

    /** Catégories disponibles (COURS, TD, TP, ANNALE) — visible par tout utilisateur connecté. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return repository.findAllByOrderBySortOrderAsc().stream()
                .map(c -> new CategoryResponse(c.getCode(), c.getLabel()))
                .toList();
    }
}
