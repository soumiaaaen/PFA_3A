package ma.ensias.content.document.web;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import ma.ensias.content.document.entity.DocumentStatus;
import ma.ensias.content.document.service.DocumentQueryService;
import ma.ensias.content.document.service.DocumentQueryService.Filters;
import ma.ensias.content.document.service.DocumentService;
import ma.ensias.content.document.service.DocumentService.Download;
import ma.ensias.content.document.service.PublicationService;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import ma.ensias.content.document.web.dto.DocumentDtos.GroupsRequest;
import ma.ensias.content.document.web.dto.DocumentDtos.UpdateDocumentRequest;
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
    private final PublicationService publication;
    private final DocumentQueryService query;

    @Operation(summary = "Liste des documents que je peux voir (filtres facultatifs ; archives seulement avec status=ARCHIVE)")
    @GetMapping
    public List<DocumentResponse> list(@RequestParam(required = false) UUID elementId,
                                       @RequestParam(required = false) UUID programId,
                                       @RequestParam(required = false) UUID periodId,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) DocumentStatus status,
                                       @RequestParam(required = false) String q) {
        return query.list(new Filters(elementId, programId, periodId, category, status, q));
    }

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

    @Operation(summary = "Modifier le titre, la description ou la catégorie (champs facultatifs)")
    @PatchMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDocumentRequest body) {
        return publication.update(id, body);
    }

    @Operation(summary = "Choisir les groupes d'étudiants autorisés (remplace la liste)")
    @PutMapping("/{id}/groups")
    public DocumentResponse setGroups(@PathVariable UUID id, @Valid @RequestBody GroupsRequest body) {
        return publication.setGroups(id, body.groupIds());
    }

    @Operation(summary = "Publier (BROUILLON → PUBLIE). Sans groupes choisis : vos groupes pour cet élément")
    @PostMapping("/{id}/publish")
    public DocumentResponse publish(@PathVariable UUID id) {
        return publication.publish(id);
    }

    @Operation(summary = "Dépublier (PUBLIE → BROUILLON) : les étudiants ne le voient plus")
    @PostMapping("/{id}/unpublish")
    public DocumentResponse unpublish(@PathVariable UUID id) {
        return publication.unpublish(id);
    }

    @Operation(summary = "Archiver (définitif) : auteur, chef de filière ou admin")
    @PostMapping("/{id}/archive")
    public DocumentResponse archive(@PathVariable UUID id) {
        return publication.archive(id);
    }

    @Operation(summary = "Supprimer un brouillon jamais publié (auteur uniquement)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@PathVariable UUID id) {
        publication.deleteDraft(id);
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
