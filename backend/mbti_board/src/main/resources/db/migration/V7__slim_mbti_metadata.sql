-- mbti_metadata 슬림화 — id 컬럼만 유지, 표시 데이터는 프론트 정적 데이터로 이전

ALTER TABLE mbti_metadata
    DROP CONSTRAINT IF EXISTS fk_mbti_metadata_best,
    DROP CONSTRAINT IF EXISTS fk_mbti_metadata_bad,
    DROP COLUMN IF EXISTS best_mbti,
    DROP COLUMN IF EXISTS bad_mbti,
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS descript,
    DROP COLUMN IF EXISTS personalities,
    DROP COLUMN IF EXISTS color_code,
    DROP COLUMN IF EXISTS image_url,
    DROP COLUMN IF EXISTS created_at,
    DROP COLUMN IF EXISTS updated_at;
