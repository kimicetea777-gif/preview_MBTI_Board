-- MBTI 메타데이터 시드
-- NONE: MBTI 미설정 사용자(비회원/OAuth 신규 가입) 전용 placeholder

INSERT INTO mbti_metadata (id, color_code, summary, description, created_at, updated_at) VALUES
('NONE', '#A8A29E', 'MBTI 미설정', '{"overview":"MBTI 미설정","traits":[],"strengths":[],"weaknesses":[],"compatible":[]}'::jsonb, NOW(), NOW()),

-- Analysts (NT)
('INTJ', '#4A6FA5', '전략적 사고와 강한 의지로 목표를 달성하는 전략가', '{"overview":"전략적 사고와 강한 의지로 목표를 달성하는 전략가","traits":["전략적","독립적","결단력"],"strengths":["목표 지향적","효율적인 계획 수립"],"weaknesses":["감정 표현 어려움","타인에게 냉담할 수 있음"],"compatible":["ENFP","ENTP"]}'::jsonb, NOW(), NOW()),
('INTP', '#5B7FA6', '논리와 분석으로 세상을 탐구하는 논리술사', '{"overview":"논리와 분석으로 세상을 탐구하는 논리술사","traits":["논리적","창의적","분석적"],"strengths":["복잡한 문제 해결","독창적 아이디어"],"weaknesses":["우유부단","감정 표현 서툼"],"compatible":["ENTJ","ESTJ"]}'::jsonb, NOW(), NOW()),
('ENTJ', '#3A5F8A', '카리스마와 리더십으로 집단을 이끄는 통솔자', '{"overview":"카리스마와 리더십으로 집단을 이끄는 통솔자","traits":["리더십","자신감","결단력"],"strengths":["효율적인 조직 관리","장기 전략 수립"],"weaknesses":["고집스러움","감정 무시"],"compatible":["INTP","INFP"]}'::jsonb, NOW(), NOW()),
('ENTP', '#4B7BA8', '지적 도전을 즐기는 창의적인 변론가', '{"overview":"지적 도전을 즐기는 창의적인 변론가","traits":["창의적","논쟁적","호기심"],"strengths":["빠른 적응력","창의적 문제 해결"],"weaknesses":["집중력 부족","논쟁 과도"],"compatible":["INFJ","INTJ"]}'::jsonb, NOW(), NOW()),

-- Diplomats (NF)
('INFJ', '#6B8F71', '통찰력과 원칙으로 타인을 돕는 옹호자', '{"overview":"통찰력과 원칙으로 타인을 돕는 옹호자","traits":["통찰력","이타적","원칙적"],"strengths":["깊은 공감 능력","장기적 비전"],"weaknesses":["완벽주의","번아웃 취약"],"compatible":["ENFP","ENTP"]}'::jsonb, NOW(), NOW()),
('INFP', '#7DA87F', '이상과 가치를 추구하는 열정적인 중재자', '{"overview":"이상과 가치를 추구하는 열정적인 중재자","traits":["이상주의","공감","창의적"],"strengths":["깊은 감수성","가치 중심 사고"],"weaknesses":["비현실적 기대","자기비판 과도"],"compatible":["ENFJ","ENTJ"]}'::jsonb, NOW(), NOW()),
('ENFJ', '#5A8060', '카리스마로 사람들에게 영감을 주는 선도자', '{"overview":"카리스마로 사람들에게 영감을 주는 선도자","traits":["카리스마","공감","리더십"],"strengths":["사람 동기 부여","조화로운 팀 구성"],"weaknesses":["타인 의존도 높음","자신 희생 과도"],"compatible":["INFP","ISFP"]}'::jsonb, NOW(), NOW()),
('ENFP', '#70A074', '열정과 창의력으로 가능성을 탐색하는 활동가', '{"overview":"열정과 창의력으로 가능성을 탐색하는 활동가","traits":["열정적","창의적","사교적"],"strengths":["아이디어 발산","사람 연결"],"weaknesses":["집중력 부족","마무리 취약"],"compatible":["INTJ","INFJ"]}'::jsonb, NOW(), NOW()),

-- Sentinels (SJ)
('ISTJ', '#6B7FA8', '신뢰와 책임감으로 규칙을 지키는 현실주의자', '{"overview":"신뢰와 책임감으로 규칙을 지키는 현실주의자","traits":["신뢰","책임감","꼼꼼함"],"strengths":["높은 책임감","체계적인 업무 처리"],"weaknesses":["변화 적응 어려움","융통성 부족"],"compatible":["ESFP","ESTP"]}'::jsonb, NOW(), NOW()),
('ISFJ', '#7D8FB8', '헌신과 배려로 주변을 돌보는 수호자', '{"overview":"헌신과 배려로 주변을 돌보는 수호자","traits":["헌신","배려","세심함"],"strengths":["타인 지원","안정적인 환경 유지"],"weaknesses":["자기주장 부족","변화 회피"],"compatible":["ESTP","ESFP"]}'::jsonb, NOW(), NOW()),
('ESTJ', '#5A6F98', '질서와 원칙으로 목표를 관리하는 경영자', '{"overview":"질서와 원칙으로 목표를 관리하는 경영자","traits":["조직력","원칙","실용적"],"strengths":["효율적인 관리","명확한 기준 제시"],"weaknesses":["융통성 부족","감정 소홀"],"compatible":["ISFP","ISTP"]}'::jsonb, NOW(), NOW()),
('ESFJ', '#6C82AC', '따뜻한 마음으로 공동체를 돌보는 집정관', '{"overview":"따뜻한 마음으로 공동체를 돌보는 집정관","traits":["따뜻함","사교적","책임감"],"strengths":["강한 유대감 형성","타인 배려"],"weaknesses":["비판에 민감","갈등 회피"],"compatible":["ISFP","ISTP"]}'::jsonb, NOW(), NOW()),

-- Explorers (SP)
('ISTP', '#B8924A', '논리와 실용성으로 문제를 해결하는 장인', '{"overview":"논리와 실용성으로 문제를 해결하는 장인","traits":["실용적","분석적","독립적"],"strengths":["손재주","위기 대처 능력"],"weaknesses":["감정 표현 어려움","장기 계획 취약"],"compatible":["ESFJ","ESTJ"]}'::jsonb, NOW(), NOW()),
('ISFP', '#C4A05A', '감성과 자유를 추구하는 호기심 많은 모험가', '{"overview":"감성과 자유를 추구하는 호기심 많은 모험가","traits":["감성적","자유로운","친절"],"strengths":["예술적 감각","유연한 사고"],"weaknesses":["자기주장 부족","장기 목표 설정 어려움"],"compatible":["ENFJ","ESFJ"]}'::jsonb, NOW(), NOW()),
('ESTP', '#A87E3A', '에너지와 실행력으로 현실을 즐기는 기업가', '{"overview":"에너지와 실행력으로 현실을 즐기는 기업가","traits":["행동력","담대함","현실적"],"strengths":["빠른 실행력","위기 대응"],"weaknesses":["충동적","장기 계획 부족"],"compatible":["ISFJ","ISTJ"]}'::jsonb, NOW(), NOW()),
('ESFP', '#BAAB50', '자발적 에너지로 주변을 밝히는 연예인', '{"overview":"자발적 에너지로 주변을 밝히는 연예인","traits":["활발","즉흥적","긍정적"],"strengths":["사람 에너지 상승","유연한 적응"],"weaknesses":["집중력 부족","장기 계획 취약"],"compatible":["ISFJ","ISTJ"]}'::jsonb, NOW(), NOW());
