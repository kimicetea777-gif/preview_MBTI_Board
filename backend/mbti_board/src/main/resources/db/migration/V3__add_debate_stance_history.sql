-- 1. 기존 댓글 기반 토론 카운트 트리거 제거 (stance_history 기반으로 교체)
DROP TRIGGER IF EXISTS trg_debate_stance_count ON comments;
DROP FUNCTION IF EXISTS update_debate_stance_count();

-- 2. 기존 pro_count / con_count 초기화 (댓글 기반 집계 값 무효화)
UPDATE debate_posts SET pro_count = 0, con_count = 0;

-- 3. 사용자 토론 진영 투표 내역 테이블
CREATE TABLE debate_stance_history (
    user_id     UUID     NOT NULL,
    post_id     BIGINT   NOT NULL,
    stance      SMALLINT NOT NULL CHECK (stance IN (1, -1)),
    voted_at    TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at  TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT pk_debate_stance_history PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_debate_stance_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_debate_stance_post FOREIGN KEY (post_id)
        REFERENCES debate_posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_debate_stance_post ON debate_stance_history (post_id);

-- 4. debate_stance_history 변경 시 pro_count / con_count 자동 동기화 트리거
CREATE OR REPLACE FUNCTION sync_debate_stance_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.stance = 1 THEN
            UPDATE debate_posts SET pro_count = pro_count + 1 WHERE id = NEW.post_id;
        ELSE
            UPDATE debate_posts SET con_count = con_count + 1 WHERE id = NEW.post_id;
        END IF;

    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.stance = 1 THEN
            UPDATE debate_posts SET pro_count = GREATEST(0, pro_count - 1) WHERE id = OLD.post_id;
        ELSE
            UPDATE debate_posts SET con_count = GREATEST(0, con_count - 1) WHERE id = OLD.post_id;
        END IF;

    ELSIF TG_OP = 'UPDATE' AND OLD.stance IS DISTINCT FROM NEW.stance THEN
        IF NEW.stance = 1 THEN
            UPDATE debate_posts
            SET pro_count = pro_count + 1,
                con_count = GREATEST(0, con_count - 1)
            WHERE id = NEW.post_id;
        ELSE
            UPDATE debate_posts
            SET con_count = con_count + 1,
                pro_count = GREATEST(0, pro_count - 1)
            WHERE id = NEW.post_id;
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_debate_stance_history_count
AFTER INSERT OR UPDATE OF stance OR DELETE ON debate_stance_history
FOR EACH ROW EXECUTE FUNCTION sync_debate_stance_count();
