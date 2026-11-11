# content-service — Suivi d'avancement (Ihssan, M4)

Légende : ✅ fait · 🔄 en cours · ⏳ à faire · 🔒 bloqué / en attente d'un autre membre

Dernière mise à jour : 10/11/2026 (soir)

| # | Étape | Contenu | Semaine cible | Statut |
|---|---|---|---|---|
| 0 | Environnement | VM VirtualBox Ubuntu 26.04 Server (10 Go, 2 CPU, 80 Go) + VS Code Remote-SSH ; Java 21.0.13, Node 24.22, Docker 29.9, Compose v5.7 ; clé SSH GitHub (port 443) ; dépôt cloné ; service d'Aya UP, login Swagger OK | S2 | ✅ 10/11 |
| 1 | Conception | Cas d'utilisation, règles d'accès, diagramme d'états, classes, séquences, contrat API, contrat événements (`CONCEPTION.md`, `EVENTS.md`) | S1 | ✅ v0.2 (décisions prises le 10/11), à présenter à l'encadrant |
| 2 | Infra | `infra/postgres/init/04-content.sql` (schéma `content`, user `content_user`), MinIO (image `cgr.dev/chainguard/minio`, ports 9000/9001) dans `docker-compose.yml`, variables `MINIO_ROOT_*` dans `.env.example` | S2 | ✅ 10/11 |
| 3 | Squelette | Projet Spring Boot 4.1.2 (`ma.ensias.content`, port 8084), sécurité JWT (signature + émetteur) + clé interne, erreurs RFC 9457, `/actuator/health`, Swagger, Dockerfile multi-stage, client S3 (AWS SDK v2) + création auto du bucket, service dans docker-compose, cibles `make content-*` ; test `GET /api/content/me` OK avec le token de prof@ | S2–S3 | ✅ 10/11 |
| 4 | Base de données | Migration `V1__documents.sql` (7 tables : document_category, storage_object, document, document_version, document_acl, outbox_event, audit_event), entités JPA, repositories, endpoint `GET /api/document-categories` | S3 | ✅ 10/11 (migration appliquée, validation Hibernate OK, catégories lues via l'API) |
| 5 | Dépôt et versions | `POST /api/documents`, `POST /{id}/versions`, `GET /{id}`, `/{id}/versions`, `/{id}/download`, `/{id}/versions/{n}/download` ; type réel vérifié (PDF/DOCX/PPTX), SHA-256, nettoyage MinIO si échec ; client admin-academic ; règles d'accès (`AccessPolicy`) ; audit | S3–S5 | ✅ 11/11 (dépôt, v2, téléchargement, MinIO OK ; refus testés : 403 non affecté, 400 faux PDF, 404 + 403 étudiant) |
| 6 | Droits et publication | `GET /api/documents` (liste filtrée selon les droits), `PATCH /{id}`, `PUT /{id}/groups`, `POST /{id}/publish`, `/unpublish`, `/archive`, `DELETE /{id}` (brouillon) ; événements écrits dans l'outbox (format EVENTS.md) | S4–S5 | ✅ 11/11 (étudiant : 0 doc avant, voit + télécharge après publication, 403 sur l'historique, 404 après dépublication ; événements outbox OK) |
| 7 | Événements | Table outbox, publication RabbitMQ `DocumentPublished` / `DocumentAccessRevoked`, retries | S5 | ⏳ (🔒 RabbitMQ dans compose) |
| 8 | API interne IA | `/internal/documents/...` : vérification d'accès, documents accessibles, contenu du fichier | S5 | ⏳ |
| 9 | Audit et démo | Journal des actions, données de démonstration (profil `demo`) | S5 | ⏳ |
| 10 | Tests | Unitaires (règles d'accès, transitions), intégration Testcontainers (PostgreSQL + MinIO + RabbitMQ), export `openapi.json` | S6 | ⏳ |
| 11 | Angular | Page « Ressources » : liste, filtres, dépôt, versions, publication, téléchargement | S5–S6 | ⏳ |
| 12 | DevOps | GitHub Actions (build, tests, scan), image GHCR, Helm chart du service, logs structurés + Correlation-ID | S7 | ⏳ |

## Conventions Git

- Dépôt : `soumiaaaen/PFA_3A`, branche principale `main` (pas encore de `develop`).
- Ma branche : `feature/content-service`, fusion dans `main` par Pull Request relue.
- Messages de commit : `feat(content): …`, `docs(content): …`, `fix(content): …`, `chore(infra): …`.

## Questions ouvertes / à envoyer à l'équipe

- [x] Aya : endpoints `GET /internal/programs?headUserId=&departmentId=` et `GET /internal/departments?headUserId=` — en ligne le 10/11
- [ ] Équipe : numérotation des ports (8082 planning, 8083 monitoring, 8084 content, 8085 ai ?)
- [ ] Soumia / équipe : qui ajoute RabbitMQ au compose, convention des exchanges
- [ ] Équipe : qui pilote la Gateway
- [ ] Équipe : prévenir que MinIO utilise l'image Chainguard (l'image officielle `minio/minio` n'est plus publiée depuis octobre 2025) et ajouter `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` dans chaque `infra/.env`
- [ ] Fatima et Soumia : relire `EVENTS.md`
- [ ] Encadrant : droits officiels sur les documents (cahier des charges §12)
