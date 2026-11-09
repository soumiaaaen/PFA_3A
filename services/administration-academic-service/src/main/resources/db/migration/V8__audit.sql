CREATE TABLE audit_event (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_user_id UUID,
    action        VARCHAR(50) NOT NULL,
    entity_type   VARCHAR(50) NOT NULL,
    entity_id     UUID,
    details       TEXT
);
CREATE INDEX idx_audit_entity   ON audit_event(entity_type, entity_id);
CREATE INDEX idx_audit_occurred ON audit_event(occurred_at DESC);