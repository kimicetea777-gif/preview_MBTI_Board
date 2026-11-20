-- ===================================================
-- users.state 길이 20 → 30
--
-- V1에서 state를 VARCHAR(20)으로 만들었지만 chk_user_state가 허용하는 'MEMBERSHIP_WITHDRAWAL_PENDING'은
-- 29자라 들어가지 않는다. 그래서 AS CLIENT_WITHDRAWN 이벤트 반영(UserService.handleMembershipWithdrawn)이
-- 항상 "value too long for type character varying(20)"으로 실패했다 — 옛 폴러는 첫 탈퇴 이벤트에서 영구히
-- 멈췄고, 재시도 큐(V15)가 생긴 뒤에는 그 이벤트가 끝없이 재시도된다.
-- varchar 길이를 늘리는 것은 테이블을 다시 쓰지 않는다(카탈로그만 바뀜). CHECK 제약은 그대로 유효하다.
-- 설계: Document/membership_event_retry_plan.md "구현 기록"
-- ===================================================

ALTER TABLE users ALTER COLUMN state TYPE VARCHAR(30);
