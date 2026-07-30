-- 자유글
INSERT INTO
    posts (
        id,
        user_id,
        author_mbti,
        target_mbti,
        post_type,
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
        'FREE',
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
        'FREE',
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
        'FREE',
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
        'FREE',
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
        'FREE',
        'ISTJ와 ESTJ 조합의 팀워크는 어떨까요',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"프로젝트 팀에 ISTJ 2명, ESTJ 1명이 있는데 역할 분배가 어떻게 되면 좋을지 조언 부탁드려요."}]}]}',
        33,
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days'
    );

-- 투표
INSERT INTO
    posts (
        id,
        user_id,
        author_mbti,
        target_mbti,
        post_type,
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
        'VOTE',
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
        'VOTE',
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
        'VOTE',
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

-- 토론
INSERT INTO
    posts (
        id,
        user_id,
        author_mbti,
        target_mbti,
        post_type,
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
        'DEBATE',
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
        'DEBATE',
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
        'DEBATE',
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

-- 시퀀스 리셋 (BIGSERIAL 명시 삽입 후 충돌 방지)
SELECT setval(
        'posts_id_seq', (
            SELECT MAX(id)
            FROM posts
        )
    );

SELECT setval(
        'vote_post_options_id_seq', (
            SELECT MAX(id)
            FROM vote_post_options
        )
    );
