package ma.ensias.adminacademic.catalog.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.catalog.service.CatalogService;
import ma.ensias.adminacademic.catalog.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService service;

    @PostMapping("/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE')")
    public ModuleResponse createModule(@Valid @RequestBody CreateModuleRequest request) {
        return service.createModule(request);
    }

    @GetMapping("/modules")
    public List<ModuleResponse> listModules(@RequestParam UUID programId) {
        return service.listModules(programId);
    }

    @GetMapping("/modules/{id}")
    public ModuleResponse getModule(@PathVariable UUID id) {
        return service.getModule(id);
    }

    @PostMapping("/modules/{moduleId}/elements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE')")
    public ElementResponse createElement(@PathVariable UUID moduleId,
                                         @Valid @RequestBody CreateElementRequest request) {
        return service.createElement(moduleId, request);
    }

    @GetMapping("/elements/{id}")
    public ElementResponse getElement(@PathVariable UUID id) {
        return service.getElement(id);
    }
}