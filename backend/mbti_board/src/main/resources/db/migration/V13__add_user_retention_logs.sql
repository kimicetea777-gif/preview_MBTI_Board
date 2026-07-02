CREATE TABLE user_retention_logs (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        UUID         NOT NULL,
    email_enc      TEXT         NOT NULL,
    phone_hash     VARCHAR(64),
    withdrawal_ip  VARCHAR(45)  NOT NULL,
    withdrawn_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_retention_expires CHECK (expires_at > withdrawn_at)
);

CREATE INDEX idx_user_retention_logs_expires ON user_retention_logs (expires_at);
