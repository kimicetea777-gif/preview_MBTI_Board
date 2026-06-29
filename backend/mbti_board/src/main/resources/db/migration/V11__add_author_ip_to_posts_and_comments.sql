-- 게시글·댓글 작성자의 IP를 저장 (어뷰징 방지 및 수사 협조 목적)
-- 회원·비회원 모두 저장. IPv6 포함 최대 45자.
ALTER TABLE posts    ADD COLUMN author_ip VARCHAR(45);
ALTER TABLE comments ADD COLUMN author_ip VARCHAR(45);
