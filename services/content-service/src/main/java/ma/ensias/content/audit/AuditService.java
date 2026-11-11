package ma.ensias.content.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    /** Enregistre une action dans la transaction en cours (qui / quand / quoi). */
    @Transactional
    public void record(String action, UUID documentId, String details) {
        AuditEvent e = new AuditEvent();
        e.setActorUserId(currentUserIdOrNull());
        e.setAction(action);
        e.setDocumentId(documentId);
        e.setDetails(details);
        repository.save(e);
    }

    private UUID currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        return null;
    }
}
