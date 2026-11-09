# ENSiaS — Plateforme de gestion pédagogique (PFA 3A)

Plateforme en microservices pour l'ENSIAS (Université Mohammed V de Rabat).

## Démarrage rapide

1. Installer les outils listés dans [VERSIONS.md](VERSIONS.md) puis vérifier : `make check`
2. Copier `infra/.env.example` en `infra/.env` et demander les valeurs en privé
3. `make up` — PostgreSQL + administration-academic-service (http://localhost:8081/swagger-ui.html)
4. `make install` puis `make front` — frontend sur http://localhost:4200

`make help` affiche toutes les commandes.

## Structure

| Dossier | Contenu |
|---|---|
| `infra/` | docker-compose, scripts PostgreSQL |
| `services/administration-academic-service/` | Comptes, rôles, JWT, calendrier, organisation, modules, affectations, audit |
| `frontend/` | Application Angular |
| `contracts/` | Contrats d'API entre services |