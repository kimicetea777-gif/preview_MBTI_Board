-- ===================================================
-- 멤버십 이벤트 폴링 실패 재시도 큐
--
-- AS 멤버십 이벤트(CLIENT_WITHDRAWN / CLIENT_BANNED / CLIENT_ACTIVATED)를 로컬에 반영하다 실패했거나,
-- 같은 유저의 앞선 이벤트가 실패해 순서를 지키려고 대기 중인 이벤트를 담는다.
-- 행이 있다는 것 자체가 "아직 반영되지 않음"이다 — 성공하면 행을 삭제한다(상태 컬럼 없음).
-- 순서: 같은 auth_server_id 안에서는 seq(줄 선 순서 = 폴링 순서) 오름차순으로만 반영한다.
--       event_id는 중복 방지용이며 순서 판단에 쓰지 않는다.
-- users로의 FK는 없다: 로컬 계정이 아직 없는(첫 로그인 전) 유저의 이벤트도 담아야 하고,
--       유저 행이 지워져도 이벤트가 함께 사라지면 안 된다.
-- 설계: Document/membership_event_retry_plan.md
-- ===================================================

CREATE TABLE membership_event_failures (
    -- AS user_client_registration_logs.id — 같은 이벤트를 다시 받아도 ON CONFLICT DO NOTHING으로 무시
    event_id BIGINT PRIMARY KEY,
    seq BIGSERIAL,
    auth_server_id UUID NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    -- AS 쪽 이벤트 발생 시각 — CLIENT_WITHDRAWN의 유예기간 계산 기준이라 로컬 처리 시각으로 바꾸지 않는다
    changed_at TIMESTAMPTZ NOT NULL,
    fail_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_membership_event_failures_fail_count CHECK (fail_count >= 0)
);

-- 유저별 맨 앞 행 조회, "이 유저에게 미해결 실패가 있는가" 확인
CREATE INDEX idx_membership_event_failures_user ON membership_event_failures (auth_server_id, seq);

-- 재시도 작업의 due 조회
CREATE INDEX idx_membership_event_failures_due ON membership_event_failures (next_attempt_at);
