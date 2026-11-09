# administration-academic-service — Guide d'intégration pour l'équipe

Service n°1 d'ENSiaS : identité (comptes, rôles, JWT) et référentiel académique
(calendrier, départements, filières, groupes, modules, éléments, affectations, étudiants).

- Port : **8081**
- Swagger : http://localhost:8081/swagger-ui.html
- Contrat OpenAPI : `contracts/administration-academic/openapi.json`
- Schéma PostgreSQL : `administration_academic` (aucun autre service ne doit le lire directement)

---

## 1. Lancer le service en local

Prérequis : Docker Desktop.

```powershell
cd infra
copy .env.example .env      # puis remplir avec les valeurs partagées en privé
docker compose up -d --build
```

Vérification :

```powershell
curl.exe http://localhost:8081/actuator/health
# {"status":"UP", ...}
```

Le profil `demo` est actif dans le docker-compose : les données du scénario de démonstration
sont créées automatiquement au démarrage (sans doublons).

### Variables d'environnement (fichier `infra/.env`, jamais dans Git)

| Variable | Rôle |
|---|---|
| `JWT_SECRET` | Clé HS256 partagée par **tous** les services pour valider les tokens (32 caractères min.) |
| `INTERNAL_API_KEY` | Clé partagée pour les appels entre services (`/internal/**`) |
| `ADMIN_PASSWORD` | Mot de passe du compte administrateur initial |

Les valeurs réelles sont communiquées en privé, pas sur GitHub.

---

## 2. Comptes de démonstration

| Rôle | Email | Mot de passe |
|---|---|---|
| ADMIN | admin@ensias.ma | valeur de `ADMIN_PASSWORD` |
| CHEF_DEPARTEMENT (+ ENSEIGNANT) | chef.dept@ensias.ma | Chef@12345 |
| CHEF_FILIERE GL (+ ENSEIGNANT) | chef.gl@ensias.ma | Chef@12345 |
| ENSEIGNANT | prof@ensias.ma | Prof@12345 |
| ETUDIANT (groupe G1, GL 2A) | etudiant@ensias.ma | Etu@12345 |
| ETUDIANT (groupe G1, GL 2A) | etudiant2@ensias.ma | Etu@12345 |

Données de démo : année 2026-2027, S1 et S2 (2 périodes chacun), département INFO,
filières GL (chef : Nadia Tazi) et SSI (sans chef), groupes G1 et G2 (GL 2A),
module GL2-M1 avec les éléments GL2-M1-E1 « Microservices » (S1-P1) et GL2-M1-E2 « Design patterns » (S1-P2),
affectations : Sara Alami → Microservices CM 20 h, Nadia Tazi → Design patterns CM 18 h.

---
## 3. Authentification : le format du JWT

### Obtenir un token

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "prof@ensias.ma", "password": "Prof@12345" }
```

Réponse :

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "x7Kp2mQ9vL...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

Autres endpoints : `POST /api/auth/refresh` (body `{ "refreshToken": "..." }`, rotation à chaque appel),
`POST /api/auth/logout` (même body), `GET /api/auth/me`, `PUT /api/auth/me/password`.

### Contenu de l'access token

| Claim | Exemple | Signification |
|---|---|---|
| `alg` (en-tête) | `HS256` | Signature HMAC avec `JWT_SECRET` |
| `iss` | `ensias-admin-academic` | Émetteur |
| `sub` | `857ffd73-996b-4bed-bf10-781446ca88db` | **id de l'utilisateur** (UUID) |
| `email` | `prof@ensias.ma` | |
| `roles` | `["ENSEIGNANT"]` | `ADMIN`, `CHEF_DEPARTEMENT`, `CHEF_FILIERE`, `ENSEIGNANT`, `ETUDIANT` (plusieurs possibles) |
| `exp` | | Expiration (15 min) |

Chaque requête vers un service porte l'en-tête : `Authorization: Bearer <accessToken>`.

### Valider le token dans un service Spring Boot

Dépendance : `spring-boot-starter-oauth2-resource-server`. Puis, dans la configuration de sécurité :

```java
@Bean
JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
}

// Transforme le claim "roles": ["ENSEIGNANT"] en autorité ROLE_ENSEIGNANT
private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return converter;
}

// Dans le SecurityFilterChain :
// .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())))
```

Ensuite `@PreAuthorize("hasRole('ENSEIGNANT')")` fonctionne, et l'id de l'utilisateur connecté
s'obtient avec `@AuthenticationPrincipal Jwt jwt` puis `UUID.fromString(jwt.getSubject())`.

### Valider le token dans le service IA (Python / FastAPI)

```python
import jwt  # pip install PyJWT

payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"], issuer="ensias-admin-academic")
user_id = payload["sub"]
roles = payload["roles"]
```

---
## 4. API internes (entre services)

Réservées aux autres microservices. **Pas de JWT** : on envoie la clé partagée dans un en-tête.

```text
X-Internal-Key: <valeur de INTERNAL_API_KEY>
```

Sans clé valide : `401`. La Gateway ne doit **jamais** router `/internal/**` vers l'extérieur.

| Endpoint | Réponse | Utilisé par |
|---|---|---|
| `GET /internal/users/{id}` | nom, email, rôles, compte actif | tous |
| `GET /internal/periods/{id}` | numéro, semestre, année, dates de début et de fin | planning |
| `GET /internal/elements/{id}` | élément + module + filière + période complète | planning, content, monitoring |
| `GET /internal/groups/{id}` | nom, niveau, filière, département, année | planning, monitoring |
| `GET /internal/groups/{id}/students` | liste des ids (UUID) des étudiants du groupe | monitoring (absences, notifications) |
| `GET /internal/students/{userId}/groups` | groupes de l'étudiant | planning (EDT), content (ACL) |
| `GET /internal/assignments?periodId=...` (+ `teacherUserId`, `groupId`, `elementId` optionnels) | affectations actives avec `targetHours` | monitoring (volumes horaires) |
| `GET /internal/assignments/check?elementId=&teacherUserId=&groupId=` | `{ "assigned": true/false, "assignments": [...] }` | planning, content |

Depuis un autre conteneur du docker-compose, l'URL de base est `http://administration-academic-service:8081`
(et non `localhost`).

---

## 5. API publiques principales (via la Gateway, avec JWT)

| Domaine | Endpoints | Écriture réservée à |
|---|---|---|
| Comptes | `/api/users`, `/api/users/{id}/roles`, `/status`, `/reset-password` | ADMIN |
| Calendrier | `/api/academic-years`, `/api/academic-years/{id}/semesters`, `/api/periods/{id}` | ADMIN |
| Organisation | `/api/departments`, `/api/programs`, `/api/groups`, `.../head` | ADMIN |
| Catalogue | `/api/modules`, `/api/modules/{id}/elements`, `/api/elements/{id}` | ADMIN, CHEF_FILIERE (sa filière) |
| Affectations | `/api/assignments`, `/api/assignments/{id}/history` | ADMIN, CHEF_FILIERE (sa filière) |
| Étudiants | `/api/students`, `/api/groups/{id}/students`, `/api/students/me/groups` | ADMIN, CHEF_FILIERE |
| Audit | `/api/audit` | lecture ADMIN |

Détail complet des champs : Swagger ou `openapi.json`.
Erreurs : format JSON standard (RFC 9457).

---

## 6. Règles communes de l'architecture

- Chaque service a **son schéma** et **son compte SQL** ; aucune jointure ni lecture entre schémas.
- Les références vers les données de ce service (utilisateur, élément, groupe, période...) sont des **UUID**,
  vérifiés par les endpoints `/internal/**`.
- Chaque service **valide lui-même le JWT**, en plus de la Gateway.
- Pour ajouter le schéma de votre service : un script dans `infra/postgres/init/`, sur le modèle de
  `01-administration-academic.sql` (créer un utilisateur et un schéma qui lui appartient).