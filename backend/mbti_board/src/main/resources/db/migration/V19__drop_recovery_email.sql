-- TypeButter 자체 복구 이메일 기능 제거. 이메일 찾기는 AS(Authorization Server) 계정 관리 페이지로
-- 완전히 이관됐고, 이 컬럼은 값을 저장·인증만 할 뿐 아무 조회 기능도 소비하지 않는 죽은 기능이었다.
-- 운영 미배포 상태라 기존 users 데이터는 없음 — 컬럼 삭제 전 백업/이관 불필요.
DROP INDEX IF EXISTS idx_users_recovery_email;

ALTER TABLE users
    DROP COLUMN recovery_email;
