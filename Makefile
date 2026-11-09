# ---------------------------------------------------------------
# Commandes communes de l'equipe ENSiaS (memes commandes pour tous)
# Windows : installer make une seule fois avec
#   winget install ezwinports.make
# Les lignes de commande commencent par ">" (au lieu d'une tabulation)
# pour eviter les erreurs de copier-coller.
# ---------------------------------------------------------------
.RECIPEPREFIX := >

ifeq ($(OS),Windows_NT)
  MVNW := mvnw.cmd
else
  MVNW := ./mvnw
endif

SERVICE := services/administration-academic-service
COMPOSE := docker compose -f infra/docker-compose.yml

.PHONY: help check up down logs db back test install front

help:
> @echo make check    - affiche les versions installees, a comparer avec VERSIONS.md
> @echo make up       - lance PostgreSQL + le service administration-academic dans Docker
> @echo make down     - arrete les conteneurs
> @echo make logs     - affiche les logs du service
> @echo make db       - lance seulement PostgreSQL
> @echo make back     - lance le backend en local sans Docker
> @echo make test     - lance les tests du backend
> @echo make install  - installe les dependances du frontend
> @echo make front    - lance le frontend sur http://localhost:4200

check:
> java -version
> node -v
> npm -v
> docker --version
> docker compose version
> cd frontend && npx ng version

up:
> $(COMPOSE) up -d --build

down:
> $(COMPOSE) down

logs:
> $(COMPOSE) logs -f administration-academic-service

db:
> $(COMPOSE) up -d postgres

back:
> cd $(SERVICE) && $(MVNW) spring-boot:run

test:
> cd $(SERVICE) && $(MVNW) test

install:
> cd frontend && npm ci

front:
> cd frontend && npm start