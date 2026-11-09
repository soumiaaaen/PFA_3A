package ma.ensias.adminacademic.audit.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.audit.entity.AuditEvent;
import ma.ensias.adminacademic.audit.repository.AuditEventRepository;
import ma.ensias.adminacademic.audit.web.AuditEventResponse;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;
    private final UserRepository userRepository;

    /** Enregistre une action dans la transaction en cours. */
    @Transactional
    public void record(String action, String entityType, UUID entityId, String details) {
        AuditEvent e = new AuditEvent();
        e.setActorUserId(currentUserIdOrNull());
        e.setAction(action);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setDetails(details);
        repository.save(e);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> list(String entityType, UUID entityId, int limit) {
        List<AuditEvent> events = (entityType != null && entityId != null)
                ? repository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId)
                : repository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limit)).getContent();

        // Une seule requête pour retrouver l'email de tous les auteurs
        Set<UUID> actorIds = events.stream().map(AuditEvent::getActorUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> emails = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return events.stream().map(e -> new AuditEventResponse(
                e.getId(), e.getOccurredAt(), e.getActorUserId(),
                e.getActorUserId() == null ? "SYSTEM" : emails.getOrDefault(e.getActorUserId(), "?"),
                e.getAction(), e.getEntityType(), e.getEntityId(), e.getDetails())).toList();
    }

    private UUID currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        return null;
    }
}