package ma.ensias.content.document.web;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import ma.ensias.content.document.service.DocumentService;
import ma.ensias.content.document.service.DocumentService.Download;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import ma.ensias.content.document.web.dto.DocumentDtos.VersionResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @Operation(summary = "Déposer un document (enseignant affecté à l'élément) — il est créé en BROUILLON, version 1")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@RequestParam UUID elementId,
                                   @RequestParam String category,
                                   @RequestParam @NotBlank @Size(max = 200) String title,
                                   @RequestParam(required = false) @Size(max = 5000) String description,
                                   @RequestPart("file") MultipartFile file) {
        return service.create(elementId, category, title, description, file);
    }

    @Operation(summary = "Détail d'un document et de sa version courante")
    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Ajouter une nouvelle version (fichier corrigé ou mis à jour)")
    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse addVersion(@PathVariable UUID id,
                                      @RequestPart("file") MultipartFile file,
                                      @RequestParam(required = false) @Size(max = 500) String comment) {
        return service.addVersion(id, file, comment);
    }

    @Operation(summary = "Historique des versions (enseignants et responsables)")
    @GetMapping("/{id}/versions")
    public List<VersionResponse> versions(@PathVariable UUID id) {
        return service.versions(id);
    }

    @Operation(summary = "Télécharger la version courante")
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        return toResponse(service.downloadCurrent(id));
    }

    @Operation(summary = "Télécharger une ancienne version (enseignants et responsables)")
    @GetMapping("/{id}/versions/{number}/download")
    public ResponseEntity<InputStreamResource> downloadVersion(@PathVariable UUID id, @PathVariable int number) {
        return toResponse(service.downloadVersion(id, number));
    }

    private ResponseEntity<InputStreamResource> toResponse(Download f) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(f.filename(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(f.mimeType()))
                .contentLength(f.size())
                .body(new InputStreamResource(f.content()));
    }
}
