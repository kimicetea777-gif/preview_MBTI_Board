-- ===================================================
-- 1. Master & User Domain
-- ===================================================

CREATE TABLE mbti_metadata (
    id VARCHAR(6) PRIMARY KEY,
    icon_url TEXT,
    color_code VARCHAR(7) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    social_provider VARCHAR(20),
    provider_id VARCHAR(255),
    profile_image_url TEXT,
    nickname VARCHAR(50) UNIQUE NOT NULL,
    mbti_id VARCHAR(6) NOT NULL,
    mbti_verified VARCHAR(10) DEFAULT 'DEFAULT' NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' NOT NULL,
    state VARCHAR(20) DEFAULT 'NORMAL' NOT NULL,
    last_login_at TIMESTAMPTZ,
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
    ei_score SMALLINT NOT NULL CHECK (ei_score BETWEEN 0 AND 100),
    sn_score SMALLINT NOT NULL CHECK (sn_score BETWEEN 0 AND 100),
    tf_score SMALLINT NOT NULL CHECK (tf_score BETWEEN 0 AND 100),
    jp_score SMALLINT NOT NULL CHECK (jp_score BETWEEN 0 AND 100),
    test_version VARCHAR(10) DEFAULT 'v1.0' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_mbti_test_history_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_mbti_test_history_mbti_metadata FOREIGN KEY (mbti_id) REFERENCES mbti_metadata (id) ON DELETE RESTRICT
);

-- ===================================================
-- 2. Post Domain
-- ===================================================

CREATE TABLE post_types (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    display_name VARCHAR(30) NOT NULL,
    description TEXT,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID,
    author_mbti VARCHAR(6) NOT NULL,
    type_id SMALLINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content JSONB NOT NULL,
    guest_password VARCHAR(255),
    view_count INT DEFAULT 0 NOT NULL,
    comment_count INT DEFAULT 0 NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_posts_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_mbti_metadata FOREIGN KEY (author_mbti) REFERENCES mbti_metadata (id) ON DELETE RESTRICT,
    CONSTRAINT fk_posts_post_types FOREIGN KEY (type_id) REFERENCES post_types (id) ON DELETE RESTRICT,
    CONSTRAINT chk_posts_guest_validation CHECK (
        (user_id IS NOT NULL)
        OR (
            user_id IS NULL
            AND guest_password IS NOT NULL
        )
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
    option_text VARCHAR(100) NOT NULL,
    mbti_target VARCHAR(6),
    display_order SMALLINT NOT NULL DEFAULT 0,
    vote_count INT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_vote_post_options_vote_posts FOREIGN KEY (post_id) REFERENCES vote_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_post_options_mbti_metadata FOREIGN KEY (mbti_target) REFERENCES mbti_metadata (id) ON DELETE RESTRICT
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
    like_count INT DEFAULT 0 NOT NULL,
    dislike_count INT DEFAULT 0 NOT NULL,
    debate_stance SMALLINT CHECK (debate_stance IN (1, -1)),
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_comments_posts FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_comments_mbti_metadata FOREIGN KEY (author_mbti) REFERENCES mbti_metadata (id) ON DELETE RESTRICT,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT chk_comments_guest_validation CHECK (
        (user_id IS NOT NULL)
        OR (
            user_id IS NULL
            AND guest_password IS NOT NULL
        )
    )
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
-- 4. Triggers
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

CREATE OR REPLACE FUNCTION update_debate_stance_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.debate_stance IS NOT NULL THEN
        IF NEW.debate_stance = 1 THEN
            UPDATE debate_posts SET pro_count = pro_count + 1 WHERE id = NEW.post_id;
        ELSE
            UPDATE debate_posts SET con_count = con_count + 1 WHERE id = NEW.post_id;
        END IF;
    ELSIF TG_OP = 'DELETE' AND OLD.debate_stance IS NOT NULL THEN
        IF OLD.debate_stance = 1 THEN
            UPDATE debate_posts SET pro_count = pro_count - 1 WHERE id = OLD.post_id;
        ELSE
            UPDATE debate_posts SET con_count = con_count - 1 WHERE id = OLD.post_id;
        END IF;
    ELSIF TG_OP = 'UPDATE' AND NEW.is_visible IS DISTINCT FROM OLD.is_visible AND NEW.debate_stance IS NOT NULL THEN
        IF NEW.debate_stance = 1 THEN
            UPDATE debate_posts SET pro_count = pro_count + (CASE WHEN NEW.is_visible THEN 1 ELSE -1 END) WHERE id = NEW.post_id;
        ELSE
            UPDATE debate_posts SET con_count = con_count + (CASE WHEN NEW.is_visible THEN 1 ELSE -1 END) WHERE id = NEW.post_id;
        END IF;
    END IF; 
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_debate_stance_count
AFTER INSERT OR UPDATE OF is_visible OR DELETE ON comments
FOR EACH ROW EXECUTE FUNCTION update_debate_stance_count();

-- ===================================================
-- 5. Indexes
-- ===================================================

CREATE INDEX idx_posts_user_id ON posts (user_id);

CREATE INDEX idx_posts_type_id ON posts (type_id);

CREATE INDEX idx_posts_mbti_created ON posts (author_mbti, created_at DESC);

CREATE INDEX idx_posts_comment_count ON posts (comment_count DESC);

CREATE INDEX idx_posts_view_count ON posts (view_count DESC);

CREATE INDEX idx_posts_visible ON posts (type_id, created_at DESC)
WHERE
    is_visible = TRUE;

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

CREATE INDEX idx_users_mbti ON users (mbti_id);

CREATE INDEX idx_mbti_test_history_user_id_created_at ON mbti_test_history (user_id, created_at DESC);

CREATE INDEX idx_vote_history_option ON vote_posts_history (option_id);

CREATE INDEX idx_vote_history_user ON vote_posts_history (user_id);

CREATE UNIQUE INDEX idx_vote_options_mbti_unique ON vote_post_options (post_id, mbti_target)
WHERE
    mbti_target IS NOT NULL;

CREATE INDEX idx_reactions_user_id ON comment_reactions (user_id);

CREATE INDEX idx_reactions_comment_id ON comment_reactions (comment_id);