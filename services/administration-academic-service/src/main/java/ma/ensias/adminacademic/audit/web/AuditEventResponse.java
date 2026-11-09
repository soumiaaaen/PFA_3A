package ma.ensias.adminacademic.audit.web;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(UUID id, Instant occurredAt,
                                 UUID actorUserId, String actorEmail,
                                 String action, String entityType, UUID entityId,
                                 String details) {
}