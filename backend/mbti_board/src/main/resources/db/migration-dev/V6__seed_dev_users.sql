-- auth_server_id는 실제 AS(thebluebutter) 계정과 매핑되지 않는 더미 UUID다 — 이 시드 계정으로는
-- 실제 OAuth2 로그인을 할 수 없고, 게시글/댓글 등 콘텐츠 소유권 시드 용도로만 쓴다.
INSERT INTO
    users (
        id,
        email,
        auth_server_id,
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a19',
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
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a20',
        '소셜유저',
        'NONE',
        'DEFAULT',
        'USER',
        'NORMAL',
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days'
    );

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
