package ma.ensias.content.outbox;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Écrit les événements dans la table outbox, DANS la transaction du changement :
 * si le changement est annulé, l'événement l'est aussi. Format : contracts/content/EVENTS.md.
 * L'envoi vers RabbitMQ est fait à part (étape 7).
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    public static final String DOCUMENT_PUBLISHED = "DocumentPublished";
    public static final String DOCUMENT_ACCESS_REVOKED = "DocumentAccessRevoked";

    private final OutboxEventRepository repository;
    private final JsonMapper json;

    @Transactional(propagation = Propagation.MANDATORY)
    public void documentPublished(Document d, DocumentVersion v, List<UUID> groupIds, UUID publishedBy) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentId", d.getId());
        data.put("versionId", v.getId());
        data.put("versionNumber", v.getVersionNumber());
        data.put("title", d.getTitle());
        data.put("category", d.getCategory().getCode());
        data.put("elementId", d.getElementId());
        data.put("programId", d.getProgramId());
        data.put("periodId", d.getPeriodId());
        data.put("groupIds", groupIds);
        data.put("mimeType", v.getStorageObject().getMimeType());
        data.put("sizeBytes", v.getStorageObject().getSizeBytes());
        data.put("sha256", v.getStorageObject().getSha256());
        data.put("publishedBy", publishedBy);
        data.put("aclVersion", d.getAclVersion());
        save(DOCUMENT_PUBLISHED, "document.published", d.getId(), data);
    }

    /** reason : UNPUBLISHED, ARCHIVED ou GROUPS_REMOVED. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void accessRevoked(Document d, String reason, List<UUID> revokedGroupIds, List<UUID> remainingGroupIds) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentId", d.getId());
        data.put("reason", reason);
        data.put("revokedGroupIds", revokedGroupIds);
        data.put("remainingGroupIds", remainingGroupIds);
        data.put("aclVersion", d.getAclVersion());
        save(DOCUMENT_ACCESS_REVOKED, "document.access-revoked", d.getId(), data);
    }

    private void save(String eventType, String routingKey, UUID aggregateId, Map<String, Object> data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("producer", "content-service");
        envelope.put("data", data);

        OutboxEvent e = new OutboxEvent();
        e.setEventType(eventType);
        e.setRoutingKey(routingKey);
        e.setAggregateId(aggregateId);
        e.setPayload(json.writeValueAsString(envelope));
        repository.save(e);
    }
}
