-- post_types 마스터 데이터
-- id는 수동 관리 — 순서 변경 금지 (type_id FK로 posts에 연결됨)

INSERT INTO post_types (id, code, display_name, description, is_active, created_at) VALUES
(1, 'FREE',   '자유글', '자유롭게 글을 작성하는 게시판',       TRUE, NOW()),
(2, 'VOTE',   '투표',   'MBTI별 투표를 진행하는 게시판',       TRUE, NOW()),
(3, 'DEBATE', '토론',   '찬반 의견을 나누는 토론 게시판',      TRUE, NOW());
