# content-service — Suivi d'avancement (Ihssan, M4)

Légende : ✅ fait · 🔄 en cours · ⏳ à faire · 🔒 bloqué / en attente d'un autre membre

Dernière mise à jour : 10/11/2026

| # | Étape | Contenu | Semaine cible | Statut |
|---|---|---|---|---|
| 0 | Environnement | VM VirtualBox Ubuntu 26.04 Server (10 Go, 2 CPU, 80 Go) + VS Code Remote-SSH ; Java 21.0.13, Node 24.22, Docker 29.9, Compose v5.7 ; clé SSH GitHub (port 443) ; dépôt cloné ; service d'Aya UP, login Swagger OK | S2 | ✅ 10/11 |
| 1 | Conception | Cas d'utilisation, règles d'accès, diagramme d'états, classes, séquences, contrat API, contrat événements (`CONCEPTION.md`, `EVENTS.md`) | S1 | 🔄 brouillon v0.1 sur la branche `feature/content-service`, décisions ❓ à valider |
| 2 | Infra | `infra/postgres/init/04-content.sql`, MinIO dans `docker-compose.yml`, variables dans `.env.example`, port 8084 | S2 | ⏳ |
| 3 | Squelette | Projet Spring Boot (`ma.ensias.content`), sécurité JWT + clé interne, gestion d'erreurs, `/actuator/health`, Swagger, Dockerfile multi-stage | S2–S3 | ⏳ |
| 4 | Base de données | Migrations Flyway V1…, entités JPA, repositories, catégories initiales | S3 | ⏳ |
| 5 | Dépôt et versions | Upload multipart → MinIO, vérification type/taille, SHA-256, versions, téléchargement | S3–S5 | ⏳ |
| 6 | Droits et publication | Client admin-academic (`/internal`), ACL par groupes, cycle BROUILLON/PUBLIE/ARCHIVE, liste filtrée selon les droits | S4–S5 | ⏳ (🔒 endpoint responsable filière à demander à Aya) |
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

- [ ] Aya : ajouter `GET /internal/programs/{id}` (headUserId, departmentId) et `GET /internal/departments/{id}` (headUserId)
- [ ] Équipe : numérotation des ports (8082 planning, 8083 monitoring, 8084 content, 8085 ai ?)
- [ ] Soumia / équipe : qui ajoute RabbitMQ au compose, convention des exchanges
- [ ] Équipe : qui pilote la Gateway
- [ ] Fatima et Soumia : relire `EVENTS.md`
- [ ] Encadrant : droits officiels sur les documents (cahier des charges §12)
