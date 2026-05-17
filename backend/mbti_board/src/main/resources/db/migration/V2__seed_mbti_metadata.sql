-- MBTI 메타데이터 시드
-- NONE: MBTI 미설정 사용자(비회원/OAuth 신규 가입) 전용 placeholder

INSERT INTO mbti_metadata (id, color_code, description, created_at, updated_at) VALUES
('NONE', '#A8A29E', 'MBTI 미설정', NOW(), NOW()),

-- Analysts (NT)
('INTJ', '#4A6FA5', '전략가', NOW(), NOW()),
('INTP', '#5B7FA6', '논리술사', NOW(), NOW()),
('ENTJ', '#3A5F8A', '통솔자', NOW(), NOW()),
('ENTP', '#4B7BA8', '변론가', NOW(), NOW()),

-- Diplomats (NF)
('INFJ', '#6B8F71', '옹호자', NOW(), NOW()),
('INFP', '#7DA87F', '중재자', NOW(), NOW()),
('ENFJ', '#5A8060', '선도자', NOW(), NOW()),
('ENFP', '#70A074', '활동가', NOW(), NOW()),

-- Sentinels (SJ)
('ISTJ', '#6B7FA8', '현실주의자', NOW(), NOW()),
('ISFJ', '#7D8FB8', '수호자', NOW(), NOW()),
('ESTJ', '#5A6F98', '경영자', NOW(), NOW()),
('ESFJ', '#6C82AC', '집정관', NOW(), NOW()),

-- Explorers (SP)
('ISTP', '#B8924A', '장인', NOW(), NOW()),
('ISFP', '#C4A05A', '모험가', NOW(), NOW()),
('ESTP', '#A87E3A', '기업가', NOW(), NOW()),
('ESFP', '#BAAB50', '연예인', NOW(), NOW());
