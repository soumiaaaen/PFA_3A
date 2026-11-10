-- ============================================================
-- content-service : schéma "content" (conception v0.2, section 3)
-- Les identifiants venant d'autres services (élément, filière, période,
-- utilisateur, groupe) sont des UUID SANS clé étrangère : chaque service
-- possède ses données, on les vérifie par API.
-- ============================================================

-- Catégories de documents (table de référence, extensible)
CREATE TABLE document_category (
    code       VARCHAR(20)  PRIMARY KEY,
    label      VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0
);

INSERT INTO document_category (code, label, sort_order) VALUES
    ('COURS',  'Cours',                1),
    ('TD',     'Travaux dirigés',      2),
    ('TP',     'Travaux pratiques',    3),
    ('ANNALE', 'Annales d''examen',    4);

-- Fichier stocké dans MinIO (séparé de la version : permet de retrouver
-- et nettoyer un fichier envoyé dont la transaction aurait échoué)
CREATE TABLE storage_object (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bucket            VARCHAR(63)  NOT NULL,
    object_key        VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type         VARCHAR(150) NOT NULL,
    size_bytes        BIGINT       NOT NULL CHECK (size_bytes > 0),
    sha256            VARCHAR(64)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_storage_object_key UNIQUE (bucket, object_key)
);

-- Document pédagogique
CREATE TABLE document (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    element_id         UUID         NOT NULL,   -- administration-academic : élément
    program_id         UUID         NOT NULL,   -- copié depuis l'élément (filtrage par filière)
    period_id          UUID         NOT NULL,   -- copié depuis l'élément (filtrage par période)
    category_code      VARCHAR(20)  NOT NULL REFERENCES document_category(code),
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'BROUILLON'
                       CHECK (status IN ('BROUILLON', 'PUBLIE', 'ARCHIVE')),
    current_version_id UUID,
    acl_version        INTEGER      NOT NULL DEFAULT 0,
    owner_user_id      UUID         NOT NULL,   -- enseignant qui a déposé le document
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at       TIMESTAMPTZ,
    archived_at        TIMESTAMPTZ
);
CREATE INDEX idx_document_element ON document(element_id);
CREATE INDEX idx_document_program ON document(program_id);
CREATE INDEX idx_document_period  ON document(period_id);
CREATE INDEX idx_document_status  ON document(status);
CREATE INDEX idx_document_owner   ON document(owner_user_id);

-- Versions successives d'un document
CREATE TABLE document_version (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id       UUID         NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    version_number    INTEGER      NOT NULL CHECK (version_number > 0),
    storage_object_id UUID         NOT NULL UNIQUE REFERENCES storage_object(id),
    comment           VARCHAR(500),
    uploaded_by       UUID         NOT NULL,
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_version UNIQUE (document_id, version_number)
);

-- La version courante doit appartenir au document (vérifiée en fin de transaction)
ALTER TABLE document
    ADD CONSTRAINT fk_document_current_version
    FOREIGN KEY (current_version_id) REFERENCES document_version(id)
    DEFERRABLE INITIALLY DEFERRED;

-- Groupes d'étudiants autorisés à voir le document publié
CREATE TABLE document_acl (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID        NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    group_id    UUID        NOT NULL,   -- administration-academic : groupe
    granted_by  UUID        NOT NULL,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_acl UNIQUE (document_id, group_id)
);
CREATE INDEX idx_document_acl_group ON document_acl(group_id);

-- Outbox transactionnelle : événements écrits dans la même transaction que
-- le changement, puis envoyés à RabbitMQ (étape 7)
CREATE TABLE outbox_event (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type   VARCHAR(60)  NOT NULL,
    routing_key  VARCHAR(100) NOT NULL,
    aggregate_id UUID         NOT NULL,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    last_error   TEXT
);
CREATE INDEX idx_outbox_pending ON outbox_event(created_at) WHERE published_at IS NULL;

-- Journal d'audit des documents (EF-15 : qui / quand / quoi)
CREATE TABLE audit_event (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_user_id UUID,
    action        VARCHAR(50) NOT NULL,
    document_id   UUID,
    details       TEXT
);
CREATE INDEX idx_audit_document ON audit_event(document_id);
CREATE INDEX idx_audit_occurred ON audit_event(occurred_at DESC);
