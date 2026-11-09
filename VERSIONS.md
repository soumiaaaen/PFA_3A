# Versions des outils — ENSiaS

Tous les membres de l'équipe utilisent **ces versions** pour éviter les « chez moi ça marche ».
Vérification rapide : `make check`.

| Outil | Version | Remarque |
|---|---|---|
| Java (JDK) | **21** (Temurin 21.0.x LTS) | `java -version` |
| Maven | *rien à installer* | le wrapper `mvnw` télécharge la bonne version |
| Spring Boot | **4.1.2** | fixé dans `pom.xml` |
| Node.js | **24.x** (testé 24.18.0) | fixé dans `frontend/.nvmrc` |
| npm | **11.x** (testé 11.16.0) | fourni avec Node 24 |
| Angular CLI / Angular | **22.2.x** | fixé dans `frontend/package.json` |
| TypeScript | **6.0.x** | installé par `npm ci` |
| Docker | **29.x** ou plus | Docker Desktop |
| Docker Compose | **v5.x** | inclus dans Docker Desktop |
| PostgreSQL | **16** | lancé par Docker, rien à installer |
| make | 4.x | Windows : `winget install ezwinports.make` |

## Règles

- Installer les dépendances du frontend avec **`npm ci`** (pas `npm install`) : il respecte `package-lock.json`.
- Ne pas changer une version sans prévenir l'équipe.
- Le fichier `infra/.env` (secrets) n'est **jamais** sur GitHub : copier `infra/.env.example` en `infra/.env`
  et demander les valeurs en privé.

## Ports utilisés

| Port | Service |
|---|---|
| 5432 | PostgreSQL |
| 8081 | administration-academic-service (Swagger : http://localhost:8081/swagger-ui.html) |
| 4200 | Frontend Angular |