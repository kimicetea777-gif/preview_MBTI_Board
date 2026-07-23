-- 테스트 계정 비밀번호: Test1234!
-- BCrypt 해시(cost=10): $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh8i

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
