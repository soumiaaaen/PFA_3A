package ma.ensias.content.internal;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import ma.ensias.content.document.service.DocumentService.Download;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import ma.ensias.content.internal.InternalDocumentService.AccessResponse;
import ma.ensias.content.internal.InternalDocumentService.AccessibleDocumentsResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** API entre microservices (en-tête X-Internal-Key). Jamais exposée par la Gateway. */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalDocumentController {

    private final InternalDocumentService service;

    @Operation(summary = "Métadonnées d'un document, version courante et groupes autorisés")
    @GetMapping("/documents/{id}")
    public DocumentResponse describe(@PathVariable UUID id) {
        return service.describe(id);
    }

    @Operation(summary = "Cet utilisateur peut-il voir ce document MAINTENANT ? (document publié + droits actuels)")
    @GetMapping("/documents/{id}/access")
    public AccessResponse access(@PathVariable UUID id, @RequestParam UUID userId) {
        return service.access(id, userId);
    }

    @Operation(summary = "Documents publiés visibles par cet utilisateur (filtre de la recherche IA)")
    @GetMapping("/users/{userId}/accessible-documents")
    public AccessibleDocumentsResponse accessibleDocuments(@PathVariable UUID userId) {
        return service.accessibleDocuments(userId);
    }

    @Operation(summary = "Fichier d'une version publiée (indexation par l'IA)")
    @GetMapping("/documents/{id}/versions/{versionId}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable UUID id, @PathVariable UUID versionId) {
        Download f = service.content(id, versionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(f.filename(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(f.mimeType()))
                .contentLength(f.size())
                .body(new InputStreamResource(f.content()));
    }
}
