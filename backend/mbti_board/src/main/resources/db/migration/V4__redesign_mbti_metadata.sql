-- mbti_metadata 컬럼 재설계
-- icon_url, summary, description 제거 → title, descript(JSONB), personalities(JSONB), best_mbti, bad_mbti 추가

ALTER TABLE mbti_metadata
    DROP COLUMN IF EXISTS icon_url,
    DROP COLUMN IF EXISTS summary,
    DROP COLUMN IF EXISTS description,
    ADD COLUMN title        VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN descript     JSONB        NOT NULL DEFAULT '{}',
    -- { "title": "...", "content": "...", "icon": "..." }
    ADD COLUMN personalities JSONB       NOT NULL DEFAULT '[]',
    -- [{ "label": "...", "description": "...", "icon": "..." }, ...]
    ADD COLUMN best_mbti    VARCHAR(6)   CONSTRAINT fk_mbti_metadata_best REFERENCES mbti_metadata (id),
    ADD COLUMN bad_mbti     VARCHAR(6)   CONSTRAINT fk_mbti_metadata_bad  REFERENCES mbti_metadata (id);
