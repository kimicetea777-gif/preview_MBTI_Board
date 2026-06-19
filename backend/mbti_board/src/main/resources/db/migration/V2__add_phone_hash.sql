ALTER TABLE users ADD COLUMN phone_hash VARCHAR(64);

CREATE UNIQUE INDEX idx_users_phone_hash ON users (phone_hash) WHERE phone_hash IS NOT NULL;
