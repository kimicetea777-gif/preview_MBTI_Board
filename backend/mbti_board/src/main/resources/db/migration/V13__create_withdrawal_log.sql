CREATE TABLE withdrawal_log (
    id           BIGSERIAL    PRIMARY KEY,
    withdrawn_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
