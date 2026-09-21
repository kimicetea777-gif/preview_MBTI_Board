-- ===================================================
-- 회원 탈퇴 시 총 투표수(vote_post_options.vote_count)도 감소시키지 않도록 변경
-- MBTI별 카운트(V12, vote_option_mbti_counts)와 동일한 정책 — 탈퇴로
-- vote_posts_history가 cascade 삭제돼도 "몇 표가 던져졌는지"의 누적 기록은 보존한다.
--
-- 참고: vote_posts_history에 대한 DELETE는 실질적으로 두 경로뿐:
--   1. 회원 탈퇴(UserCleanupService.bulkDeleteByState → users ON DELETE CASCADE)
--      — 옵션/게시글은 그대로 남음 → 이 케이스가 이번에 보존하려는 대상
--   2. 투표 게시글/선택지 자체가 삭제되는 경우 — vote_post_options 행 자체가 함께
--      cascade 삭제되므로 vote_count를 건드리든 안 건드리든 결과에 영향 없음
-- 따라서 DELETE를 단순히 no-op 처리해도 안전함
-- ===================================================

CREATE OR REPLACE FUNCTION sync_vote_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE vote_post_options SET vote_count = vote_count + 1 WHERE id = NEW.option_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        -- 의도적으로 감소시키지 않음 — 회원 탈퇴로 이력이 삭제돼도 총 투표수는 보존
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.option_id IS DISTINCT FROM NEW.option_id THEN
            UPDATE vote_post_options SET vote_count = vote_count - 1 WHERE id = OLD.option_id;
            UPDATE vote_post_options SET vote_count = vote_count + 1 WHERE id = NEW.option_id;
        END IF;
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;
