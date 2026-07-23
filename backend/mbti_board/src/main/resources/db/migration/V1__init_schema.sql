-- ===================================================
-- SCHEMA INITIALIZATION
-- Consolidated from V1 ~ V15
-- ===================================================

-- ===================================================
-- 1. Master & User Domain
-- ===================================================

CREATE TABLE mbti_metadata (id VARCHAR(6) PRIMARY KEY);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    social_provider VARCHAR(20),
    provider_id VARCHAR(255),
    profile_image_url TEXT,
    nickname VARCHAR(12) UNIQUE NOT NULL,
    mbti_id VARCHAR(6) NOT NULL,
    mbti_verified VARCHAR(10) DEFAULT 'DEFAULT' NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' NOT NULL,
    state VARCHAR(20) DEFAULT 'NORMAL' NOT NULL,
    last_login_at TIMESTAMPTZ,
    recovery_email VARCHAR(255),
    withdrawn_at TIMESTAMPTZ,
    terms_agreed_at TIMESTAMPTZ,
    terms_version INT,
    privacy_agreed_at TIMESTAMPTZ,
    privacy_version INT,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_users_mbti_metadata FOREIGN KEY (mbti_id) REFERENCES mbti_metadata (id) ON DELETE RESTRICT,
    CONSTRAINT chk_mbti_verified CHECK (
        mbti_verified IN ('TESTED', 'SELF', 'DEFAULT')
    ),
    CONSTRAINT chk_user_role CHECK (
        role IN (
            'ADMIN',
            'TESTER',
            'USER',
            'GUEST'
        )
    ),
    CONSTRAINT chk_user_state CHECK (
        state IN (
            'NORMAL',
            'PENDING',
            'INACTIVE',
            'WITHDRAWN',
            'LOCKED',
            'SHADOW_BANNED',
            'PERMANENT_BAN'
        )
    ),
    CONSTRAINT chk_social_login CHECK (
        (
            social_provider IS NULL
            AND provider_id IS NULL
        )
        OR (
            social_provider IS NOT NULL
            AND provider_id IS NOT NULL
        )
    )
);

CREATE TABLE mbti_test_history (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    mbti_id VARCHAR(6) NOT NULL,
    ei_score SMALLINT NOT NULL CHECK (ei_score BETWEEN -16 AND 16),
    sn_score SMALLINT NOT NULL CHECK (sn_score BETWEEN -16 AND 16),
    tf_score SMALLINT NOT NULL CHECK (tf_score BETWEEN -16 AND 16),
    jp_score SMALLINT NOT NULL CHECK (jp_score BETWEEN -16 AND 16),
    test_version VARCHAR(10) DEFAULT 'v1.0' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_mbti_test_history_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_mbti_test_history_mbti_metadata FOREIGN KEY (mbti_id) REFERENCES mbti_metadata (id) ON DELETE RESTRICT
);

-- ===================================================
-- 2. Post Domain
-- ===================================================

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID,
    author_mbti VARCHAR(6) NOT NULL,
    target_mbti VARCHAR(6),
    post_type VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content JSONB NOT NULL,
    content_preview VARCHAR(300),
    image_urls TEXT [],
    guest_password VARCHAR(255),
    author_ip VARCHAR(45), -- 어뷰징 방지 및 수사 협조 목적, 회원·비회원 모두 저장 (IPv6 포함 최대 45자)
    view_count INT DEFAULT 0 NOT NULL,
    comment_count INT DEFAULT 0 NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_posts_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_mbti_metadata FOREIGN KEY (author_mbti) REFERENCES mbti_metadata (id) ON DELETE RESTRICT,
    CONSTRAINT fk_posts_target_mbti FOREIGN KEY (target_mbti) REFERENCES mbti_metadata (id) ON DELETE SET NULL,
    CONSTRAINT chk_posts_image_urls_max CHECK (
        image_urls IS NULL
        OR array_length(image_urls, 1) <= 3
    )
);

CREATE TABLE vote_posts (
    id BIGINT PRIMARY KEY,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_vote_posts_posts FOREIGN KEY (id) REFERENCES posts (id) ON DELETE CASCADE
);

CREATE TABLE vote_post_options (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    option_text VARCHAR(100),
    mbti_target VARCHAR(6),
    display_order SMALLINT NOT NULL DEFAULT 0,
    vote_count INT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_vote_post_options_vote_posts FOREIGN KEY (post_id) REFERENCES vote_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_post_options_mbti_metadata FOREIGN KEY (mbti_target) REFERENCES mbti_metadata (id) ON DELETE RESTRICT,
    CONSTRAINT chk_option_has_content CHECK (
        option_text IS NOT NULL
        OR mbti_target IS NOT NULL
    )
);

CREATE TABLE vote_posts_history (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    voted_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT unique_user_vote_link UNIQUE (user_id, post_id),
    CONSTRAINT fk_vote_posts_history FOREIGN KEY (post_id) REFERENCES vote_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_options_history FOREIGN KEY (option_id) REFERENCES vote_post_options (id) ON DELETE CASCADE,
    CONSTRAINT fk_users_vote_history FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE debate_posts (
    id BIGINT PRIMARY KEY,
    pro_label VARCHAR(50) DEFAULT '찬성' NOT NULL,
    con_label VARCHAR(50) DEFAULT '반대' NOT NULL,
    pro_count INT DEFAULT 0 NOT NULL,
    con_count INT DEFAULT 0 NOT NULL,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_debate_posts_posts FOREIGN KEY (id) REFERENCES posts (id) ON DELETE CASCADE
);

CREATE TABLE debate_stance_history (
    user_id UUID NOT NULL,
    post_id BIGINT NOT NULL,
    stance SMALLINT NOT NULL CHECK (stance IN (1, -1)),
    voted_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT pk_debate_stance_history PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_debate_stance_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_debate_stance_post FOREIGN KEY (post_id) REFERENCES debate_posts (id) ON DELETE CASCADE
);

-- ===================================================
-- 3. Comment & Reaction Domain
-- ===================================================

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id UUID,
    author_mbti VARCHAR(6) NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    guest_password VARCHAR(255),
    author_ip VARCHAR(45),
    like_count INT DEFAULT 0 NOT NULL,
    dislike_count INT DEFAULT 0 NOT NULL,
    debate_stance SMALLINT CHECK (debate_stance IN (1, -1)),
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_comments_posts FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_comments_mbti_metadata FOREIGN KEY (author_mbti) REFERENCES mbti_metadata (id) ON DELETE RESTRICT,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE
);

CREATE TABLE comment_reactions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    reaction_type SMALLINT NOT NULL CHECK (reaction_type IN (1, -1)),
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT unique_comment_user_reaction UNIQUE (comment_id, user_id),
    CONSTRAINT fk_comment_reactions_comments FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_reactions_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ===================================================
-- 4. MBTI Test Domain
-- ===================================================

CREATE TABLE mbti_questions (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(2) NOT NULL CHECK (
        category IN ('EI', 'SN', 'TF', 'JP')
    ),
    title TEXT NOT NULL,
    is_reverse BOOLEAN NOT NULL,
    test_version VARCHAR(10) DEFAULT 'v1.0' NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ===================================================
-- 5. Fun Question Domain
-- ===================================================

CREATE TABLE fun_questions (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    test_version VARCHAR(10) NOT NULL DEFAULT 'v1.0',
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE fun_question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_text VARCHAR(100) NOT NULL,
    display_order SMALLINT NOT NULL,
    CONSTRAINT fk_fun_question_options_question FOREIGN KEY (question_id) REFERENCES fun_questions (id) ON DELETE CASCADE
);

CREATE TABLE fun_response_counts (
    option_id BIGINT NOT NULL,
    mbti_id VARCHAR(6) NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (option_id, mbti_id),
    CONSTRAINT fk_fun_response_counts_option FOREIGN KEY (option_id) REFERENCES fun_question_options (id) ON DELETE CASCADE,
    CONSTRAINT fk_fun_response_counts_mbti FOREIGN KEY (mbti_id) REFERENCES mbti_metadata (id)
);

-- ===================================================
-- 6. Image Cleanup Domain
-- ===================================================

CREATE TABLE deleted_images (
    id BIGSERIAL PRIMARY KEY,
    url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    marked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_deleted_images_status CHECK (
        status IN ('PENDING', 'DELETE_REQUESTED')
    )
);

-- ===================================================
-- 7. Withdrawal Domain
-- ===================================================

CREATE TABLE withdrawal_log (
    id BIGSERIAL PRIMARY KEY,
    withdrawn_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ===================================================
-- 8. Audit & Retention Domain
-- 주의: actor_id/target_id/user_id는 users(id) FK를 의도적으로 걸지 않음 —
--       UserCleanupService가 탈퇴 회원을 주기적으로 하드 삭제(DELETE FROM users)하는데,
--       이 두 테이블은 그 이후에도(감사 로그 영구 보존 / 개인정보 12개월 법정 보존) 살아있어야 함.
--       FK를 걸면 CASCADE 삭제로 보존 목적 자체가 깨지므로 추가하지 말 것.
-- ===================================================

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID         NOT NULL,
    actor_role  VARCHAR(20)  NOT NULL,
    target_id   UUID,
    event       VARCHAR(50)  NOT NULL,
    payload     JSONB,
    ip          VARCHAR(45)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor_date ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_event_date ON audit_logs (event,    created_at DESC);

-- 탈퇴 회원의 이메일·전화번호·IP를 수사 협조 및 불법 재가입 방지 목적으로 암호화 보관 (12개월 후 파기)
CREATE TABLE user_retention_logs (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        UUID         NOT NULL,
    email_enc      TEXT         NOT NULL,
    withdrawal_ip  VARCHAR(45)  NOT NULL,
    withdrawn_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_retention_expires CHECK (expires_at > withdrawn_at)
);

CREATE INDEX idx_user_retention_logs_expires ON user_retention_logs (expires_at);

-- ===================================================
-- 9. Notice Domain
-- ===================================================

CREATE TABLE notices (
    id         BIGSERIAL    PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    content    TEXT         NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    priority   INT          NOT NULL DEFAULT 0,
    starts_at  TIMESTAMPTZ,
    ends_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ===================================================
-- 10. Triggers
-- ===================================================

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
        IF OLD.reaction_type = 1 AND NEW.reaction_type = -1 THEN
            UPDATE comments SET like_count = like_count - 1, dislike_count = dislike_count + 1 WHERE id = NEW.comment_id;
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

-- 신규 삽입 시에만 작성자 정보 검증 (ON DELETE SET NULL 캐스케이드 UPDATE는 대상 외)
CREATE OR REPLACE FUNCTION validate_post_author()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS NULL AND NEW.guest_password IS NULL THEN
        RAISE EXCEPTION 'post must have either user_id or guest_password';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_post_author
    BEFORE INSERT ON posts
    FOR EACH ROW EXECUTE FUNCTION validate_post_author();

CREATE OR REPLACE FUNCTION validate_comment_author()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS NULL AND NEW.guest_password IS NULL THEN
        RAISE EXCEPTION 'comment must have either user_id or guest_password';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_comment_author
    BEFORE INSERT ON comments
    FOR EACH ROW EXECUTE FUNCTION validate_comment_author();

-- ===================================================
-- 11. Indexes
-- ===================================================

CREATE INDEX idx_posts_user_id ON posts (user_id);

CREATE INDEX idx_posts_post_type ON posts (post_type);

CREATE INDEX idx_posts_comment_count ON posts (comment_count DESC);

CREATE INDEX idx_posts_view_count ON posts (view_count DESC);

CREATE INDEX idx_posts_visible ON posts (post_type, created_at DESC)
WHERE
    is_visible = TRUE;

CREATE INDEX idx_posts_target_mbti ON posts (target_mbti, created_at DESC)
WHERE
    target_mbti IS NOT NULL;

CREATE INDEX idx_comments_post_id ON comments (post_id);

CREATE INDEX idx_comments_parent_id ON comments (parent_id);

CREATE INDEX idx_comments_debate ON comments (post_id, debate_stance)
WHERE
    debate_stance IS NOT NULL;

CREATE INDEX idx_comments_hierarchy ON comments (
    post_id,
    COALESCE(parent_id, id),
    created_at ASC
);

CREATE UNIQUE INDEX idx_users_provider ON users (social_provider, provider_id)
WHERE
    social_provider IS NOT NULL;

CREATE UNIQUE INDEX idx_users_recovery_email ON users (recovery_email)
WHERE
    recovery_email IS NOT NULL;

CREATE INDEX idx_mbti_test_history_user_id_created_at ON mbti_test_history (user_id, created_at DESC);

CREATE INDEX idx_vote_history_post_user ON vote_posts_history (post_id, user_id);

CREATE UNIQUE INDEX idx_vote_options_mbti_unique ON vote_post_options (post_id, mbti_target)
WHERE
    mbti_target IS NOT NULL;

CREATE UNIQUE INDEX idx_vote_options_text_unique ON vote_post_options (post_id, option_text)
WHERE
    option_text IS NOT NULL;

CREATE INDEX idx_deleted_images_marked_at ON deleted_images (marked_at);

CREATE INDEX idx_deleted_images_status ON deleted_images (status);

CREATE INDEX idx_debate_stance_post ON debate_stance_history (post_id);

CREATE INDEX idx_fun_question_options_question_id ON fun_question_options (question_id);

-- ===================================================
-- 12. Seed
-- 이어지는 버전에서 실행:
--   V2  seed_fun_questions.sql       — FUN 문항 콘텐츠 (운영 필수)
--   V3  seed_mbti_metadata.sql       — MBTI 유형 참조 데이터 (운영 필수)
--   V5  seed_mbti_questions.sql      — MBTI 검사 문항 콘텐츠 (운영 필수)
--   dev/V4, V6~V10                   — 로컬 개발용 더미 데이터 (prod 프로필에서 제외, application-prod.yaml 참고)
-- ===================================================