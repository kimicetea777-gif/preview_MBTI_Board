-- ===================================================
-- 1. Master & User Domain
-- ===================================================

CREATE TABLE mbti_metadata (
    id VARCHAR(6) PRIMARY KEY,
    icon_url TEXT,
    color_code VARCHAR(7) NOT NULL,
    summary VARCHAR(100) NOT NULL,
    description JSONB NOT NULL,
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
    target_mbti VARCHAR(6),
    type_id SMALLINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content JSONB NOT NULL,
    content_preview VARCHAR(300),
    image_urls TEXT [] CONSTRAINT chk_posts_image_urls_max CHECK (
        image_urls IS NULL
        OR array_length(image_urls, 1) <= 3
    ),
    guest_password VARCHAR(255),
    view_count INT DEFAULT 0 NOT NULL,
    comment_count INT DEFAULT 0 NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_posts_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_mbti_metadata FOREIGN KEY (author_mbti) REFERENCES mbti_metadata (id) ON DELETE RESTRICT,
    CONSTRAINT fk_posts_target_mbti FOREIGN KEY (target_mbti) REFERENCES mbti_metadata (id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_post_types FOREIGN KEY (type_id) REFERENCES post_types (id) ON DELETE RESTRICT
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
-- 5. MBTI Test Domain
-- ===================================================

CREATE TABLE mbti_questions (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(2) NOT NULL CHECK (
        category IN ('EI', 'SN', 'TF', 'JP')
    ),
    title TEXT NOT NULL,
    is_reverse BOOLEAN NOT NULL,
    test_version VARCHAR(10) DEFAULT 'v1.0' NOT NULL
);

-- ===================================================
-- 6. Indexes
-- ===================================================

CREATE INDEX idx_posts_user_id ON posts (user_id);

CREATE INDEX idx_posts_type_id ON posts (type_id);

CREATE INDEX idx_posts_mbti_created ON posts (author_mbti, created_at DESC);

CREATE INDEX idx_posts_comment_count ON posts (comment_count DESC);

CREATE INDEX idx_posts_view_count ON posts (view_count DESC);

CREATE INDEX idx_posts_visible ON posts (type_id, created_at DESC)
WHERE
    is_visible = TRUE;

CREATE INDEX idx_posts_target_mbti ON posts (target_mbti)
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

CREATE INDEX idx_users_mbti ON users (mbti_id);

CREATE INDEX idx_mbti_test_history_user_id_created_at ON mbti_test_history (user_id, created_at DESC);

CREATE INDEX idx_vote_history_option ON vote_posts_history (option_id);

CREATE INDEX idx_vote_history_user ON vote_posts_history (user_id);

CREATE UNIQUE INDEX idx_vote_options_mbti_unique ON vote_post_options (post_id, mbti_target)
WHERE
    mbti_target IS NOT NULL;

CREATE INDEX idx_reactions_user_id ON comment_reactions (user_id);

CREATE INDEX idx_reactions_comment_id ON comment_reactions (comment_id);

-- ===================================================
-- 7. Image Cleanup Domain
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

CREATE INDEX idx_deleted_images_marked_at ON deleted_images (marked_at);

CREATE INDEX idx_deleted_images_status ON deleted_images (status);
-- ===================================================
-- 8. MBTI Metadata Seed
-- ===================================================

-- NONE: MBTI 미설정 사용자(비회원/OAuth 신규 가입) 전용 placeholder
INSERT INTO
    mbti_metadata (
        id,
        color_code,
        summary,
        description,
        created_at,
        updated_at
    )
VALUES (
        'NONE',
        '#A8A29E',
        'MBTI 미설정',
        '{"overview":"MBTI 미설정","traits":[],"strengths":[],"weaknesses":[],"compatible":[]}'::jsonb,
        NOW(),
        NOW()
    ),

-- Analysts (NT)
(
    'INTJ',
    '#4A6FA5',
    '전략적 사고와 강한 의지로 목표를 달성하는 전략가',
    '{"overview":"전략적 사고와 강한 의지로 목표를 달성하는 전략가","traits":["전략적","독립적","결단력"],"strengths":["목표 지향적","효율적인 계획 수립"],"weaknesses":["감정 표현 어려움","타인에게 냉담할 수 있음"],"compatible":["ENFP","ENTP"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'INTP',
    '#5B7FA6',
    '논리와 분석으로 세상을 탐구하는 논리술사',
    '{"overview":"논리와 분석으로 세상을 탐구하는 논리술사","traits":["논리적","창의적","분석적"],"strengths":["복잡한 문제 해결","독창적 아이디어"],"weaknesses":["우유부단","감정 표현 서툼"],"compatible":["ENTJ","ESTJ"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ENTJ',
    '#3A5F8A',
    '카리스마와 리더십으로 집단을 이끄는 통솔자',
    '{"overview":"카리스마와 리더십으로 집단을 이끄는 통솔자","traits":["리더십","자신감","결단력"],"strengths":["효율적인 조직 관리","장기 전략 수립"],"weaknesses":["고집스러움","감정 무시"],"compatible":["INTP","INFP"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ENTP',
    '#4B7BA8',
    '지적 도전을 즐기는 창의적인 변론가',
    '{"overview":"지적 도전을 즐기는 창의적인 변론가","traits":["창의적","논쟁적","호기심"],"strengths":["빠른 적응력","창의적 문제 해결"],"weaknesses":["집중력 부족","논쟁 과도"],"compatible":["INFJ","INTJ"]}'::jsonb,
    NOW(),
    NOW()
),

-- Diplomats (NF)
(
    'INFJ',
    '#6B8F71',
    '통찰력과 원칙으로 타인을 돕는 옹호자',
    '{"overview":"통찰력과 원칙으로 타인을 돕는 옹호자","traits":["통찰력","이타적","원칙적"],"strengths":["깊은 공감 능력","장기적 비전"],"weaknesses":["완벽주의","번아웃 취약"],"compatible":["ENFP","ENTP"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'INFP',
    '#7DA87F',
    '이상과 가치를 추구하는 열정적인 중재자',
    '{"overview":"이상과 가치를 추구하는 열정적인 중재자","traits":["이상주의","공감","창의적"],"strengths":["깊은 감수성","가치 중심 사고"],"weaknesses":["비현실적 기대","자기비판 과도"],"compatible":["ENFJ","ENTJ"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ENFJ',
    '#5A8060',
    '카리스마로 사람들에게 영감을 주는 선도자',
    '{"overview":"카리스마로 사람들에게 영감을 주는 선도자","traits":["카리스마","공감","리더십"],"strengths":["사람 동기 부여","조화로운 팀 구성"],"weaknesses":["타인 의존도 높음","자신 희생 과도"],"compatible":["INFP","ISFP"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ENFP',
    '#70A074',
    '열정과 창의력으로 가능성을 탐색하는 활동가',
    '{"overview":"열정과 창의력으로 가능성을 탐색하는 활동가","traits":["열정적","창의적","사교적"],"strengths":["아이디어 발산","사람 연결"],"weaknesses":["집중력 부족","마무리 취약"],"compatible":["INTJ","INFJ"]}'::jsonb,
    NOW(),
    NOW()
),

-- Sentinels (SJ)
(
    'ISTJ',
    '#6B7FA8',
    '신뢰와 책임감으로 규칙을 지키는 현실주의자',
    '{"overview":"신뢰와 책임감으로 규칙을 지키는 현실주의자","traits":["신뢰","책임감","꼼꼼함"],"strengths":["높은 책임감","체계적인 업무 처리"],"weaknesses":["변화 적응 어려움","융통성 부족"],"compatible":["ESFP","ESTP"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ISFJ',
    '#7D8FB8',
    '헌신과 배려로 주변을 돌보는 수호자',
    '{"overview":"헌신과 배려로 주변을 돌보는 수호자","traits":["헌신","배려","세심함"],"strengths":["타인 지원","안정적인 환경 유지"],"weaknesses":["자기주장 부족","변화 회피"],"compatible":["ESTP","ESFP"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ESTJ',
    '#5A6F98',
    '질서와 원칙으로 목표를 관리하는 경영자',
    '{"overview":"질서와 원칙으로 목표를 관리하는 경영자","traits":["조직력","원칙","실용적"],"strengths":["효율적인 관리","명확한 기준 제시"],"weaknesses":["융통성 부족","감정 소홀"],"compatible":["ISFP","ISTP"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ESFJ',
    '#6C82AC',
    '따뜻한 마음으로 공동체를 돌보는 집정관',
    '{"overview":"따뜻한 마음으로 공동체를 돌보는 집정관","traits":["따뜻함","사교적","책임감"],"strengths":["강한 유대감 형성","타인 배려"],"weaknesses":["비판에 민감","갈등 회피"],"compatible":["ISFP","ISTP"]}'::jsonb,
    NOW(),
    NOW()
),

-- Explorers (SP)
(
    'ISTP',
    '#B8924A',
    '논리와 실용성으로 문제를 해결하는 장인',
    '{"overview":"논리와 실용성으로 문제를 해결하는 장인","traits":["실용적","분석적","독립적"],"strengths":["손재주","위기 대처 능력"],"weaknesses":["감정 표현 어려움","장기 계획 취약"],"compatible":["ESFJ","ESTJ"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ISFP',
    '#C4A05A',
    '감성과 자유를 추구하는 호기심 많은 모험가',
    '{"overview":"감성과 자유를 추구하는 호기심 많은 모험가","traits":["감성적","자유로운","친절"],"strengths":["예술적 감각","유연한 사고"],"weaknesses":["자기주장 부족","장기 목표 설정 어려움"],"compatible":["ENFJ","ESFJ"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ESTP',
    '#A87E3A',
    '에너지와 실행력으로 현실을 즐기는 기업가',
    '{"overview":"에너지와 실행력으로 현실을 즐기는 기업가","traits":["행동력","담대함","현실적"],"strengths":["빠른 실행력","위기 대응"],"weaknesses":["충동적","장기 계획 부족"],"compatible":["ISFJ","ISTJ"]}'::jsonb,
    NOW(),
    NOW()
),
(
    'ESFP',
    '#BAAB50',
    '자발적 에너지로 주변을 밝히는 연예인',
    '{"overview":"자발적 에너지로 주변을 밝히는 연예인","traits":["활발","즉흥적","긍정적"],"strengths":["사람 에너지 상승","유연한 적응"],"weaknesses":["집중력 부족","장기 계획 취약"],"compatible":["ISFJ","ISTJ"]}'::jsonb,
    NOW(),
    NOW()
);

-- ===================================================
-- 9. Post Types Seed
-- ===================================================

-- id는 수동 관리 — 순서 변경 금지 (type_id FK로 posts에 연결됨)
INSERT INTO
    post_types (
        id,
        code,
        display_name,
        description,
        is_active,
        created_at
    )
VALUES (
        1,
        'FREE',
        '자유글',
        '자유롭게 글을 작성하는 게시판',
        TRUE,
        NOW()
    ),
    (
        2,
        'VOTE',
        '투표',
        'MBTI별 투표를 진행하는 게시판',
        TRUE,
        NOW()
    ),
    (
        3,
        'DEBATE',
        '토론',
        '찬반 의견을 나누는 토론 게시판',
        TRUE,
        NOW()
    );

-- ===================================================
-- 10. MBTI Questions Seed
-- ===================================================

INSERT INTO
    mbti_questions (
        category,
        title,
        is_reverse,
        test_version
    )
VALUES
    -- [일반 문항: 8개] 그렇다(양수) 선택 시 E(외향) 성향 누적
    (
        'EI',
        '새로운 사람들과 만나 통성명을 하고 대화를 나누는 것이 즐겁다.',
        false,
        'v1.0'
    ),
    (
        'EI',
        '주말에 집에만 있으면 답답하고, 어떻게든 밖으로 나가고 싶어진다.',
        false,
        'v1.0'
    ),
    (
        'EI',
        '여러 사람과 함께하는 단체 활동이나 모임에서 주로 에너지를 얻는다.',
        false,
        'v1.0'
    ),
    (
        'EI',
        '생각을 글로 정리하는 것보다 말로 표현하는 것이 더 편하고 빠르다.',
        false,
        'v1.0'
    ),
    (
        'EI',
        '관심사의 중심이 주로 외부 세계나 주변 사람들의 행동에 쏠려 있다.',
        false,
        'v1.0'
    ),
    (
        'EI',
        '축제나 파티처럼 시끌벅적하고 활기찬 분위기 속에 있는 것을 좋아한다.',
        false,
        'v1.0'
    ),
    (
        'EI',
        '처음 보는 사람에게도 먼저 스스럼없이 말을 건네는 편이다.',
        false,
        'v1.0'
    ),
    (
        'EI',
        '대화할 때 주로 이야기를 주도하거나 리액션을 크게 하며 분위기를 이끈다.',
        false,
        'v1.0'
    ),
    -- [역채점 문항: 8개] 그렇다(양수) 선택 시 부호가 바뀌어 I(내향) 성향 누적
    (
        'EI',
        '약속이 갑자기 취소되면 아쉽기보다는 솔직히 내심 기쁘다.',
        true,
        'v1.0'
    ),
    (
        'EI',
        '주변 사람들에게 나는 비교적 조용하고 신중한 편이라는 말을 자주 듣는다.',
        true,
        'v1.0'
    ),
    (
        'EI',
        '혼자만의 아늑한 공간에서 조용히 취미 생활을 할 때 가장 충전되는 기분이다.',
        true,
        'v1.0'
    ),
    (
        'EI',
        '모임에서 많은 사람의 주목을 한 번에 받으면 어쩐지 부담스럽고 어색하다.',
        true,
        'v1.0'
    ),
    (
        'EI',
        '속마음이나 개인적인 이야기를 타인에게 털어놓는 데 오랜 시간이 걸린다.',
        true,
        'v1.0'
    ),
    (
        'EI',
        '주말에 나가서 놀고 오면, 반드시 혼자 휴식하며 에너지를 채우는 시간이 필요하다.',
        true,
        'v1.0'
    ),
    (
        'EI',
        '넓고 얕은 인간관계보다는, 소수의 깊은 인간관계를 맺는 것을 선호한다.',
        true,
        'v1.0'
    ),
    (
        'EI',
        '말을 뱉기 전에 내 생각을 속으로 먼저 정리하고 검토하는 편이다.',
        true,
        'v1.0'
    ),
    -- [일반 문항: 8개] 그렇다 선택 시 S(감각) 성향 누적 / 현실, 데이터, 경험 중심
    (
        'SN',
        '어떤 일을 시작할 때, 막연한 아이디어보다는 구체적인 계획과 데이터가 있어야 안심이 된다.',
        false,
        'v1.0'
    ),
    (
        'SN',
        '설명서나 조립 가이드를 읽을 때, 쓰여 있는 단어 그대로 이해하고 정석대로 따라 하는 편이다.',
        false,
        'v1.0'
    ),
    (
        'SN',
        '과거나 미래에 대한 상상보다는, 지금 내 눈앞에 있는 현실과 현재의 삶이 더 중요하다고 생각한다.',
        false,
        'v1.0'
    ),
    (
        'SN',
        '새로운 이론을 배우는 것보다, 이미 검증된 실용적인 기술을 익혀서 바로 써먹는 것을 좋아한다.',
        false,
        'v1.0'
    ),
    (
        'SN',
        '이야기를 할 때 장황한 배경이나 비유보다는, 핵심적인 사실(Fact)과 결론 위주로 간단명료하게 말한다.',
        false,
        'v1.0'
    ),
    (
        'SN',
        '전통적인 방식이나 이미 검증된 익숙한 규칙을 따르는 것이 효율적이라고 믿는다.',
        false,
        'v1.0'
    ),
    (
        'SN',
        '주변 사물이나 환경의 변화(가구 배치, 친구의 헤어스타일 변화 등)를 비교적 잘 알아차린다.',
        false,
        'v1.0'
    ),
    (
        'SN',
        '영화를 볼 때 개연성이 떨어지거나 지나치게 비현실적인 판타지 설정이 나오면 몰입하기 힘들다.',
        false,
        'v1.0'
    ),
    -- [역채점 문항: 8개] 그렇다 선택 시 부호가 바뀌어 N(직관) 성향 누적 / 상상, 아이디어, 가능성 중심
    (
        'SN',
        '샤워를 하거나 버스를 타고 갈 때, 꼬리에 꼬리를 무는 엉뚱한 상상에 자주 빠지곤 한다.',
        true,
        'v1.0'
    ),
    (
        'SN',
        '반복적이고 뻔한 일상보다는, 매번 새로운 시도를 하거나 창의적인 아이디어를 내는 일이 더 즐겁다.',
        true,
        'v1.0'
    ),
    (
        'SN',
        '영화나 소설을 볼 때, 결말이 열린 결말이거나 다양한 해석의 여지가 남는 작품이 더 매력적이다.',
        true,
        'v1.0'
    ),
    (
        'SN',
        '"만약에 지구가 멸망한다면?" 같은 발생하지 않을 가상의 상황에 대해 이야기 나누는 것을 좋아한다.',
        true,
        'v1.0'
    ),
    (
        'SN',
        '글을 읽을 때 행간의 숨은 의미나 비유적인 표현을 파악하는 것에 흥미를 느낀다.',
        true,
        'v1.0'
    ),
    (
        'SN',
        '현재의 안정적인 상태에 안주하기보다는, 미래의 가능성과 잠재력에 투자하는 쪽을 선호한다.',
        true,
        'v1.0'
    ),
    (
        'SN',
        '전체적인 숲(맥락)을 먼저 보기 때문에, 때로는 세부적인 나무(디테일)를 놓쳐서 실수할 때가 있다.',
        true,
        'v1.0'
    ),
    (
        'SN',
        '기존의 방식대로 일하기보다는, 나만의 새로운 방식이나 지름길을 찾아내려고 시도하는 편이다.',
        true,
        'v1.0'
    ),
    -- [일반 문항: 8개] 그렇다 선택 시 T(사고) 성향 누적 / 논리, 사실, 인과관계, 해결 중심
    (
        'TF',
        '친구가 고민을 털어놓을 때, 감정적인 위로보다는 문제를 해결할 수 있는 현실적인 조언이 더 중요하다고 생각한다.',
        false,
        'v1.0'
    ),
    (
        'TF',
        '어떤 의견을 평가할 때, 말하는 사람의 기분이나 입장보다는 그 내용이 논리적으로 타당한지를 먼저 따진다.',
        false,
        'v1.0'
    ),
    (
        'TF',
        '갈등이 생겼을 때, 서운한 감정을 가라앉히기보다 무엇 때문에 문제가 발생했는지 인과관계를 명확히 규명하고 싶어 한다.',
        false,
        'v1.0'
    ),
    (
        'TF',
        '원칙을 지키는 것이 중요하며, 사사로운 사정이나 인간관계 때문에 규칙을 어기거나 예외를 두는 것을 좋아하지 않는다.',
        false,
        'v1.0'
    ),
    (
        'TF',
        '누군가 나에게 "일을 참 똑부러지고 유능하게 잘하네요"라고 칭찬해 줄 때 "착하고 좋은 사람이네요"라는 말보다 더 기쁘다.',
        false,
        'v1.0'
    ),
    (
        'TF',
        '친구가 열심히 준비한 시험이나 면접에서 떨어졌다고 하면, "속상하겠다"는 말보다 "무슨 실수를 했는지, 커트라인이 몇 점이었는지"가 먼저 궁금하다.',
        false,
        'v1.0'
    ),
    (
        'TF',
        '토론이나 회의를 할 때, 내 의견이 비판을 받더라도 그것이 객관적이고 정확한 지적이라면 기분 나빠하지 않고 수용한다.',
        false,
        'v1.0'
    ),
    (
        'TF',
        '영화나 드라마를 볼 때, 인물들의 감정선에 몰입해 눈물을 흘리기보다는 전체적인 플롯의 개연성이나 설정을 분석하며 보는 편이다.',
        false,
        'v1.0'
    ),
    -- [역채점 문항: 8개] 그렇다 선택 시 부호가 바뀌어 F(감정) 성향 누적 / 공감, 관계, 조화, 감정 중심
    (
        'TF',
        '대화할 때 대화의 내용(Fact) 자체도 중요하지만, 상대방의 말투, 표정, 분위기 등 정서적인 뉘앙스에 더 민감하게 반응한다.',
        true,
        'v1.0'
    ),
    (
        'TF',
        '친구가 "나 속상해서 빵 샀어"라고 말한다면, 왜 빵을 샀는지 이유를 묻기 전에 무슨 일 때문에 속상했는지 마음부터 먼저 헤아려준다.',
        true,
        'v1.0'
    ),
    (
        'TF',
        '내 의견이 옳다고 확신하더라도, 그 주장이 주변 사람들의 기분을 상하게 하거나 모임의 분위기를 해친다면 한 걸음 양보하는 편이다.',
        true,
        'v1.0'
    ),
    (
        'TF',
        '타인의 비판이나 쓴소리를 들으면 객관적인 조언인 줄 알면서도, 마음속으로 깊은 상처를 받거나 오랫동안 신경이 쓰인다.',
        true,
        'v1.0'
    ),
    (
        'TF',
        '주변 사람들의 기분이나 감정 상태를 잘 알아차리며, 슬프거나 기쁜 일에 깊게 동화되어 함께 울고 웃어주는 일이 많다.',
        true,
        'v1.0'
    ),
    (
        'TF',
        '누군가에게 거절이나 쓴소리를 해야 하는 상황이 오면, 상대방이 상처받을까 봐 미안한 마음에 말을 꺼내기가 무척 힘들다.',
        true,
        'v1.0'
    ),
    (
        'TF',
        '어떤 결정을 내릴 때 이성적인 이익이나 효율성보다는, 이 결정이 주변 사람들에게 어떤 영향을 미칠지(사람들의 마음)를 더 중요하게 고려한다.',
        true,
        'v1.0'
    ),
    (
        'TF',
        '칭찬을 들을 때 내 결과물의 퀄리티보다는, "너랑 같이 일해서 정말 든든하고 좋다" 같은 관계 중심의 격려를 들을 때 훨씬 보람을 느낀다.',
        true,
        'v1.0'
    ),
    -- [일반 문항: 8개] 그렇다 선택 시 J(판단) 성향 누적 / 계획, 체계, 정리, 마감 엄수
    (
        'JP',
        '여행을 갈 때 일자별, 시간대별로 방문할 장소와 식당 리스트를 미리 꼼꼼하게 정리해 두어야 마음이 편하다.',
        false,
        'v1.0'
    ),
    (
        'JP',
        '일을 하거나 공부를 시작하기 전, 해야 할 일들의 우선순위를 정하고 체크리스트(To-Do List)를 만드는 편이다.',
        false,
        'v1.0'
    ),
    (
        'JP',
        '정해진 마감 기한이나 약속 시간은 무슨 일이 있어도 엄격하게 지키려고 노력하며, 늦는 것을 극도로 싫어한다.',
        false,
        'v1.0'
    ),
    (
        'JP',
        '예상치 못한 돌발 상황이 생겨 내가 세워둔 기존 계획이나 일정이 틀어지면 큰 스트레스를 받는다.',
        false,
        'v1.0'
    ),
    (
        'JP',
        '과제나 업무를 할 때 마감 직전에 벼락치기를 하기보다는, 미리 일정을 분배해서 계획대로 차근차근 끝내는 편이다.',
        false,
        'v1.0'
    ),
    (
        'JP',
        '내 방 책상이나 파일 폴더 등이 용도에 맞게 잘 정리정돈되어 있을 때 정서적인 안정감을 느낀다.',
        false,
        'v1.0'
    ),
    (
        'JP',
        '어떤 일을 결정할 때 결론을 미루지 않고 빠르게 매듭지어 버리는 것이 속 편하다고 생각한다.',
        false,
        'v1.0'
    ),
    (
        'JP',
        '목표를 달성했을 때보다, 그 목표를 향해 계획적이고 체계적으로 과정을 통제해 나갈 때 더 큰 보람을 느낀다.',
        false,
        'v1.0'
    ),
    -- [역채점 문항: 8개] 그렇다 선택 시 부호가 바뀌어 P(인식) 성향 누적 / 즉흥, 유연함, 자율성, 상황 적응
    (
        'JP',
        '여행은 세부적인 시간표 없이, 큰 틀이나 목적지만 정해두고 현지 기분이나 상황에 따라 즉흥적으로 다니는 게 묘미다.',
        true,
        'v1.0'
    ),
    (
        'JP',
        '엄격한 규칙과 정해진 시간표에 맞춰 움직이는 것보다, 상황에 따라 내 자율성대로 자유롭게 일할 때 능률이 더 오른다.',
        true,
        'v1.0'
    ),
    (
        'JP',
        '마감 기한이 코앞으로 다가와서 벼락치기할 때, 평소보다 놀라운 집중력과 최고의 효율이 발휘된다고 느낀다.',
        true,
        'v1.0'
    ),
    (
        'JP',
        '미리 세워둔 계획이 틀어지더라도 "그럴 수도 있지, 다른 거 하면 돼" 하고 유연하고 긍정적으로 대처하는 편이다.',
        true,
        'v1.0'
    ),
    (
        'JP',
        '물건을 사용한 뒤 바로 제자리에 정리하기보다, 일단 편한 곳에 두고 나중에 한꺼번에 몰아서 청소하는 경우가 많다.',
        true,
        'v1.0'
    ),
    (
        'JP',
        '어떤 선택을 해야 할 때, 성급하게 결론을 내리기보다는 마지막 순간까지 최대한 많은 가능성과 정보를 열어두고 싶어 한다.',
        true,
        'v1.0'
    ),
    (
        'JP',
        '시험 공부나 프로젝트를 할 때 처음 계획은 거창하지만, 갈수록 계획대로 되지 않아 결국 흐지부지 변경할 때가 많다.',
        true,
        'v1.0'
    ),
    (
        'JP',
        '일상적인 대화를 하거나 생각을 할 때, 하나의 주제에 얽매이지 않고 흥미에 따라 자유롭게 다른 주제로 잘 넘어간다.',
        true,
        'v1.0'
    );

-- ===================================================
-- 11. Dev/Test 더미 데이터 (개발/테스트 전용)
-- ===================================================
-- 테스트 계정 비밀번호: Test1234!
-- BCrypt 해시(cost=10): $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i

-- ----------------------------------------------------------------
-- 11-1. users  (10명)
-- ----------------------------------------------------------------
INSERT INTO
    users (
        id,
        email,
        password,
        social_provider,
        provider_id,
        nickname,
        mbti_id,
        mbti_verified,
        role,
        state,
        created_at,
        updated_at
    )
VALUES (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'admin@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '관리자',
        'INFJ',
        'DEFAULT',
        'ADMIN',
        'NORMAL',
        NOW() - INTERVAL '30 days',
        NOW() - INTERVAL '30 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'infp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '조용한INFP',
        'INFP',
        'TESTED',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '25 days',
        NOW() - INTERVAL '25 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'entp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '논쟁꾼ENTP',
        'ENTP',
        'SELF',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        'estj@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '계획형ESTJ',
        'ESTJ',
        'TESTED',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '18 days',
        NOW() - INTERVAL '18 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
        'isfp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '자유로운ISFP',
        'ISFP',
        'SELF',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '15 days',
        NOW() - INTERVAL '15 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'entj@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '리더ENTJ',
        'ENTJ',
        'TESTED',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '12 days',
        NOW() - INTERVAL '12 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        'intp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '분석가INTP',
        'INTP',
        'TESTED',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '10 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        'enfp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '열정ENFP',
        'ENFP',
        'SELF',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '8 days',
        NOW() - INTERVAL '8 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',
        'istj@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL,
        NULL,
        '성실한ISTJ',
        'ISTJ',
        'TESTED',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a20',
        'social@gmail.com',
        NULL,
        'google',
        'google_dummy_provider_12345',
        '소셜유저',
        'NONE',
        'DEFAULT',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days'
    );

-- ----------------------------------------------------------------
-- 11-2. mbti_test_history
-- ----------------------------------------------------------------
INSERT INTO
    mbti_test_history (
        user_id,
        mbti_id,
        ei_score,
        sn_score,
        tf_score,
        jp_score,
        test_version,
        created_at
    )
VALUES (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'INFP',
        0,
        0,
        0,
        0,
        'v1.0',
        NOW() - INTERVAL '24 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        'ESTJ',
        0,
        0,
        0,
        0,
        'v1.0',
        NOW() - INTERVAL '17 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'ENTJ',
        0,
        0,
        0,
        0,
        'v1.0',
        NOW() - INTERVAL '11 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        'INTP',
        0,
        0,
        0,
        0,
        'v1.0',
        NOW() - INTERVAL '9 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',
        'ISTJ',
        0,
        0,
        0,
        0,
        'v1.0',
        NOW() - INTERVAL '4 days'
    );

-- ----------------------------------------------------------------
-- 11-3. posts + 확장 테이블
-- ----------------------------------------------------------------

-- 자유글 (type_id = 1)
INSERT INTO
    posts (
        id,
        user_id,
        author_mbti,
        target_mbti,
        type_id,
        title,
        content,
        view_count,
        created_at,
        updated_at
    )
VALUES (
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'INFJ',
        NULL,
        1,
        'MBTI 게시판에 오신 걸 환영합니다!',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"안녕하세요, MBTI 게시판입니다. 자유롭게 이야기를 나눠보세요!"}]}]}',
        120,
        NOW() - INTERVAL '29 days',
        NOW() - INTERVAL '29 days'
    ),
    (
        2,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'INFP',
        'INFP',
        1,
        'INFP인데 공감되는 분?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"혼자 있을 때 가장 에너지가 충전되는데 INFP들은 다들 그런가요?"}]},{"type":"paragraph","content":[{"type":"text","text":"이런 성향이 직장 생활에서 좀 힘들 때가 있어요 ㅠㅠ"}]}]}',
        85,
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days'
    ),
    (
        3,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        'ENFP',
        NULL,
        1,
        'ENTP vs ENTJ 차이가 뭔가요',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"두 유형 모두 외향적이고 리더십이 있다고 하던데, 실제로 어떻게 다른지 궁금합니다."}]}]}',
        62,
        NOW() - INTERVAL '15 days',
        NOW() - INTERVAL '15 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
        'ISFP',
        NULL,
        1,
        '오늘 MBTI 테스트 다시 했더니 유형이 바뀌었어요',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"예전에는 ISFJ였는데 오늘 테스트하니까 INFJ가 나왔어요. 다들 유형이 바뀐 경험 있으신가요?"}]}]}',
        44,
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '10 days'
    ),
    (
        5,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',
        'ISTJ',
        NULL,
        1,
        'ISTJ와 ESTJ 조합의 팀워크는 어떨까요',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"프로젝트 팀에 ISTJ 2명, ESTJ 1명이 있는데 역할 분배가 어떻게 되면 좋을지 조언 부탁드려요."}]}]}',
        33,
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days'
    );

-- 투표 (type_id = 2)
INSERT INTO
    posts (
        id,
        user_id,
        author_mbti,
        target_mbti,
        type_id,
        title,
        content,
        view_count,
        created_at,
        updated_at
    )
VALUES (
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'ENTJ',
        NULL,
        2,
        '당신의 에너지 방향은?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"I vs E, 어느 쪽인가요?"}]}]}',
        210,
        NOW() - INTERVAL '22 days',
        NOW() - INTERVAL '22 days'
    ),
    (
        7,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'ENTP',
        NULL,
        2,
        'MBTI 테스트 결과를 믿나요?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"공식 유료 검사 vs 무료 온라인 테스트, 어느 쪽 결과를 더 신뢰하시나요?"}]}]}',
        155,
        NOW() - INTERVAL '14 days',
        NOW() - INTERVAL '14 days'
    ),
    (
        8,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        'ENFP',
        NULL,
        2,
        '첫 만남에서 MBTI를 물어보는 것에 대해',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"처음 만나는 자리에서 MBTI를 물어보는 게 자연스럽다고 생각하시나요?"}]}]}',
        98,
        NOW() - INTERVAL '7 days',
        NOW() - INTERVAL '7 days'
    );

INSERT INTO
    vote_posts (id, ends_at, created_at)
VALUES (
        6,
        NOW() + INTERVAL '7 days',
        NOW() - INTERVAL '22 days'
    ),
    (
        7,
        NULL,
        NOW() - INTERVAL '14 days'
    ),
    (
        8,
        NOW() + INTERVAL '14 days',
        NOW() - INTERVAL '7 days'
    );

INSERT INTO
    vote_post_options (
        id,
        post_id,
        option_text,
        display_order,
        mbti_target
    )
VALUES (1, 6, '내향형 (I)', 1, NULL),
    (2, 6, '외향형 (E)', 2, NULL),
    (3, 7, '공식 유료 검사가 맞다', 1, NULL),
    (4, 7, '무료 온라인도 충분해', 2, NULL),
    (5, 7, '둘 다 별로', 3, NULL),
    (6, 8, '자연스럽다', 1, NULL),
    (7, 8, '좀 어색하다', 2, NULL),
    (8, 8, '상황에 따라 다르다', 3, NULL);

-- 토론 (type_id = 3)
INSERT INTO
    posts (
        id,
        user_id,
        author_mbti,
        target_mbti,
        type_id,
        title,
        content,
        view_count,
        created_at,
        updated_at
    )
VALUES (
        9,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'ENTP',
        NULL,
        3,
        'MBTI는 과학적으로 유효한가?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"MBTI가 심리학적으로 신뢰할 수 있는 도구인지 토론해봅시다."}]}]}',
        178,
        NOW() - INTERVAL '18 days',
        NOW() - INTERVAL '18 days'
    ),
    (
        10,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'ENTJ',
        NULL,
        3,
        '내향형(I)이 외향형(E)보다 직장에서 불리한가?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"현대 직장 환경에서 내향형이 외향형보다 불이익을 받는다고 생각하시나요?"}]}]}',
        134,
        NOW() - INTERVAL '11 days',
        NOW() - INTERVAL '11 days'
    ),
    (
        11,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        'ENFP',
        NULL,
        3,
        'MBTI로 연인 궁합을 판단해도 될까?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"연애 초기에 MBTI 궁합표를 기준으로 상대를 평가하는 것이 합리적인지 토론해봐요."}]}]}',
        89,
        NOW() - INTERVAL '6 days',
        NOW() - INTERVAL '6 days'
    );

INSERT INTO
    debate_posts (
        id,
        pro_label,
        con_label,
        ends_at,
        created_at
    )
VALUES (
        9,
        '유효하다',
        '유효하지 않다',
        NOW() + INTERVAL '30 days',
        NOW() - INTERVAL '18 days'
    ),
    (
        10,
        '불리하다',
        '상관없다',
        NULL,
        NOW() - INTERVAL '11 days'
    ),
    (
        11,
        '판단해도 된다',
        '판단하면 안 된다',
        NOW() + INTERVAL '10 days',
        NOW() - INTERVAL '6 days'
    );

-- ----------------------------------------------------------------
-- 11-4. comments
-- ----------------------------------------------------------------
INSERT INTO
    comments (
        id,
        post_id,
        user_id,
        author_mbti,
        content,
        parent_id,
        debate_stance,
        is_visible,
        created_at,
        updated_at
    )
VALUES (
        1,
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'INFP',
        '반갑습니다! INFP입니다 ㅎㅎ',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '28 days',
        NOW() - INTERVAL '28 days'
    ),
    (
        2,
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'ENTP',
        '좋은 게시판이네요, 활성화되면 좋겠어요',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '27 days',
        NOW() - INTERVAL '27 days'
    ),
    (
        3,
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        'ESTJ',
        '저도 오늘부터 참여합니다!',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '26 days',
        NOW() - INTERVAL '26 days'
    ),
    (
        4,
        2,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        'INTP',
        '완전 공감이에요. 혼자 있는 시간이 진짜 소중하죠',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '19 days',
        NOW() - INTERVAL '19 days'
    ),
    (
        5,
        2,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'INFP',
        'INTP분도 그러시군요! 저도 퇴근 후엔 혼자 있고 싶어요',
        4,
        NULL,
        TRUE,
        NOW() - INTERVAL '18 days',
        NOW() - INTERVAL '18 days'
    ),
    (
        6,
        3,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'ENTP',
        'ENTP는 가능성과 아이디어를 좋아하고, ENTJ는 실행과 결과를 중시해요',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '14 days',
        NOW() - INTERVAL '14 days'
    ),
    (
        7,
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'INFP',
        '당연히 I죠! 파티보다 집이 좋아요',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '21 days',
        NOW() - INTERVAL '21 days'
    ),
    (
        8,
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'ENTP',
        'E입니다. 사람이 많을수록 에너지가 충전돼요!',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days'
    ),
    (
        9,
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'ENTJ',
        'E가 맞는데 가끔은 혼자 있고 싶기도 해요',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days'
    ),
    (
        10,
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        'INTP',
        'I 100%',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '19 days',
        NOW() - INTERVAL '19 days'
    ),
    (
        11,
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
        'ISFP',
        'I인데 E처럼 보인다는 말을 자주 들어요 ㅎ',
        NULL,
        NULL,
        TRUE,
        NOW() - INTERVAL '19 days',
        NOW() - INTERVAL '19 days'
    ),
    (
        12,
        9,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        'ESTJ',
        '수십 년간 기업에서 사용해온 도구인데 전혀 근거 없다고 하기엔 무리가 있죠',
        NULL,
        1,
        TRUE,
        NOW() - INTERVAL '17 days',
        NOW() - INTERVAL '17 days'
    ),
    (
        13,
        9,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'ENTP',
        '신뢰도와 타당도 연구 결과들을 보면 과학적 기준을 충족하지 못해요',
        NULL,
        -1,
        TRUE,
        NOW() - INTERVAL '16 days',
        NOW() - INTERVAL '16 days'
    ),
    (
        14,
        9,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        'INTP',
        '자기 인식 도구로서의 가치와 과학적 타당성은 분리해서 봐야 해요',
        NULL,
        1,
        TRUE,
        NOW() - INTERVAL '16 days',
        NOW() - INTERVAL '16 days'
    ),
    (
        15,
        9,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'ENTJ',
        '심리학 논문에서도 신뢰도가 낮다는 결론이 많습니다',
        NULL,
        -1,
        TRUE,
        NOW() - INTERVAL '15 days',
        NOW() - INTERVAL '15 days'
    ),
    (
        16,
        10,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'INFP',
        '발표나 미팅이 많은 환경에서는 확실히 E가 유리하다고 느껴요',
        NULL,
        1,
        TRUE,
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '10 days'
    ),
    (
        17,
        10,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'ENTJ',
        '능력과 성과로만 평가받아야 하지, MBTI가 직장 성과와 무슨 관계입니까',
        NULL,
        -1,
        TRUE,
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '10 days'
    ),
    (
        18,
        10,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        'ESTJ',
        '직무에 따라 다르지, 일률적으로 불리하다고 할 수 없어요',
        NULL,
        -1,
        TRUE,
        NOW() - INTERVAL '9 days',
        NOW() - INTERVAL '9 days'
    ),
    (
        19,
        11,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        'ENFP',
        '저는 상대방 MBTI 먼저 확인하고 잘 맞는지 보는 편이에요',
        NULL,
        1,
        TRUE,
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days'
    ),
    (
        20,
        11,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',
        'ISTJ',
        '사람을 유형으로 판단하는 건 편견일 수 있어요. 실제로 만나봐야 알죠',
        NULL,
        -1,
        TRUE,
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days'
    );

-- ----------------------------------------------------------------
-- 11-5. vote_posts_history
-- ----------------------------------------------------------------
INSERT INTO
    vote_posts_history (
        post_id,
        option_id,
        user_id,
        voted_at,
        updated_at
    )
VALUES (
        6,
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        NOW() - INTERVAL '21 days',
        NOW() - INTERVAL '21 days'
    ),
    (
        6,
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        NOW() - INTERVAL '21 days',
        NOW() - INTERVAL '21 days'
    ),
    (
        6,
        2,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days'
    ),
    (
        6,
        2,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days'
    ),
    (
        7,
        3,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        NOW() - INTERVAL '13 days',
        NOW() - INTERVAL '13 days'
    ),
    (
        7,
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        NOW() - INTERVAL '13 days',
        NOW() - INTERVAL '13 days'
    ),
    (
        7,
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
        NOW() - INTERVAL '12 days',
        NOW() - INTERVAL '12 days'
    ),
    (
        8,
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        NOW() - INTERVAL '6 days',
        NOW() - INTERVAL '6 days'
    ),
    (
        8,
        8,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        NOW() - INTERVAL '6 days',
        NOW() - INTERVAL '6 days'
    );

-- ----------------------------------------------------------------
-- 11-6. comment_reactions
-- ----------------------------------------------------------------
INSERT INTO
    comment_reactions (
        comment_id,
        user_id,
        reaction_type,
        created_at,
        updated_at
    )
VALUES (
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        1,
        NOW() - INTERVAL '27 days',
        NOW() - INTERVAL '27 days'
    ),
    (
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        1,
        NOW() - INTERVAL '27 days',
        NOW() - INTERVAL '27 days'
    ),
    (
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        1,
        NOW() - INTERVAL '26 days',
        NOW() - INTERVAL '26 days'
    ),
    (
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        1,
        NOW() - INTERVAL '26 days',
        NOW() - INTERVAL '26 days'
    ),
    (
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        1,
        NOW() - INTERVAL '26 days',
        NOW() - INTERVAL '26 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        1,
        NOW() - INTERVAL '18 days',
        NOW() - INTERVAL '18 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
        1,
        NOW() - INTERVAL '18 days',
        NOW() - INTERVAL '18 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        1,
        NOW() - INTERVAL '18 days',
        NOW() - INTERVAL '18 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',
        1,
        NOW() - INTERVAL '17 days',
        NOW() - INTERVAL '17 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        1,
        NOW() - INTERVAL '17 days',
        NOW() - INTERVAL '17 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a20',
        1,
        NOW() - INTERVAL '17 days',
        NOW() - INTERVAL '17 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        1,
        NOW() - INTERVAL '17 days',
        NOW() - INTERVAL '17 days'
    ),
    (
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
        -1,
        NOW() - INTERVAL '13 days',
        NOW() - INTERVAL '13 days'
    ),
    (
        12,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        1,
        NOW() - INTERVAL '16 days',
        NOW() - INTERVAL '16 days'
    ),
    (
        12,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        1,
        NOW() - INTERVAL '16 days',
        NOW() - INTERVAL '16 days'
    ),
    (
        13,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        1,
        NOW() - INTERVAL '15 days',
        NOW() - INTERVAL '15 days'
    ),
    (
        13,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        1,
        NOW() - INTERVAL '15 days',
        NOW() - INTERVAL '15 days'
    );

-- ----------------------------------------------------------------
-- 11-7. 시퀀스 리셋 (BIGSERIAL 명시 삽입 후 충돌 방지)
-- ----------------------------------------------------------------
SELECT setval( 'posts_id_seq', ( SELECT MAX(id) FROM posts ) );

SELECT setval(
        'vote_post_options_id_seq', (
            SELECT MAX(id)
            FROM vote_post_options
        )
    );

SELECT setval( 'comments_id_seq', ( SELECT MAX(id) FROM comments ) );