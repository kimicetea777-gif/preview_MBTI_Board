-- ===================================================
-- 게시글-이미지 소유 관계 + Image Server 확정·정리 작업 상태
--
-- 1) 소유: 이미지 id는 공개 URL(mbti_board/{uuid}.webp)에 그대로 들어 있어서, 누구나 남의 글 이미지 URL을
--    자기 글 본문에 넣을 수 있다. 글 삭제·수정 시 "본문에 있는 이미지"를 release하면 원래 글의 이미지까지
--    지워지므로, 이 테이블에 기록된 "그 글이 소유한 이미지"만 정리한다.
--    image_id가 PK라 한 이미지는 한 글만 소유한다. 글 저장 시 INSERT ... ON CONFLICT DO NOTHING으로
--    먼저 등록한 글이 소유자가 되고, 이미 다른 글이 소유한 이미지는 본문에 있어도 소유하지 않는다.
--
-- 2) 작업 상태(outbox): Image Server confirm/release를 글 저장 트랜잭션 안에서 호출하지 않고, 이 행의
--    상태로 "해야 할 일"을 남긴 뒤 워커(PostImageSyncService)가 처리한다.
--      PENDING_CONFIRM  글에 붙었고 아직 확정 전 (워커가 confirm)
--      CONFIRMED        확정 완료
--      CONFIRM_FAILED   확정 불가로 결론(이미 만료·삭제됐거나 재시도 소진) — 이미지는 깨진 상태
--      PENDING_RELEASE  글에서 빠졌거나 글이 삭제됨 (워커가 release 후 행 삭제, 성공할 때까지 재시도)
--
-- 설계: Document/image_async_confirm_plan.md
-- ===================================================

CREATE TABLE post_images (
    image_id UUID PRIMARY KEY,
    -- 관리자 영구 삭제 후에도 워커가 release할 수 있도록 NULL 허용(아래 chk_post_images_orphan)
    post_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_CONFIRM',
    fail_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- CASCADE면 관리자 영구 삭제 시 워커가 release하기 전에 행이 사라져 R2 객체가 영영 남는다
    CONSTRAINT fk_post_images_posts FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE SET NULL,
    CONSTRAINT chk_post_images_status CHECK (
        status IN ('PENDING_CONFIRM', 'CONFIRMED', 'CONFIRM_FAILED', 'PENDING_RELEASE')
    ),
    -- 글이 사라진 행은 지우는 중인 상태만 허용
    CONSTRAINT chk_post_images_orphan CHECK (
        post_id IS NOT NULL OR status = 'PENDING_RELEASE'
    )
);

CREATE INDEX idx_post_images_post_id ON post_images (post_id);

-- 워커 조회 전용 부분 인덱스 — 대부분의 행(CONFIRMED)은 담지 않는다
CREATE INDEX idx_post_images_due ON post_images (next_attempt_at)
    WHERE status IN ('PENDING_CONFIRM', 'PENDING_RELEASE');

-- 기존 글 백필: image_urls(본문의 이미지 key, 최대 3개)에서 id를 뽑는다.
-- - 같은 id가 여러 글에 있으면 가장 먼저 작성된 글(id 최소)을 소유자로 본다 — 남의 이미지는 원래 글이
--   저장돼 URL이 공개된 뒤에야 넣을 수 있으므로 대부분 원래 글이 먼저다.
-- - PENDING_CONFIRM으로 넣어 배포 직후 워커가 한 번씩 confirm하게 한다(confirm은 멱등). 옛 코드가
--   수정 시 confirm하지 않아 아직 PENDING인 이미지를 GC 전에 살리고, 이미 지워진 이미지는
--   CONFIRM_FAILED로 드러난다.
-- - 본인이 삭제한 글(hidden_by = 'USER')은 이미지가 이미 release됐으므로 제외. 관리자가 숨긴 글은 포함.
INSERT INTO post_images (image_id, post_id, status)
SELECT DISTINCT ON (image_id) image_id, post_id, 'PENDING_CONFIRM'
FROM (
    SELECT substring(k.image_key FROM '^mbti_board/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.webp$')::uuid AS image_id,
           p.id AS post_id
    FROM posts p
    CROSS JOIN LATERAL unnest(p.image_urls) AS k(image_key)
    WHERE NOT (p.is_visible = FALSE AND p.hidden_by = 'USER')
) AS extracted
WHERE image_id IS NOT NULL
ORDER BY image_id, post_id;
