ALTER TABLE posts
ADD COLUMN is_pinned BOOLEAN DEFAULT FALSE NOT NULL;

-- 목록 조회 시 고정글을 최상단에 노출하기 위해 정렬 컬럼(is_pinned) 포함하도록 재생성
DROP INDEX idx_posts_visible;

CREATE INDEX idx_posts_visible ON posts (post_type, is_pinned DESC, created_at DESC)
WHERE
    is_visible = TRUE;
