CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID         NOT NULL,
    actor_role  VARCHAR(20)  NOT NULL,
    target_id   UUID,
    event       VARCHAR(50)  NOT NULL,
    payload     JSONB,
    ip          VARCHAR(45)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor_date ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_event_date ON audit_logs (event,    created_at DESC);
