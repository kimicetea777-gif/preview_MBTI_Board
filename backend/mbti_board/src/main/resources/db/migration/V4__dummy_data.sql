-- ================================================================
-- V4__dummy_data.sql  –  개발/테스트용 더미 데이터
-- ================================================================
-- 테스트 계정 비밀번호: Test1234!
-- BCrypt 해시(cost=10): $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i
-- ⚠ 실제 로그인 테스트 시 BCryptPasswordEncoder.encode("Test1234!") 결과로 교체 권장
--
-- 삽입 순서 (FK 의존성):
--   users → mbti_test_history
--   posts → vote_posts → vote_post_options
--   posts → debate_posts
--   posts → comments  (트리거: posts.comment_count, debate_posts.pro/con_count 자동 갱신)
--   vote_posts_history  (트리거: vote_post_options.vote_count 자동 갱신)
--   comment_reactions   (트리거: comments.like/dislike_count 자동 갱신)
-- ================================================================

-- ----------------------------------------------------------------
-- 1. users  (10명)
-- ----------------------------------------------------------------
INSERT INTO users (
    id, email, password, social_provider, provider_id,
    nickname, mbti_id, mbti_verified, role, state,
    created_at, updated_at
) VALUES
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'admin@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '관리자', 'INFJ', 'DEFAULT', 'ADMIN', 'NORMAL',
        NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'infp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '조용한INFP', 'INFP', 'TESTED', 'USER', 'NORMAL',
        NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'entp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '논쟁꾼ENTP', 'ENTP', 'SELF', 'USER', 'NORMAL',
        NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        'estj@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '계획형ESTJ', 'ESTJ', 'TESTED', 'USER', 'NORMAL',
        NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
        'isfp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '자유로운ISFP', 'ISFP', 'SELF', 'USER', 'NORMAL',
        NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
        'entj@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '리더ENTJ', 'ENTJ', 'TESTED', 'USER', 'NORMAL',
        NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        'intp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '분석가INTP', 'INTP', 'TESTED', 'USER', 'NORMAL',
        NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        'enfp@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '열정ENFP', 'ENFP', 'SELF', 'USER', 'NORMAL',
        NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',
        'istj@mbti.dev',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i',
        NULL, NULL,
        '성실한ISTJ', 'ISTJ', 'TESTED', 'USER', 'NORMAL',
        NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a20',
        'social@gmail.com',
        NULL,
        'google', 'google_dummy_provider_12345',
        '소셜유저', 'NONE', 'DEFAULT', 'USER', 'NORMAL',
        NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'
    );

-- ----------------------------------------------------------------
-- 2. mbti_test_history
-- ----------------------------------------------------------------
-- 점수 해석: 낮을수록 앞 글자(E=높음/I=낮음, N=높음/S=낮음, F=낮음/T=높음, P=높음/J=낮음)
INSERT INTO mbti_test_history (user_id, mbti_id, ei_score, sn_score, tf_score, jp_score, test_version, created_at)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'INFP', 25, 35, 22, 63, 'v1.0', NOW() - INTERVAL '24 days'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'ESTJ', 74, 68, 72, 24, 'v1.0', NOW() - INTERVAL '17 days'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'ENTJ', 82, 65, 68, 28, 'v1.0', NOW() - INTERVAL '11 days'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'INTP', 22, 70, 45, 40, 'v1.0', NOW() - INTERVAL '9 days'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'ISTJ', 20, 66, 60, 26, 'v1.0', NOW() - INTERVAL '4 days');

-- ================================================================
-- 3. posts + 확장 테이블
-- ================================================================

-- ----------------------------------------------------------------
-- 3-1. 자유글 (type_id = 1)
-- ----------------------------------------------------------------
INSERT INTO posts (id, user_id, author_mbti, target_mbti, type_id, title, content, view_count, created_at, updated_at)
VALUES
    (
        1,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'INFJ', NULL, 1,
        'MBTI 게시판에 오신 걸 환영합니다!',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"안녕하세요, MBTI 게시판입니다. 자유롭게 이야기를 나눠보세요!"}]}]}',
        120, NOW() - INTERVAL '29 days', NOW() - INTERVAL '29 days'
    ),
    (
        2,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'INFP', 'INFP', 1,
        'INFP인데 공감되는 분?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"혼자 있을 때 가장 에너지가 충전되는데 INFP들은 다들 그런가요?"}]},{"type":"paragraph","content":[{"type":"text","text":"이런 성향이 직장 생활에서 좀 힘들 때가 있어요 ㅠㅠ"}]}]}',
        85, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'
    ),
    (
        3,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'ENFP', NULL, 1,
        'ENTP vs ENTJ 차이가 뭔가요',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"두 유형 모두 외향적이고 리더십이 있다고 하던데, 실제로 어떻게 다른지 궁금합니다."}]}]}',
        62, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'
    ),
    (
        4,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'ISFP', NULL, 1,
        '오늘 MBTI 테스트 다시 했더니 유형이 바뀌었어요',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"예전에는 ISFJ였는데 오늘 테스트하니까 INFJ가 나왔어요. 다들 유형이 바뀐 경험 있으신가요?"}]}]}',
        44, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'
    ),
    (
        5,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'ISTJ', NULL, 1,
        'ISTJ와 ESTJ 조합의 팀워크는 어떨까요',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"프로젝트 팀에 ISTJ 2명, ESTJ 1명이 있는데 역할 분배가 어떻게 되면 좋을지 조언 부탁드려요."}]}]}',
        33, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'
    );

-- ----------------------------------------------------------------
-- 3-2. 투표 (type_id = 2)
-- ----------------------------------------------------------------
INSERT INTO posts (id, user_id, author_mbti, target_mbti, type_id, title, content, view_count, created_at, updated_at)
VALUES
    (
        6,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'ENTJ', NULL, 2,
        '당신의 에너지 방향은?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"I vs E, 어느 쪽인가요?"}]}]}',
        210, NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days'
    ),
    (
        7,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ENTP', NULL, 2,
        'MBTI 테스트 결과를 믿나요?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"공식 유료 검사 vs 무료 온라인 테스트, 어느 쪽 결과를 더 신뢰하시나요?"}]}]}',
        155, NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'
    ),
    (
        8,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'ENFP', NULL, 2,
        '첫 만남에서 MBTI를 물어보는 것에 대해',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"처음 만나는 자리에서 MBTI를 물어보는 게 자연스럽다고 생각하시나요?"}]}]}',
        98, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'
    );

INSERT INTO vote_posts (id, ends_at, created_at)
VALUES
    (6, NOW() + INTERVAL '7 days',  NOW() - INTERVAL '22 days'),
    (7, NULL,                        NOW() - INTERVAL '14 days'),
    (8, NOW() + INTERVAL '14 days', NOW() - INTERVAL '7 days');

-- vote_count 는 vote_posts_history INSERT 시 트리거가 자동 갱신
INSERT INTO vote_post_options (id, post_id, option_text, display_order, mbti_target)
VALUES
    (1, 6, '내향형 (I)',          1, NULL),
    (2, 6, '외향형 (E)',          2, NULL),
    (3, 7, '공식 유료 검사가 맞다', 1, NULL),
    (4, 7, '무료 온라인도 충분해',  2, NULL),
    (5, 7, '둘 다 별로',           3, NULL),
    (6, 8, '자연스럽다',           1, NULL),
    (7, 8, '좀 어색하다',          2, NULL),
    (8, 8, '상황에 따라 다르다',   3, NULL);

-- ----------------------------------------------------------------
-- 3-3. 토론 (type_id = 3)
-- ----------------------------------------------------------------
INSERT INTO posts (id, user_id, author_mbti, target_mbti, type_id, title, content, view_count, created_at, updated_at)
VALUES
    (
        9,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ENTP', NULL, 3,
        'MBTI는 과학적으로 유효한가?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"MBTI가 심리학적으로 신뢰할 수 있는 도구인지 토론해봅시다."}]}]}',
        178, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'
    ),
    (
        10,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'ENTJ', NULL, 3,
        '내향형(I)이 외향형(E)보다 직장에서 불리한가?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"현대 직장 환경에서 내향형이 외향형보다 불이익을 받는다고 생각하시나요?"}]}]}',
        134, NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days'
    ),
    (
        11,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'ENFP', NULL, 3,
        'MBTI로 연인 궁합을 판단해도 될까?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"연애 초기에 MBTI 궁합표를 기준으로 상대를 평가하는 것이 합리적인지 토론해봐요."}]}]}',
        89, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'
    );

-- pro_count / con_count 는 comments INSERT 시 트리거가 자동 갱신
INSERT INTO debate_posts (id, pro_label, con_label, ends_at, created_at)
VALUES
    (9,  '유효하다',       '유효하지 않다',    NOW() + INTERVAL '30 days', NOW() - INTERVAL '18 days'),
    (10, '불리하다',       '상관없다',         NULL,                       NOW() - INTERVAL '11 days'),
    (11, '판단해도 된다',  '판단하면 안 된다', NOW() + INTERVAL '10 days', NOW() - INTERVAL '6 days');

-- ================================================================
-- 4. comments
-- ================================================================
-- comment_count(posts), like/dislike_count(comments), pro/con_count(debate_posts)
-- 는 모두 트리거가 자동 갱신 — 직접 지정 불필요
INSERT INTO comments (
    id, post_id, user_id, author_mbti, content, parent_id, debate_stance, is_visible,
    created_at, updated_at
) VALUES
    -- post 1 (자유글)
    (1,  1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'INFP',
     '반갑습니다! INFP입니다 ㅎㅎ',
     NULL, NULL, TRUE, NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days'),
    (2,  1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ENTP',
     '좋은 게시판이네요, 활성화되면 좋겠어요',
     NULL, NULL, TRUE, NOW() - INTERVAL '27 days', NOW() - INTERVAL '27 days'),
    (3,  1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'ESTJ',
     '저도 오늘부터 참여합니다!',
     NULL, NULL, TRUE, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),

    -- post 2 (자유글) + 대댓글
    (4,  2, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'INTP',
     '완전 공감이에요. 혼자 있는 시간이 진짜 소중하죠',
     NULL, NULL, TRUE, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
    (5,  2, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'INFP',
     'INTP분도 그러시군요! 저도 퇴근 후엔 혼자 있고 싶어요',
     4,    NULL, TRUE, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),

    -- post 3 (자유글)
    (6,  3, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ENTP',
     'ENTP는 가능성과 아이디어를 좋아하고, ENTJ는 실행과 결과를 중시해요',
     NULL, NULL, TRUE, NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),

    -- post 6 (투표 — debate_stance NULL)
    (7,  6, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'INFP',
     '당연히 I죠! 파티보다 집이 좋아요',
     NULL, NULL, TRUE, NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days'),
    (8,  6, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ENTP',
     'E입니다. 사람이 많을수록 에너지가 충전돼요!',
     NULL, NULL, TRUE, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
    (9,  6, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'ENTJ',
     'E가 맞는데 가끔은 혼자 있고 싶기도 해요',
     NULL, NULL, TRUE, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
    (10, 6, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'INTP',
     'I 100%',
     NULL, NULL, TRUE, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),
    (11, 6, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'ISFP',
     'I인데 E처럼 보인다는 말을 자주 들어요 ㅎ',
     NULL, NULL, TRUE, NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days'),

    -- post 9 (토론: 찬성=1, 반대=-1)
    (12, 9, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'ESTJ',
     '수십 년간 기업에서 사용해온 도구인데 전혀 근거 없다고 하기엔 무리가 있죠',
     NULL,  1, TRUE, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
    (13, 9, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ENTP',
     '신뢰도와 타당도 연구 결과들을 보면 과학적 기준을 충족하지 못해요',
     NULL, -1, TRUE, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
    (14, 9, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'INTP',
     '자기 인식 도구로서의 가치와 과학적 타당성은 분리해서 봐야 해요',
     NULL,  1, TRUE, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
    (15, 9, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'ENTJ',
     '심리학 논문에서도 신뢰도가 낮다는 결론이 많습니다',
     NULL, -1, TRUE, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),

    -- post 10 (토론)
    (16, 10, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'INFP',
     '발표나 미팅이 많은 환경에서는 확실히 E가 유리하다고 느껴요',
     NULL,  1, TRUE, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    (17, 10, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'ENTJ',
     '능력과 성과로만 평가받아야 하지, MBTI가 직장 성과와 무슨 관계입니까',
     NULL, -1, TRUE, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    (18, 10, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'ESTJ',
     '직무에 따라 다르지, 일률적으로 불리하다고 할 수 없어요',
     NULL, -1, TRUE, NOW() - INTERVAL '9 days',  NOW() - INTERVAL '9 days'),

    -- post 11 (토론)
    (19, 11, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'ENFP',
     '저는 상대방 MBTI 먼저 확인하고 잘 맞는지 보는 편이에요',
     NULL,  1, TRUE, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (20, 11, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'ISTJ',
     '사람을 유형으로 판단하는 건 편견일 수 있어요. 실제로 만나봐야 알죠',
     NULL, -1, TRUE, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days');

-- ================================================================
-- 5. vote_posts_history  (트리거 → vote_post_options.vote_count 갱신)
-- ================================================================
INSERT INTO vote_posts_history (post_id, option_id, user_id, voted_at, updated_at)
VALUES
    (6, 1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days'),
    (6, 1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days'),
    (6, 2, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
    (6, 2, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
    (7, 3, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
    (7, 4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
    (7, 4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
    (8, 6, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', NOW() - INTERVAL '6 days',  NOW() - INTERVAL '6 days'),
    (8, 8, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', NOW() - INTERVAL '6 days',  NOW() - INTERVAL '6 days');

-- ================================================================
-- 6. comment_reactions  (트리거 → comments.like_count / dislike_count 갱신)
-- ================================================================
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at, updated_at)
VALUES
    -- comment 1 좋아요 (5개)
    (1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',  1, NOW() - INTERVAL '27 days', NOW() - INTERVAL '27 days'),
    (1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',  1, NOW() - INTERVAL '27 days', NOW() - INTERVAL '27 days'),
    (1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',  1, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
    (1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',  1, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
    (1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',  1, NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days'),
    -- comment 4 좋아요 (7개)
    (4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',  1, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
    (4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',  1, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
    (4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',  1, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
    (4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',  1, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
    (4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',  1, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
    (4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a20',  1, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
    (4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',  1, NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days'),
    -- comment 6 싫어요 (1개)
    (6, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', -1, NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
    -- 토론 댓글 좋아요
    (12, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',  1, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
    (12, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',  1, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
    (13, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',  1, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
    (13, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',  1, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days');

-- ================================================================
-- 7. 시퀀스 리셋  (BIGSERIAL 명시 삽입 후 충돌 방지)
-- ================================================================
SELECT setval('posts_id_seq',             (SELECT MAX(id) FROM posts));
SELECT setval('vote_post_options_id_seq', (SELECT MAX(id) FROM vote_post_options));
SELECT setval('comments_id_seq',          (SELECT MAX(id) FROM comments));
