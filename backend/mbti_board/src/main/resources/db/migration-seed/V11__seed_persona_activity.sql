-- 2026-07-21 ~ 2026-07-23 기준 활동 시드: 신규 페르소나 6명 + 게시글 7건 + 댓글 13건
-- auth_server_id는 실제 AS 계정과 매핑되지 않는 더미 UUID다(V13과 동일 원칙) — 콘텐츠 소유권 시드 용도
-- 이 파일이 단독으로(prod) 돌 수 있도록, 댓글 작성자로만 쓰이는 기존 유저 3명(a13/a17/a18)도
-- 여기서 함께 만든다. db/migration-dev/V6도 같은 3명을 만들기 때문에, 로컬 dev처럼 두 파일이
-- 함께 도는 환경에서는 ON CONFLICT DO NOTHING으로 중복 삽입을 건너뛴다.

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
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21',
        'isfj@mbti.dev',
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a21',
        '헌신적ISFJ',
        'ISFJ',
        'TESTED',
        'USER',
        'NORMAL',
        TIMESTAMPTZ '2026-07-21 09:00:00+09',
        TIMESTAMPTZ '2026-07-21 09:00:00+09'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        'esfp@mbti.dev',
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        '흥많은ESFP',
        'ESFP',
        'SELF',
        'USER',
        'NORMAL',
        TIMESTAMPTZ '2026-07-21 15:00:00+09',
        TIMESTAMPTZ '2026-07-21 15:00:00+09'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23',
        'esfj@mbti.dev',
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a23',
        '다정한ESFJ',
        'ESFJ',
        'TESTED',
        'USER',
        'NORMAL',
        TIMESTAMPTZ '2026-07-22 10:00:00+09',
        TIMESTAMPTZ '2026-07-22 10:00:00+09'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a24',
        'istp@mbti.dev',
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a24',
        '해결사ISTP',
        'ISTP',
        'SELF',
        'USER',
        'NORMAL',
        TIMESTAMPTZ '2026-07-22 13:00:00+09',
        TIMESTAMPTZ '2026-07-22 13:00:00+09'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a25',
        'intj@mbti.dev',
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a25',
        '전략가INTJ',
        'INTJ',
        'SELF',
        'USER',
        'NORMAL',
        TIMESTAMPTZ '2026-07-22 20:00:00+09',
        TIMESTAMPTZ '2026-07-22 20:00:00+09'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a26',
        'enfj@mbti.dev',
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a26',
        '분위기ENFJ',
        'ENFJ',
        'TESTED',
        'USER',
        'NORMAL',
        TIMESTAMPTZ '2026-07-23 08:00:00+09',
        TIMESTAMPTZ '2026-07-23 08:00:00+09'
    ),
    -- 아래 3명은 새 페르소나가 아니라, 그 이전부터 있던 기존 유저 — 새 게시글에 댓글로 반응하는 역할
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'entp@mbti.dev',
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        '논쟁꾼ENTP',
        'ENTP',
        'SELF',
        'USER',
        'NORMAL',
        TIMESTAMPTZ '2026-06-20 11:00:00+09',
        TIMESTAMPTZ '2026-06-20 11:00:00+09'
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
        TIMESTAMPTZ '2026-06-25 14:00:00+09',
        TIMESTAMPTZ '2026-06-25 14:00:00+09'
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
        TIMESTAMPTZ '2026-07-01 09:00:00+09',
        TIMESTAMPTZ '2026-07-01 09:00:00+09'
    )
ON CONFLICT (id) DO NOTHING;

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
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21',
        'ISFJ',
        0,
        0,
        0,
        0,
        'v1.0',
        TIMESTAMPTZ '2026-07-21 08:50:00+09'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23',
        'ESFJ',
        0,
        0,
        0,
        0,
        'v1.0',
        TIMESTAMPTZ '2026-07-22 09:50:00+09'
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a26',
        'ENFJ',
        0,
        0,
        0,
        0,
        'v1.0',
        TIMESTAMPTZ '2026-07-23 07:50:00+09'
    );

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
        12,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21',
        'ISFJ',
        NULL,
        'FREE',
        '다들 여행 갈 때 계획 세우고 가세요?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"저는 일정, 숙소, 식당까지 미리 다 정해놔야 마음이 편하더라고요. 즉흥적으로 다니시는 분들은 그게 어떻게 가능한지 궁금해요."}]}]}',
        18,
        TIMESTAMPTZ '2026-07-21 09:30:00+09',
        TIMESTAMPTZ '2026-07-21 09:30:00+09'
    ),
    (
        13,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        'ESFP',
        NULL,
        'FREE',
        '즉흥적으로 떠난 여행 후기 남깁니다',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"어제 갑자기 바다 보고 싶어서 기차표 끊고 바로 출발했어요. 계획 없이 다녀도 오히려 더 재밌는 것 같아요!"}]}]}',
        27,
        TIMESTAMPTZ '2026-07-21 16:10:00+09',
        TIMESTAMPTZ '2026-07-21 16:10:00+09'
    ),
    (
        15,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23',
        'ESFJ',
        NULL,
        'FREE',
        '회사에서 ESFJ로 산다는 것',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"동료들 챙기는 거 좋아하는데 정작 제 할 일이 밀려서 야근하는 날이 많아요. 다른 ESFJ분들은 어떻게 균형 잡으세요?"}]}]}',
        40,
        TIMESTAMPTZ '2026-07-22 14:20:00+09',
        TIMESTAMPTZ '2026-07-22 14:20:00+09'
    ),
    (
        17,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a26',
        'ENFJ',
        NULL,
        'FREE',
        '처음 뵙는 분들과도 금방 친해지는 편이신가요?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"저는 처음 본 사람이랑도 30분이면 친구가 되는데, 나중에 너무 많은 사람을 챙기려다 지칠 때가 있어요."}]}]}',
        22,
        TIMESTAMPTZ '2026-07-23 08:30:00+09',
        TIMESTAMPTZ '2026-07-23 08:30:00+09'
    ),
    (
        18,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21',
        'ISFJ',
        'ENFP',
        'FREE',
        'ISFJ가 보는 ENFP는 어떤 느낌인가요?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"저희 팀 ENFP 동료가 늘 밝고 에너지가 넘쳐서 챙겨주고 싶어져요. 다른 분들도 비슷한 경험 있으신가요?"}]}]}',
        15,
        TIMESTAMPTZ '2026-07-23 12:00:00+09',
        TIMESTAMPTZ '2026-07-23 12:00:00+09'
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
        14,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a25',
        'INTJ',
        NULL,
        'VOTE',
        '장기 계획 vs 즉흥 실행, 뭐가 더 나을까요?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"새 프로젝트를 시작할 때 큰 그림부터 짜는 편인가요, 일단 해보면서 조정하는 편인가요?"}]}]}',
        31,
        TIMESTAMPTZ '2026-07-22 11:00:00+09',
        TIMESTAMPTZ '2026-07-22 11:00:00+09'
    );

INSERT INTO
    vote_posts (id, ends_at, created_at)
VALUES (
        14,
        TIMESTAMPTZ '2026-08-05 11:00:00+09',
        TIMESTAMPTZ '2026-07-22 11:00:00+09'
    );

INSERT INTO
    vote_post_options (
        id,
        post_id,
        option_text,
        display_order,
        mbti_target
    )
VALUES (9, 14, '장기 계획부터 세운다', 1, NULL),
    (10, 14, '일단 해보고 조정한다', 2, NULL);

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
        16,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a24',
        'ISTP',
        NULL,
        'DEBATE',
        '감성보다 효율이 먼저 아닌가요?',
        '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"결정을 내릴 때 사람 감정보다 효율과 결과를 우선해야 한다고 생각하는데, 다들 어떻게 생각하세요?"}]}]}',
        29,
        TIMESTAMPTZ '2026-07-22 19:45:00+09',
        TIMESTAMPTZ '2026-07-22 19:45:00+09'
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
        16,
        '효율이 먼저다',
        '감정도 중요하다',
        TIMESTAMPTZ '2026-08-12 19:45:00+09',
        TIMESTAMPTZ '2026-07-22 19:45:00+09'
    );

-- 댓글
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
        21,
        12,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        'ESFP',
        '저는 무조건 즉흥이 좋아요! 계획 세우면 그대로 안 되더라고요 ㅋㅋ',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-21 10:15:00+09',
        TIMESTAMPTZ '2026-07-21 10:15:00+09'
    ),
    (
        22,
        12,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a25',
        'INTJ',
        '저는 계획 없이는 못 움직여요. 최소한의 동선은 짜야 마음이 편해요',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-21 11:40:00+09',
        TIMESTAMPTZ '2026-07-21 11:40:00+09'
    ),
    (
        23,
        13,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21',
        'ISFJ',
        '부럽습니다 저는 예약 안 하면 불안해서 못 가요 ㅠㅠ',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-21 17:00:00+09',
        TIMESTAMPTZ '2026-07-21 17:00:00+09'
    ),
    (
        24,
        14,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23',
        'ESFJ',
        '저는 장기 계획파예요, 팀원들이랑 맞추려면 미리 정해야 하더라고요',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-22 12:10:00+09',
        TIMESTAMPTZ '2026-07-22 12:10:00+09'
    ),
    (
        25,
        14,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a24',
        'ISTP',
        '일단 해보고 아니다 싶으면 바로 바꾸는 게 시간 아끼는 길이죠',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-22 12:50:00+09',
        TIMESTAMPTZ '2026-07-22 12:50:00+09'
    ),
    (
        26,
        15,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a26',
        'ENFJ',
        '완전 공감돼요, 다른 사람 신경 쓰다 보면 저를 잊을 때가 많아요',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-22 15:00:00+09',
        TIMESTAMPTZ '2026-07-22 15:00:00+09'
    ),
    (
        27,
        15,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
        'INTP',
        '그 상황에서 본인 기분도 챙기시는 연습이 필요할 것 같아요',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-22 16:30:00+09',
        TIMESTAMPTZ '2026-07-22 16:30:00+09'
    ),
    (
        28,
        16,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21',
        'ISFJ',
        '사람 마음까지 효율로만 따지면 관계가 삭막해지지 않을까요',
        NULL,
        -1,
        TRUE,
        TIMESTAMPTZ '2026-07-22 20:30:00+09',
        TIMESTAMPTZ '2026-07-22 20:30:00+09'
    ),
    (
        29,
        16,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a24',
        'ISTP',
        '감정도 결국 상황을 개선하기 위한 수단 아닌가요? 결과가 먼저죠',
        NULL,
        1,
        TRUE,
        TIMESTAMPTZ '2026-07-22 21:00:00+09',
        TIMESTAMPTZ '2026-07-22 21:00:00+09'
    ),
    (
        30,
        16,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        'ENTP',
        '효율을 추구하는 것도 결국 사람을 위한 거니까 나쁘지 않다고 봐요',
        NULL,
        1,
        TRUE,
        TIMESTAMPTZ '2026-07-22 21:30:00+09',
        TIMESTAMPTZ '2026-07-22 21:30:00+09'
    ),
    (
        31,
        17,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        'ESFP',
        '저도 낯가림 1도 없어서 금방 친해지는 편이에요!',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-23 09:00:00+09',
        TIMESTAMPTZ '2026-07-23 09:00:00+09'
    ),
    (
        32,
        18,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
        'ENFP',
        '저 ENFP인데 ISFJ분들이 챙겨주시면 진짜 감동이에요',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-23 12:40:00+09',
        TIMESTAMPTZ '2026-07-23 12:40:00+09'
    ),
    (
        33,
        18,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a26',
        'ENFJ',
        '저도 ISFJ 친구가 있는데 배려심이 남다르더라고요',
        NULL,
        NULL,
        TRUE,
        TIMESTAMPTZ '2026-07-23 13:10:00+09',
        TIMESTAMPTZ '2026-07-23 13:10:00+09'
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

SELECT setval(
        'comments_id_seq', (
            SELECT MAX(id)
            FROM comments
        )
    );
