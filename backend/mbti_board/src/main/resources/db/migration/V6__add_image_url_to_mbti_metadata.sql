-- mbti_metadata 테이블에 image_url 컬럼 추가
-- 이미지 경로: /images/mbti/icons/{id}.png (Next.js public 폴더 기준)

ALTER TABLE mbti_metadata
    ADD COLUMN image_url VARCHAR(255);

-- NONE은 이미지 없음 (NULL 유지)
UPDATE mbti_metadata SET image_url = '/images/mbti/icons/' || id || '.png'
WHERE id != 'NONE';
