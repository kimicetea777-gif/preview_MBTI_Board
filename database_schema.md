# MBTI Community Database & Architecture Specification

본 문서는 MBTI 커뮤니티 플랫폼의 PostgreSQL 데이터베이스 스키마 및 Spring Boot JPA 매핑을 위한 명세서입니다. 모든 테이블의 기본키(PK)는 객체 지향 패러다임과의 호환성을 위해 `id`로 통일합니다.

---

## 도메인 구조 요약

| 테이블               | 설명                                      |
| -------------------- | ----------------------------------------- |
| `mbti_metadata`      | MBTI 16개 타입 + UNKNOWN 마스터 데이터    |
| `users`              | 회원 (소셜 로그인 지원, UUID PK)          |
| `mbti_test_history`  | MBTI 검사 이력                            |
| `post_types`         | 게시글 타입 마스터 (FREE / VOTE / DEBATE) |
| `posts`              | 게시글 (회원/비회원 공통)                 |
| `vote_posts`         | 투표 게시글 메타 (posts 1:1 확장)         |
| `vote_post_options`  | 투표 선택지 (MBTI 타입 또는 자유 텍스트)  |
| `vote_posts_history` | 유저 투표 내역 (1인 1표, 변경 가능)       |
| `debate_posts`       | 토론 게시글 메타 (posts 1:1 확장)         |
| `comments`           | 댓글/대댓글 + 토론 스탠스 통합            |
| `comment_reactions`  | 댓글 좋아요/싫어요                        |

## 주요 설계 결정 사항

- `posts.author_mbti`, `comments.author_mbti`: 작성 시점 MBTI 스냅샷 — 유저가 MBTI를 변경해도 과거 글은 원본 유지
- `comment_count`, `like_count`, `dislike_count`, `vote_count`: 비정규화 카운터 — DB 트리거로 자동 동기화
- 비회원 게시글/댓글: `guest_password`(BCrypt 해시)로 본인 확인
- 투표 선택지: `option_text`(필수) + `mbti_target`(MBTI 투표 시에만 FK 연결)
- `posts.content`: JSONB 타입 — RichContentEditor 구조화 데이터 저장

## 1. Domain DDL Schema

### 1.1 마스터 및 유저 도메인

```sql
-- 1. MBTI 마스터 데이터
CREATE TABLE mbti_metadata (
    id            VARCHAR(6)   PRIMARY KEY, -- 'NONE','INTP', 'ENFP' 등
    title         VARCHAR(100) NOT NULL,
    color_code    VARCHAR(7)   NOT NULL,
    image_url     VARCHAR(255),            -- Next.js public 기준 경로 (/images/mbti/icons/{id}.png), NONE은 NULL
    descript      JSONB        NOT NULL,    -- { "title": "...", "content": "...", "icon": "..." }
    personalities JSONB        NOT NULL DEFAULT '[]',
    -- [{ "label": "...", "description": "...", "icon": "..." }, ...]
    best_mbti     VARCHAR(6)   CONSTRAINT fk_mbti_metadata_best REFERENCES mbti_metadata (id),
    bad_mbti      VARCHAR(6)   CONSTRAINT fk_mbti_metadata_bad  REFERENCES mbti_metadata (id),
    created_at    TIMESTAMPTZ  DEFAULT NOW() NOT NULL,
    updated_at    TIMESTAMPTZ  DEFAULT NOW() NOT NULL
);

-- 2. 유저 테이블
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255), -- 소셜 로그인 고려 NULL 허용
    social_provider VARCHAR(20),
    provider_id VARCHAR(255),
    profile_image_url TEXT,
    nickname VARCHAR(50) UNIQUE NOT NULL,
    mbti_id VARCHAR(7) NOT NULL REFERENCES mbti_metadata(id) ON DELETE RESTRICT,

    -- 애플리케이션 Enum 매핑 컬럼
    mbti_verified VARCHAR(10) DEFAULT 'DEFAULT' NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' NOT NULL,
    state VARCHAR(20) DEFAULT 'NORMAL' NOT NULL,

    last_login_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT chk_mbti_verified CHECK (mbti_verified IN ('TESTED', 'SELF', 'DEFAULT')),
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'TESTER', 'USER', 'GUEST')),
    CONSTRAINT chk_user_state CHECK (state IN ('NORMAL', 'PENDING', 'INACTIVE', 'WITHDRAWN', 'LOCKED', 'SHADOW_BANNED', 'PERMANENT_BAN')),
    CONSTRAINT chk_social_login CHECK (
        (social_provider IS NULL AND provider_id IS NULL) OR
        (social_provider IS NOT NULL AND provider_id IS NOT NULL)
    )
);

-- 3. 회원 MBTI 검사 이력
CREATE TABLE mbti_test_history (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mbti_id VARCHAR(7) NOT NULL REFERENCES mbti_metadata(id) ON DELETE RESTRICT,
    ei_score SMALLINT NOT NULL CHECK (ei_score BETWEEN 0 AND 100),
    sn_score SMALLINT NOT NULL CHECK (sn_score BETWEEN 0 AND 100),
    tf_score SMALLINT NOT NULL CHECK (tf_score BETWEEN 0 AND 100),
    jp_score SMALLINT NOT NULL CHECK (jp_score BETWEEN 0 AND 100),
    test_version VARCHAR(10) DEFAULT 'v1.0' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- -----------게시판 및 확장 도메인---------------
-- 4. 게시글 타입 마스터
CREATE TABLE post_types (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL, -- 'FREE', 'VOTE', 'DEBATE'
    display_name VARCHAR(30) NOT NULL,
    description TEXT,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 5. 게시글 테이블
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID CONSTRAINT fk_posts_users REFERENCES users(id) ON DELETE SET NULL, -- 회원 탈퇴 시 글 보존
    author_mbti VARCHAR(7) NOT NULL REFERENCES mbti_metadata(id) ON DELETE RESTRICT, -- 작성 시점 MBTI 스냅샷
    type_id SMALLINT NOT NULL REFERENCES post_types(id) ON DELETE RESTRICT,
    title VARCHAR(150) NOT NULL,
    content JSONB NOT NULL, -- RichContentEditor 구조화 데이터 저장용
    guest_password VARCHAR(255), -- 비회원용 BCrypt 해시
    view_count INT DEFAULT 0 NOT NULL,
    comment_count INT DEFAULT 0 NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT chk_posts_guest_validation CHECK ((user_id IS NOT NULL) OR (user_id IS NULL AND guest_password IS NOT NULL))
);

-- 6. 투표 메타 정보 테이블 (posts와 1:1 관계)
CREATE TABLE vote_posts (
    id BIGINT PRIMARY KEY REFERENCES posts(id) ON DELETE CASCADE,
    ends_at TIMESTAMPTZ, -- 투표 마감일 (NULL이면 무기한)
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 7. 투표 선택지 테이블
CREATE TABLE vote_post_options (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES vote_posts(id) ON DELETE CASCADE,
    option_text VARCHAR(100) NOT NULL,                                       -- 항상 표시용 텍스트 (필수)
    mbti_target VARCHAR(7) REFERENCES mbti_metadata(id) ON DELETE RESTRICT, -- MBTI 투표일 때만 FK 연결, 자유 투표는 NULL
    display_order SMALLINT NOT NULL DEFAULT 0,                               -- 선택지 표시 순서
    vote_count INT DEFAULT 0 NOT NULL                                        -- 실시간 득표수 카운트 스냅샷
);

-- 8. 유저 투표 내역 테이블 (회원 전용, 1인 1표)
CREATE TABLE vote_posts_history (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    voted_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,  -- 최초 투표 시각
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL, -- 투표 변경 시각

    CONSTRAINT unique_user_vote_link UNIQUE (user_id, post_id),
    CONSTRAINT fk_vote_posts_history FOREIGN KEY (post_id) REFERENCES vote_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_options_history FOREIGN KEY (option_id) REFERENCES vote_post_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_users_vote_history FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 9. 토론 메타 정보 테이블 (posts와 1:1 관계)
CREATE TABLE debate_posts (
    id BIGINT PRIMARY KEY REFERENCES posts(id) ON DELETE CASCADE,
    pro_label VARCHAR(50) DEFAULT '찬성' NOT NULL, -- 매핑 스탠스: 1
    con_label VARCHAR(50) DEFAULT '반대' NOT NULL, -- 매핑 스탠스: -1
    ends_at TIMESTAMPTZ, -- 토론 마감 기한
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- -----------------댓글 및 상호작용 도메인----------------------
-- 10. 댓글 테이블 (대댓글 및 토론 진영 스탠스 통합)
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL, -- 회원 탈퇴 시 댓글 보존
    author_mbti VARCHAR(7) NOT NULL REFERENCES mbti_metadata(id) ON DELETE RESTRICT,  -- 작성 시점 MBTI 스냅샷.
    parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE, -- 대댓글 1단계 깊이 제한용 self-reference
    content TEXT NOT NULL,
    guest_password VARCHAR(255), -- 비회원 댓글 비밀번호 (BCrypt 해시)
    like_count INT DEFAULT 0 NOT NULL,
    dislike_count INT DEFAULT 0 NOT NULL,
    debate_stance SMALLINT CHECK (debate_stance IN (1, -1)), -- 1: 찬성, -1: 반대, 일반글은 NULL
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT chk_comments_guest_validation CHECK ((user_id IS NOT NULL) OR (user_id IS NULL AND guest_password IS NOT NULL))
);

-- 11. 댓글 액션 (좋아요 / 싫어요)
CREATE TABLE comment_reactions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction_type SMALLINT NOT NULL CHECK (reaction_type IN (1, -1)), -- 1: 좋아요, -1: 싫어요
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT unique_comment_user_reaction UNIQUE (comment_id, user_id)
);

-- ----------Database Trigger Architecture-----------
-- 댓글 추가/숨김/삭제 시 posts.comment_count 자동 동기화
CREATE OR REPLACE FUNCTION update_post_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE posts SET comment_count = comment_count - 1 WHERE id = OLD.post_id;
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        -- 숨김 처리 시 감소, 숨김 해제 시 증가
        IF OLD.is_visible = TRUE AND NEW.is_visible = FALSE THEN
            UPDATE posts SET comment_count = comment_count - 1 WHERE id = NEW.post_id;
        ELSIF OLD.is_visible = FALSE AND NEW.is_visible = TRUE THEN
            UPDATE posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
        END IF;
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comment_count
AFTER INSERT OR UPDATE OF is_visible OR DELETE ON comments
FOR EACH ROW EXECUTE FUNCTION update_post_comment_count();

-- 댓글 리액션 추가/변경/삭제 시 comments.like_count / dislike_count 자동 동기화
CREATE OR REPLACE FUNCTION update_comment_reaction_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.reaction_type = 1 THEN
            UPDATE comments SET like_count = like_count + 1 WHERE id = NEW.comment_id;
        ELSE
            UPDATE comments SET dislike_count = dislike_count + 1 WHERE id = NEW.comment_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.reaction_type = 1 THEN
            UPDATE comments SET like_count = like_count - 1 WHERE id = OLD.comment_id;
        ELSE
            UPDATE comments SET dislike_count = dislike_count - 1 WHERE id = OLD.comment_id;
        END IF;
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        -- 좋아요 → 싫어요
        IF OLD.reaction_type = 1 AND NEW.reaction_type = -1 THEN
            UPDATE comments SET like_count = like_count - 1, dislike_count = dislike_count + 1 WHERE id = NEW.comment_id;
        -- 싫어요 → 좋아요
        ELSIF OLD.reaction_type = -1 AND NEW.reaction_type = 1 THEN
            UPDATE comments SET dislike_count = dislike_count - 1, like_count = like_count + 1 WHERE id = NEW.comment_id;
        END IF;
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comment_reactions
AFTER INSERT OR UPDATE OF reaction_type OR DELETE ON comment_reactions
FOR EACH ROW EXECUTE FUNCTION update_comment_reaction_counts();

-- 투표 추가/변경/삭제 시 vote_post_options.vote_count 자동 동기화
CREATE OR REPLACE FUNCTION sync_vote_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE vote_post_options SET vote_count = vote_count + 1 WHERE id = NEW.option_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE vote_post_options SET vote_count = vote_count - 1 WHERE id = OLD.option_id;
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        -- 선택지 변경 시 기존 선택지 감소, 새 선택지 증가
        IF OLD.option_id IS DISTINCT FROM NEW.option_id THEN
            UPDATE vote_post_options SET vote_count = vote_count - 1 WHERE id = OLD.option_id;
            UPDATE vote_post_options SET vote_count = vote_count + 1 WHERE id = NEW.option_id;
        END IF;
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_vote_history
AFTER INSERT OR UPDATE OF option_id OR DELETE ON vote_posts_history
FOR EACH ROW EXECUTE FUNCTION sync_vote_count();

-- ----------Database Index Architecture-----------
-- 게시글 조회 및 성능 최적화 인덱스
CREATE INDEX idx_posts_user_id ON posts (user_id);
CREATE INDEX idx_posts_type_id ON posts (type_id);
CREATE INDEX idx_posts_mbti_created ON posts (author_mbti, created_at DESC);
CREATE INDEX idx_posts_comment_count ON posts (comment_count DESC);
CREATE INDEX idx_posts_view_count ON posts (view_count DESC);
CREATE INDEX idx_posts_visible ON posts (type_id, created_at DESC) WHERE is_visible = TRUE;

-- 계층형 댓글 조회 및 토론 화면 분할 최적화 인덱스
CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);
CREATE INDEX idx_comments_debate ON comments (post_id, debate_stance) WHERE debate_stance IS NOT NULL;
CREATE INDEX idx_comments_hierarchy ON comments (post_id, COALESCE(parent_id, id), created_at ASC);

-- 유저 및 이력 조회 성능 최적화 인덱스
CREATE UNIQUE INDEX idx_users_provider ON users (social_provider, provider_id) WHERE social_provider IS NOT NULL;
CREATE INDEX idx_users_mbti ON users (mbti_id);

CREATE INDEX idx_mbti_test_history_user_id_created_at ON mbti_test_history (user_id, created_at DESC);

CREATE INDEX idx_vote_history_option ON vote_posts_history (option_id);
CREATE INDEX idx_vote_history_user ON vote_posts_history (user_id);

-- 같은 투표에 동일 MBTI 선택지 중복 방지 (자유 텍스트 옵션은 제외)
CREATE UNIQUE INDEX idx_vote_options_mbti_unique ON vote_post_options (post_id, mbti_target) WHERE mbti_target IS NOT NULL;

CREATE INDEX idx_reactions_user_id ON comment_reactions (user_id);
CREATE INDEX idx_reactions_comment_id ON comment_reactions (comment_id);
```

---

## 2. Architectural & Implementation Rules for Claude Code

### 2.1 Id Unification Strategy (JPA Mapping)

모든 테이블은 데이터베이스 기본키 컬럼명으로 `id`를 사용합니다.

Spring Boot JPA 엔티티를 생성할 때 고유 식별자 필드는 `@Id`와 함께 `@Column(name = "id")`로 매핑하여 일관성을 유지해야 합니다.

`vote_posts`와 `debate_posts` 같은 1:1 확장 테이블은 `@MapsId`를 활용하여 부모 테이블(`posts`)의 고유 ID를 주키이자 외래키로 공유하도록 매핑합니다.

### 2.2 updated_at 자동 갱신 전략

`updated_at` 컬럼은 DB 트리거 없이 JPA `@UpdateTimestamp` 어노테이션으로 관리합니다. 해당 어노테이션이 선언된 엔티티는 UPDATE 시 JPA가 자동으로 현재 시각을 반영합니다.

적용 대상 엔티티: `User`, `Post`, `Comment`, `CommentReaction`, `VotePostsHistory`

### 2.3 Application-Level Enum Matching Strategies

데이터 정합성과 리소스 최적화를 위해 다음 상태값들은 별도의 테이블을 생성하지 않고 Spring Boot 애플리케이션 단에서 Enum 클래스로 관리하며, 엔티티 매핑 시 `@Enumerated(EnumType.STRING)`을 사용합니다.

**A. MbtiVerified** (MBTI 인증 상태 및 배지 컬러 정보 내장)

- `TESTED`: 사이트 내 검사를 통해 인증 완료 (colorCode: `#2E5C40`)
- `SELF`: 마이페이지 등에서 사용자가 직접 입력 (colorCode: `#7DB27A`)
- `DEFAULT`: MBTI 미설정 회원 또는 비회원의 기본 상태 (colorCode: `#A8A29E`)

**B. UserRole** (유저 권한 및 가중치 내장)

- `ADMIN`: 최고 관리자 (weight: 100)
- `TESTER`: 모니터링 및 테스트 유저 (weight: 50)
- `USER`: 일반 인증 회원 (weight: 10)
- `GUEST`: 가입 직후 또는 비회원 기본 권한 (weight: 1)

**C. UserState** (계정 생명주기 및 제재 상태)

- `NORMAL`: 정상 상태
- `PENDING`: 가입 대기 (이메일 인증 전)
- `INACTIVE`: 휴면 계정
- `WITHDRAWN`: 탈퇴 대기 상태
- `LOCKED`: 일시 보안 잠금
- `SHADOW_BANNED`: 쉐도우 밴 상태 (로그 감시 및 콘텐츠 노출 제한)
- `PERMANENT_BAN`: 영구 정지 상태
