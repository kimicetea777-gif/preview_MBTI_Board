-- 로그인/가입/비밀번호 재설정을 외부 Authorization Server(AS, client_id=TypeButter)에 위임하면서
-- 로컬 비밀번호·소셜 provider 컬럼이 전부 불필요해짐. AS가 발급하는 sub(UUID)만 있으면 충분하다.
-- 약관 동의(terms_*)도 AS가 로그인/가입 시점에 처리하므로 로컬 컬럼을 유지할 이유가 없다.
-- 운영 미배포 상태라 기존 users 데이터는 없음 — 컬럼 삭제 전 백업/이관 불필요.
ALTER TABLE users
    ADD COLUMN auth_server_id UUID,
    DROP COLUMN password,
    DROP COLUMN social_provider,
    DROP COLUMN provider_id,
    DROP COLUMN terms_agreed_at,
    DROP COLUMN terms_version,
    DROP COLUMN privacy_agreed_at,
    DROP COLUMN privacy_version;

ALTER TABLE users
    ALTER COLUMN auth_server_id SET NOT NULL,
    ADD CONSTRAINT uq_users_auth_server_id UNIQUE (auth_server_id);
