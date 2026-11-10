# content-service — Contrat des événements RabbitMQ

À l'attention de **Fatima (ai-service)** et **Soumia (pedagogical-monitoring-service)**.
Statut : **brouillon v0.1** — vous pouvez déjà simuler ces messages dans vos tests.

## Transport

| | |
|---|---|
| Exchange | `ensias.content` (type **topic**, durable) ❓ à aligner avec la convention commune |
| Routing keys | `document.published`, `document.access-revoked` |
| Format | JSON UTF-8, `content_type=application/json` |
| En-têtes AMQP | `message_id` = `eventId`, `correlation_id` = Correlation-ID de la requête d'origine |
| Garantie | **au moins une fois** (outbox transactionnelle) : un même événement peut arriver deux fois → le consommateur doit être idempotent (`eventId` déjà traité → ignorer) |
| Ordre | non garanti : comparer `aclVersion` (ignorer un événement dont `aclVersion` ≤ celle déjà connue pour ce document) |

Chaque consommateur crée **sa propre queue** liée à l'exchange (ex. `ai.content-events`, `monitoring.content-events`) avec sa DLQ.

## Enveloppe commune

```json
{
  "eventId": "6f1c2b9e-...",
  "eventType": "DocumentPublished",
  "eventVersion": 1,
  "occurredAt": "2026-11-20T09:15:02Z",
  "producer": "content-service",
  "data": { }
}
```

## `DocumentPublished` — routing key `document.published`

Émis quand un document passe à PUBLIE, quand une **nouvelle version** d'un document publié est déposée, ou quand des **groupes sont ajoutés** à un document publié.

```json
{
  "eventId": "6f1c2b9e-0b7a-4f7e-9d0c-1a2b3c4d5e6f",
  "eventType": "DocumentPublished",
  "eventVersion": 1,
  "occurredAt": "2026-11-20T09:15:02Z",
  "producer": "content-service",
  "data": {
    "documentId": "b3a1...",
    "versionId": "c9d2...",
    "versionNumber": 2,
    "title": "Chapitre 3 — Architecture microservices",
    "category": "COURS",
    "elementId": "e1f0...",
    "programId": "p7a3...",
    "periodId": "d4c8...",
    "groupIds": ["g1...", "g2..."],
    "mimeType": "application/pdf",
    "sizeBytes": 1834221,
    "sha256": "9f86d081884c7d65...",
    "publishedBy": "857ffd73-996b-4bed-bf10-781446ca88db",
    "aclVersion": 3
  }
}
```

Utilisation attendue :
- **ai-service** : télécharger le fichier via `GET /internal/documents/{documentId}/versions/{versionId}/content`, (ré)indexer, supprimer les passages des versions précédentes du même document.
- **monitoring** : notification « nouveau document » aux étudiants de `groupIds` (EF-12).

## `DocumentAccessRevoked` — routing key `document.access-revoked`

Émis quand un document est **dépublié**, **archivé**, ou quand des **groupes sont retirés** de l'ACL.

```json
{
  "eventId": "0a9b...",
  "eventType": "DocumentAccessRevoked",
  "eventVersion": 1,
  "occurredAt": "2026-11-21T14:02:40Z",
  "producer": "content-service",
  "data": {
    "documentId": "b3a1...",
    "reason": "GROUPS_REMOVED",
    "revokedGroupIds": ["g2..."],
    "remainingGroupIds": ["g1..."],
    "aclVersion": 4
  }
}
```

`reason` ∈ `UNPUBLISHED`, `ARCHIVED`, `GROUPS_REMOVED`.
Pour `UNPUBLISHED` et `ARCHIVED`, `remainingGroupIds` est vide : l'ai-service doit **désindexer** tout le document.

> Même si un événement se perd ou arrive en retard, l'ai-service reste sûr : il vérifie les droits **actuels** via `/internal/users/{userId}/accessible-documents` avant chaque recherche.
