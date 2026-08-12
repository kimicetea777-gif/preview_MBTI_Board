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

-- 시퀀스 리셋 (BIGSERIAL 명시 삽입 후 충돌 방지)
SELECT setval(
        'comments_id_seq', (
            SELECT MAX(id)
            FROM comments
        )
    );
