-- ===================================================
-- MBTI Metadata Seed
-- V7 슬림화 이후 mbti_metadata 테이블은 id 컬럼만 보유.
-- 표시 데이터(title, color_code 등)는 프론트엔드 정적 데이터로 관리.
-- ===================================================

INSERT INTO mbti_metadata (id) VALUES
    ('NONE'),
    -- Analysts (NT)
    ('INTJ'), ('INTP'), ('ENTJ'), ('ENTP'),
    -- Diplomats (NF)
    ('INFJ'), ('INFP'), ('ENFJ'), ('ENFP'),
    -- Sentinels (SJ)
    ('ISTJ'), ('ISFJ'), ('ESTJ'), ('ESFJ'),
    -- Explorers (SP)
    ('ISTP'), ('ISFP'), ('ESTP'), ('ESFP')
ON CONFLICT (id) DO NOTHING;
