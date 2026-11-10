package ma.ensias.content.web;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.common.security.CurrentUser;
import ma.ensias.content.storage.StorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

/** Endpoint de vérification de l'étape 3 : le token est-il accepté, MinIO est-il joignable ? */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class SystemController {

    private final CurrentUser currentUser;
    private final StorageService storage;

    public record MeResponse(UUID userId, String email, Set<String> roles, boolean storageAvailable) {
    }

    @GetMapping("/me")
    public MeResponse me() {
        return new MeResponse(currentUser.id(), currentUser.email(), currentUser.roles(), storage.isAvailable());
    }
}
