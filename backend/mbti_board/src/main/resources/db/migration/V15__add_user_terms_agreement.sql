ALTER TABLE users ADD COLUMN terms_agreed_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN terms_version INT;
ALTER TABLE users ADD COLUMN privacy_agreed_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN privacy_version INT;
