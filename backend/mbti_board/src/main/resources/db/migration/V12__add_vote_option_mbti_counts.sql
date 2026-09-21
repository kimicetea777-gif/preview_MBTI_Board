-- ===================================================
-- 투표 선택지별 MBTI 분포 사전 집계 테이블
-- fun_response_counts와 동일한 패턴: (option_id, mbti_id) 복합키 카운터를
-- 트리거로 유지 — 조회 시 JOIN/GROUP BY 없이 O(1) 읽기
--
-- 설계 원칙 — "몇 표를 던졌는지"가 아니라 "투표 시점에 어떤 MBTI들이 던졌는지"를
-- 남기는 영구 통계이므로, 아래 두 가지는 카운트를 되돌리지 않는다:
--   1. 유저가 MBTI를 재검사해 mbti_id가 바뀌어도, 이미 캐스팅된 투표는 투표 당시의
--      mbti로 계속 집계됨 (users를 다시 조회하지 않고 vote_posts_history.mbti_id
--      스냅샷만 사용 — 재검사 후 재투표 시에도 엉뚱한 버킷을 감소시키지 않기 위함)
--   2. 회원 탈퇴로 vote_posts_history가 삭제돼도(하단 참고) MBTI별 카운트는 감소시키지
--      않고 그대로 보존함
-- ===================================================

ALTER TABLE vote_posts_history ADD COLUMN mbti_id VARCHAR(6);

UPDATE vote_posts_history vh
SET mbti_id = u.mbti_id
FROM users u
WHERE u.id = vh.user_id;

ALTER TABLE vote_posts_history ALTER COLUMN mbti_id SET NOT NULL;

ALTER TABLE vote_posts_history
    ADD CONSTRAINT fk_vote_posts_history_mbti FOREIGN KEY (mbti_id) REFERENCES mbti_metadata (id);

CREATE TABLE vote_option_mbti_counts (
    option_id BIGINT NOT NULL,
    mbti_id VARCHAR(6) NOT NULL,
    vote_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (option_id, mbti_id),
    CONSTRAINT fk_vote_option_mbti_counts_option FOREIGN KEY (option_id) REFERENCES vote_post_options (id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_option_mbti_counts_mbti FOREIGN KEY (mbti_id) REFERENCES mbti_metadata (id)
);

-- 기존 vote_posts_history 데이터 백필 — 방금 채운 mbti_id 스냅샷 기준으로 집계
INSERT INTO vote_option_mbti_counts (option_id, mbti_id, vote_count)
SELECT vh.option_id, vh.mbti_id, COUNT(*)
FROM vote_posts_history vh
GROUP BY vh.option_id, vh.mbti_id;

-- 투표 등록/변경 시점의 mbti_id를 스냅샷 — 카운트 동기화 트리거가 users를
-- 다시 조회하지 않고 이 값만 참조하도록 함
CREATE OR REPLACE FUNCTION snapshot_vote_history_mbti()
RETURNS TRIGGER AS $$
BEGIN
    SELECT mbti_id INTO NEW.mbti_id FROM users WHERE id = NEW.user_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_vote_history_mbti_snapshot
BEFORE INSERT OR UPDATE OF option_id ON vote_posts_history
FOR EACH ROW EXECUTE FUNCTION snapshot_vote_history_mbti();

-- sync_vote_count와 별개 트리거 — OLD/NEW.mbti_id 스냅샷만으로 옵션×MBTI 카운트 증감
-- (users 재조회 없음 — mbti 재검사 후 불일치 문제 회피)
-- 주의: DELETE는 의도적으로 처리하지 않음 — 회원 탈퇴(UserCleanupService.bulkDeleteByState
-- → users ON DELETE CASCADE → vote_posts_history)로 이력이 지워져도 MBTI별 통계는
-- 감소시키지 않고 그대로 보존한다. V13에서 sync_vote_count(총 투표수)도 동일하게 DELETE를
-- no-op 처리하도록 맞춰서, 재투표(옵션 변경)든 회원 탈퇴든 옵션별 MBTI 합계와 vote_count는
-- 항상 일치한다 — 재투표는 둘 다 감소·증가, 탈퇴는 둘 다 보존.
CREATE OR REPLACE FUNCTION sync_vote_mbti_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO vote_option_mbti_counts (option_id, mbti_id, vote_count)
        VALUES (NEW.option_id, NEW.mbti_id, 1)
        ON CONFLICT (option_id, mbti_id) DO UPDATE
            SET vote_count = vote_option_mbti_counts.vote_count + 1;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.option_id IS DISTINCT FROM NEW.option_id THEN
            UPDATE vote_option_mbti_counts
            SET vote_count = GREATEST(0, vote_count - 1)
            WHERE option_id = OLD.option_id AND mbti_id = OLD.mbti_id;

            INSERT INTO vote_option_mbti_counts (option_id, mbti_id, vote_count)
            VALUES (NEW.option_id, NEW.mbti_id, 1)
            ON CONFLICT (option_id, mbti_id) DO UPDATE
                SET vote_count = vote_option_mbti_counts.vote_count + 1;
        END IF;
        RETURN NEW;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_vote_history_mbti_count
AFTER INSERT OR UPDATE OF option_id ON vote_posts_history
FOR EACH ROW EXECUTE FUNCTION sync_vote_mbti_count();
