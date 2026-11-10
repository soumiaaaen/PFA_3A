-- content-service (Ihssan) : utilisateur et schéma propres au service
CREATE USER content_user WITH PASSWORD 'content_pwd';
CREATE SCHEMA content AUTHORIZATION content_user;
