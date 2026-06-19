-- mbti_metadata 시드 데이터 (16개 유형 + NONE)
-- best_mbti / bad_mbti 자기 참조 FK 때문에 2단계로 삽입

-- ① 기본 데이터 삽입 (best_mbti / bad_mbti 는 UPDATE에서 처리)
INSERT INTO
    mbti_metadata (
        id,
        title,
        color_code,
        descript,
        personalities,
        created_at,
        updated_at
    )
VALUES (
        'NONE',
        'MBTI 미설정',
        '#A8A29E',
        '{"title":"아직 유형이 없어요","content":"MBTI 검사를 통해 자신의 성격 유형을 알아보세요. 검사는 총 32문항으로 구성되어 있으며, 약 5분 정도 소요됩니다.","icon":"LuCircle"}',
        '[]',
        NOW(),
        NOW()
    ),
    (
        'ISTJ',
        '철저한 원칙으로 세상을 지탱하는 현실주의자',
        '#4A6B8A',
        '{"title":"단단한 반석 위에 선 파수꾼","content":"당신은 흔들리지 않는 원칙과 철저한 책임감으로 주변의 신뢰를 쌓아가는 사람입니다. 말보다 행동으로 증명하고, 작은 약속 하나도 소홀히 하지 않습니다. 오랜 경험에서 비롯된 지혜와 실용적인 판단력으로 복잡한 상황에서도 정확한 해결책을 찾아냅니다. 당신이 있는 곳은 언제나 질서와 안정이 깃들며, 사람들은 당신의 신뢰감 있는 존재에 든든함을 느낍니다.","icon":"LuShield"}',
        '[{"label":"철저한 책임감","description":"맡은 일은 반드시 완수하며, 의무를 소중히 여깁니다.","icon":"LuShield"},{"label":"뛰어난 꼼꼼함","description":"세부사항을 놓치지 않고 정확하게 처리합니다.","icon":"LuClipboardList"},{"label":"흔들리지 않는 신뢰","description":"한번 맺은 약속은 끝까지 지키는 사람입니다.","icon":"LuHandshake"},{"label":"실용적 판단력","description":"경험을 바탕으로 현실적이고 효율적인 해결책을 찾습니다.","icon":"LuTarget"}]',
        NOW(),
        NOW()
    ),
    (
        'ISFJ',
        '언제나 곁에서 돌봐주는 따뜻한 수호자',
        '#6B9E7A',
        '{"title":"조용히 세상을 지탱하는 든든한 버팀목","content":"당신은 말없이 곁에 있어주는 것만으로도 세상을 따뜻하게 만드는 사람입니다. 타인의 필요를 먼저 알아채고, 묵묵히 손을 내밀어 줍니다. 오랜 기억력과 세심한 배려로 소중한 사람들의 작은 것 하나까지 챙기며, 공동체의 화합을 위해 자신을 아낌없이 내어줍니다. 당신 곁에 있는 사람들은 언제나 따뜻한 보호막 안에 있음을 느낍니다.","icon":"LuHeart"}',
        '[{"label":"헌신적인 보살핌","description":"타인을 위해 기꺼이 자신의 시간과 에너지를 내어줍니다.","icon":"LuHeart"},{"label":"뛰어난 기억력","description":"소중한 사람들의 사소한 것까지 기억하고 챙깁니다.","icon":"LuBookMarked"},{"label":"완벽한 배려","description":"상대가 필요한 것을 먼저 알아채고 행동합니다.","icon":"LuGift"},{"label":"묵묵한 성실함","description":"조용하지만 꾸준히 주변을 지탱하는 힘을 가집니다.","icon":"LuAnchor"}]',
        NOW(),
        NOW()
    ),
    (
        'INFJ',
        '세상을 더 나은 곳으로 이끄는 통찰력 있는 비전가',
        '#7B5B9E',
        '{"title":"어둠 속에서 빛을 보는 예언자","content":"당신은 표면 너머의 진실을 꿰뚫어 보는 깊은 통찰력을 가진 사람입니다. 인류의 더 나은 미래를 위한 강한 신념을 품고 있으며, 그 신념을 행동으로 옮기는 용기도 갖추고 있습니다. 조용하지만 강렬한 존재감으로 주변 사람들에게 영감을 주며, 인간의 복잡한 내면을 섬세하게 이해합니다. 당신은 단순히 꿈을 꾸는 것이 아니라, 세상을 변화시키기 위해 행동합니다.","icon":"LuEye"}',
        '[{"label":"깊은 통찰력","description":"상황의 본질과 사람의 내면을 직관적으로 파악합니다.","icon":"LuEye"},{"label":"강한 신념","description":"옳다고 믿는 것을 위해 끝까지 행동합니다.","icon":"LuFlame"},{"label":"섬세한 공감","description":"타인의 감정과 고통을 깊이 이해하고 함께합니다.","icon":"LuHeartHandshake"},{"label":"완벽을 향한 열망","description":"자신과 세상에 대해 높은 기준을 세우고 추구합니다.","icon":"LuStar"}]',
        NOW(),
        NOW()
    ),
    (
        'INTJ',
        '독자적인 전략으로 목표를 완성하는 건축가',
        '#3D4F6A',
        '{"title":"먼 미래를 설계하는 냉철한 설계자","content":"당신은 복잡한 시스템을 꿰뚫는 전략적 사고와 독립적인 정신을 가진 사람입니다. 타인의 시선에 흔들리지 않고 자신만의 비전을 향해 묵묵히 전진합니다. 방대한 지식과 날카로운 분석력으로 다른 이들이 보지 못하는 패턴을 포착하며, 치밀한 계획으로 목표를 현실로 만들어 냅니다. 당신의 의지 앞에서 불가능은 단지 아직 해결되지 않은 문제일 뿐입니다.","icon":"LuBrain"}',
        '[{"label":"전략적 사고","description":"복잡한 상황에서도 최적의 방법을 계획하고 실행합니다.","icon":"LuBrain"},{"label":"강한 독립심","description":"외부의 압력에 흔들리지 않고 자신의 판단을 신뢰합니다.","icon":"LuCompass"},{"label":"높은 기준","description":"자신과 타인에게 높은 수준의 능력과 노력을 요구합니다.","icon":"LuTrendingUp"},{"label":"결단력","description":"분석이 끝나면 과감하게 결정하고 책임집니다.","icon":"LuSword"}]',
        NOW(),
        NOW()
    ),
    (
        'ISTP',
        '효율을 추구하는 냉철한 문제 해결사',
        '#6B7B6B',
        '{"title":"고요한 폭풍 속의 기술자","content":"당신은 복잡한 메커니즘을 직관적으로 이해하고, 손끝으로 세상을 다루는 능력을 가진 사람입니다. 불필요한 말 대신 행동으로 문제를 해결하며, 위기 상황에서도 냉정함을 잃지 않습니다. 얽매임을 싫어하고 자유롭게 자신의 방식으로 세상을 탐험합니다. 당신의 조용한 유능함은 가장 필요한 순간에 빛을 발합니다.","icon":"LuWrench"}',
        '[{"label":"논리적 분석","description":"감정보다 사실과 논리로 상황을 판단합니다.","icon":"LuBinary"},{"label":"뛰어난 손재주","description":"도구와 기계를 직관적으로 다루는 능력을 가집니다.","icon":"LuWrench"},{"label":"위기 대처 능력","description":"압박 상황에서도 침착하게 최선의 해결책을 찾습니다.","icon":"LuShieldCheck"},{"label":"자유로운 독립성","description":"규칙보다 자신의 판단에 따라 유연하게 행동합니다.","icon":"LuWind"}]',
        NOW(),
        NOW()
    ),
    (
        'ISFP',
        '조용히 세상의 아름다움을 느끼는 자유로운 예술가',
        '#9E8060',
        '{"title":"순간 속에 영원을 담는 감성가","content":"당신은 세상의 아름다움을 누구보다 섬세하게 느끼는 사람입니다. 강요받지 않는 자유로운 환경에서 자신만의 방식으로 감동을 표현하며, 조용하지만 깊은 열정으로 삶을 살아갑니다. 타인의 고통에 공감하는 따뜻한 마음과 현재의 순간에 충실한 태도로 주변에 잔잔한 온기를 전합니다. 당신의 존재 자체가 하나의 아름다운 작품입니다.","icon":"LuLeaf"}',
        '[{"label":"섬세한 감수성","description":"세상의 아름다움과 감동을 누구보다 깊이 느낍니다.","icon":"LuLeaf"},{"label":"자유로운 영혼","description":"틀에 얽매이지 않고 자신만의 방식으로 살아갑니다.","icon":"LuBird"},{"label":"따뜻한 친절함","description":"조용하지만 진심을 다해 타인을 배려합니다.","icon":"LuHeart"},{"label":"현재 지향적 삶","description":"지금 이 순간에 집중하며 삶을 충만하게 만듭니다.","icon":"LuSunrise"}]',
        NOW(),
        NOW()
    ),
    (
        'INFP',
        '이상을 향해 조용히 타오르는 열정적인 중재자',
        '#8B7BAE',
        '{"title":"마음속 별을 품은 몽상가","content":"당신은 세상이 더 나아질 수 있다는 믿음을 누구보다 강하게 품고 있는 사람입니다. 겉으로는 조용해 보여도 내면에는 강렬한 가치관과 열정이 타오릅니다. 타인의 아픔에 깊이 공감하며, 자신의 이상과 진정성을 지키기 위해 묵묵히 싸웁니다. 당신의 풍부한 내면 세계는 이 세상에 필요한 공감과 창의성의 원천이 됩니다.","icon":"LuFeather"}',
        '[{"label":"깊은 공감 능력","description":"타인의 감정과 상황을 자신의 것처럼 이해합니다.","icon":"LuHeartHandshake"},{"label":"창의적 상상력","description":"풍부한 내면에서 독창적인 아이디어를 만들어냅니다.","icon":"LuSparkles"},{"label":"강한 가치관","description":"자신이 옳다고 믿는 것을 절대 타협하지 않습니다.","icon":"LuFlame"},{"label":"유연한 열린 사고","description":"다양한 관점을 수용하고 새로운 가능성을 탐구합니다.","icon":"LuRefreshCw"}]',
        NOW(),
        NOW()
    ),
    (
        'INTP',
        '끝없는 지식을 탐구하는 논리적인 사색가',
        '#5B7B9E',
        '{"title":"우주의 이치를 풀어내는 철학자","content":"당신은 세상의 모든 것에 왜라는 질문을 던지며 진리를 탐구하는 사람입니다. 복잡한 이론과 추상적인 개념을 즐겨 다루며, 독창적인 사고로 기존의 틀을 넘어서는 새로운 관점을 제시합니다. 혼자만의 시간 속에서 깊은 사유를 즐기며, 진짜 지식과 정확한 논리에 가장 큰 가치를 둡니다. 당신의 지적 호기심은 세상의 지평을 넓히는 힘이 됩니다.","icon":"LuFlaskConical"}',
        '[{"label":"논리적 사고","description":"감정보다 이성과 분석으로 문제의 본질에 접근합니다.","icon":"LuCalculator"},{"label":"창의적 문제 해결","description":"기존의 틀을 벗어나 독창적인 해결책을 고안합니다.","icon":"LuLightbulb"},{"label":"끝없는 지식 탐구","description":"호기심을 바탕으로 다양한 분야를 깊이 파고듭니다.","icon":"LuFlaskConical"},{"label":"독립적 사고","description":"권위보다 논리와 증거를 바탕으로 판단합니다.","icon":"LuCompass"}]',
        NOW(),
        NOW()
    ),
    (
        'ESTP',
        '현실을 즐기며 순간에 집중하는 대담한 사업가',
        '#C46B3A',
        '{"title":"무대 위에서 빛나는 행동파","content":"당신은 살아있는 현재를 가장 강렬하게 경험하는 사람입니다. 날카로운 관찰력과 빠른 판단력으로 기회를 포착하고, 주저 없이 행동으로 옮깁니다. 어떤 상황에서도 유머와 에너지로 주변을 활기차게 만들며, 실제로 눈에 보이는 결과를 만들어냅니다. 당신의 대담함과 추진력은 타인이 망설이는 곳에서 새로운 길을 열어냅니다.","icon":"LuZap"}',
        '[{"label":"강한 추진력","description":"생각보다 행동이 앞서며 빠르게 결과를 만들어냅니다.","icon":"LuZap"},{"label":"뛰어난 관찰력","description":"주변 상황과 사람들을 날카롭게 파악합니다.","icon":"LuScan"},{"label":"유연한 적응력","description":"어떤 환경에서도 빠르게 적응하고 기회를 찾습니다.","icon":"LuRefreshCw"},{"label":"실용적 해결","description":"이론보다 실제 작동하는 해결책을 선호합니다.","icon":"LuTarget"}]',
        NOW(),
        NOW()
    ),
    (
        'ESFP',
        '삶의 무대를 화려하게 장식하는 자유로운 연예인',
        '#D4903A',
        '{"title":"세상이라는 무대의 영원한 주인공","content":"당신은 삶 자체를 축제로 만드는 타고난 엔터테이너입니다. 넘치는 에너지와 밝은 웃음으로 어디서든 분위기를 바꾸고, 모든 사람이 함께 즐길 수 있는 공간을 만들어냅니다. 현재의 기쁨을 최대한 누리며, 주변 사람들이 지금 이 순간 행복하기를 진심으로 원합니다. 당신의 존재 자체가 세상에 생기와 즐거움을 더합니다.","icon":"LuSparkles"}',
        '[{"label":"넘치는 에너지","description":"언제나 활기차고 긍정적인 에너지로 주변을 밝힙니다.","icon":"LuSparkles"},{"label":"뛰어난 사교성","description":"낯선 사람과도 금방 친해지는 자연스러운 매력을 가집니다.","icon":"LuUsers"},{"label":"즉흥적인 유머","description":"순간의 재치로 모두를 웃게 만드는 능력이 있습니다.","icon":"LuSmile"},{"label":"현재에 충실","description":"지금 이 순간을 가장 풍성하게 즐길 줄 압니다.","icon":"LuSunrise"}]',
        NOW(),
        NOW()
    ),
    (
        'ENFP',
        '가능성을 발견하고 세상을 밝히는 재기발랄한 활동가',
        '#E07B5B',
        '{"title":"반짝이는 아이디어로 세상을 물들이는 탐험가","content":"당신은 세상 어디서나 새로운 가능성을 발견하는 낙관적인 탐험가입니다. 풍부한 상상력과 전염성 있는 열정으로 주변 사람들에게 영감을 주며, 인간의 잠재력을 진심으로 믿고 응원합니다. 규칙보다 사람을, 형식보다 진정성을 추구하며, 자신만의 방식으로 세상과 따뜻하게 연결됩니다. 당신이 있는 곳에는 언제나 새로운 바람이 불어옵니다.","icon":"LuSun"}',
        '[{"label":"풍부한 상상력","description":"어디서든 새로운 아이디어와 가능성을 발견합니다.","icon":"LuSparkles"},{"label":"열정적인 추진력","description":"관심 있는 일에는 폭발적인 에너지를 쏟아냅니다.","icon":"LuFlame"},{"label":"뛰어난 공감력","description":"타인의 감정을 빠르게 파악하고 진심으로 연결됩니다.","icon":"LuHeart"},{"label":"창의적 사고","description":"기존의 틀을 넘는 참신한 시각으로 세상을 바라봅니다.","icon":"LuLightbulb"}]',
        NOW(),
        NOW()
    ),
    (
        'ENTP',
        '아이디어로 세상을 도전하는 혁신적인 변론가',
        '#5B9E8B',
        '{"title":"불꽃 튀는 토론 속의 혁신가","content":"당신은 어떤 개념이든 뒤집어보고 더 나은 방식을 찾아내는 날카로운 혁신가입니다. 지적 자극을 사랑하며, 치열한 토론을 통해 아이디어를 연마하고 새로운 통찰을 얻습니다. 규칙과 관습에 의문을 제기하며, 더 효율적이고 창의적인 해결책을 끊임없이 탐구합니다. 당신의 도전정신은 세상이 더 나아지도록 끊임없이 자극합니다.","icon":"LuLightbulb"}',
        '[{"label":"창의적인 발상","description":"기존 방식에 의문을 제기하고 혁신적인 아이디어를 냅니다.","icon":"LuLightbulb"},{"label":"날카로운 논리","description":"복잡한 개념도 빠르게 파악하고 논리적으로 분석합니다.","icon":"LuBinary"},{"label":"유연한 적응력","description":"변화를 두려워하지 않고 오히려 기회로 삼습니다.","icon":"LuRefreshCw"},{"label":"강한 지적 호기심","description":"다양한 분야에서 새로운 것을 배우고 탐구합니다.","icon":"LuFlaskConical"}]',
        NOW(),
        NOW()
    ),
    (
        'ESTJ',
        '체계와 질서로 공동체를 이끄는 엄격한 관리자',
        '#5B6B8A',
        '{"title":"사회의 뼈대를 세우는 질서의 수호자","content":"당신은 혼란 속에서도 질서를 만들어내는 타고난 조직가입니다. 명확한 기준과 체계적인 계획으로 집단을 효율적으로 이끌며, 책임과 의무를 무엇보다 중요하게 여깁니다. 전통과 규범을 존중하고, 구성원 모두가 제 역할을 다할 수 있도록 환경을 조성합니다. 당신의 리더십 아래에서 조직은 가장 효과적으로 작동합니다.","icon":"LuScale"}',
        '[{"label":"강한 리더십","description":"명확한 방향을 제시하고 구성원을 효율적으로 이끕니다.","icon":"LuCrown"},{"label":"철저한 계획","description":"목표를 향한 세밀한 계획을 수립하고 실행합니다.","icon":"LuClipboardList"},{"label":"강한 책임감","description":"맡은 역할을 끝까지 완수하고 결과에 책임을 집니다.","icon":"LuShield"},{"label":"탁월한 실행력","description":"결정된 사항을 신속하고 정확하게 실행합니다.","icon":"LuZap"}]',
        NOW(),
        NOW()
    ),
    (
        'ESFJ',
        '따뜻한 관계로 공동체를 하나로 묶는 사교적인 외교관',
        '#C47B8B',
        '{"title":"사람들의 마음을 하나로 이어주는 연결자","content":"당신은 주변 사람들이 행복하고 화목하게 지내는 것에서 가장 큰 보람을 느끼는 사람입니다. 뛰어난 사교성과 따뜻한 배려로 누구와도 쉽게 연결되며, 공동체의 분위기를 밝고 화합적으로 만들어갑니다. 타인의 감정에 민감하게 반응하고, 갈등을 중재하며 모두가 소속감을 느낄 수 있도록 노력합니다. 당신이 있는 곳에는 언제나 따뜻한 인간관계의 꽃이 핍니다.","icon":"LuUsers"}',
        '[{"label":"따뜻한 배려심","description":"타인의 필요와 감정을 세심하게 살피고 행동합니다.","icon":"LuHeart"},{"label":"뛰어난 사교성","description":"다양한 사람들과 자연스럽게 어울리며 관계를 맺습니다.","icon":"LuUsers"},{"label":"강한 책임감","description":"공동체를 위한 의무를 성실하게 이행합니다.","icon":"LuShield"},{"label":"화합 추구","description":"갈등을 중재하고 모두가 조화롭게 지낼 수 있도록 돕습니다.","icon":"LuHandshake"}]',
        NOW(),
        NOW()
    ),
    (
        'ENFJ',
        '카리스마로 사람들에게 영감을 주는 선도자',
        '#2E7D52',
        '{"title":"숲을 비추는 따뜻한 등불","content":"당신은 숲의 사자처럼 강인한 리더십과 부드러운 성품을 동시에 지닌 사람입니다. 주변 사람들의 잠재력을 발견하고, 그들이 올바른 길로 나아갈 수 있도록 기꺼이 자신의 등불을 들어 길을 밝혀줍니다. 때로는 자신의 필요보다 타인의 성장을 우선시하며, 진심 어린 소통을 통해 공동체의 화합을 이끌어냅니다. 당신이 걷는 길에는 언제나 따뜻한 온기가 감돌며, 지친 영혼들이 쉬어갈 수 있는 든든한 그늘이 되어줍니다.","icon":"LuSprout"}',
        '[{"label":"깊은 공감 능력","description":"타인의 감정을 섬세하게 읽고 진심으로 위로합니다.","icon":"LuHeartHandshake"},{"label":"카리스마 있는 리더","description":"비전을 제시하고 사람들을 한마음으로 모읍니다.","icon":"LuCrown"},{"label":"직관적인 통찰","description":"사건의 본질과 미래의 가능성을 꿰뚫어 봅니다.","icon":"LuEye"},{"label":"화합의 매개자","description":"갈등을 중재하고 평화로운 환경을 조성합니다.","icon":"LuHandshake"}]',
        NOW(),
        NOW()
    ),
    (
        'ENTJ',
        '강한 의지와 전략으로 세상을 이끄는 대담한 통솔자',
        '#3A4A5A',
        '{"title":"정상을 향해 거침없이 나아가는 총사령관","content":"당신은 목표를 향해 모든 자원을 결집시키는 타고난 지휘관입니다. 어떤 상황에서도 흔들리지 않는 의지와 전략적 사고로 최적의 경로를 개척하며, 주저하는 이들에게 방향을 제시합니다. 무능함과 비효율을 용납하지 않고, 자신과 주변 모두가 최고의 성과를 낼 수 있도록 끊임없이 밀어붙입니다. 당신의 리더십 아래에서 불가능해 보이던 목표들이 현실이 됩니다.","icon":"LuCrown"}',
        '[{"label":"강력한 리더십","description":"자신감 있게 방향을 제시하고 구성원을 이끕니다.","icon":"LuCrown"},{"label":"전략적 사고","description":"장기적 관점에서 최선의 계획을 수립하고 실행합니다.","icon":"LuBrain"},{"label":"목표 지향적","description":"설정한 목표를 향해 흔들리지 않고 전진합니다.","icon":"LuTarget"},{"label":"강한 결단력","description":"복잡한 상황에서도 신속하고 과감하게 결정합니다.","icon":"LuSword"}]',
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO
UPDATE
SET
    title = EXCLUDED.title,
    color_code = EXCLUDED.color_code,
    descript = EXCLUDED.descript,
    personalities = EXCLUDED.personalities,
    updated_at = NOW();

-- ② best_mbti / bad_mbti FK 설정
UPDATE mbti_metadata
SET
    best_mbti = 'ESFP',
    bad_mbti = 'ENFP'
WHERE
    id = 'ISTJ';

UPDATE mbti_metadata
SET
    best_mbti = 'ESTP',
    bad_mbti = 'ENTP'
WHERE
    id = 'ISFJ';

UPDATE mbti_metadata
SET
    best_mbti = 'ENFP',
    bad_mbti = 'ESTP'
WHERE
    id = 'INFJ';

UPDATE mbti_metadata
SET
    best_mbti = 'ENTP',
    bad_mbti = 'ESFP'
WHERE
    id = 'INTJ';

UPDATE mbti_metadata
SET
    best_mbti = 'ESFJ',
    bad_mbti = 'ENFJ'
WHERE
    id = 'ISTP';

UPDATE mbti_metadata
SET
    best_mbti = 'ESFJ',
    bad_mbti = 'ENTJ'
WHERE
    id = 'ISFP';

UPDATE mbti_metadata
SET
    best_mbti = 'ENFJ',
    bad_mbti = 'ESTJ'
WHERE
    id = 'INFP';

UPDATE mbti_metadata
SET
    best_mbti = 'ENTJ',
    bad_mbti = 'ESFJ'
WHERE
    id = 'INTP';

UPDATE mbti_metadata
SET
    best_mbti = 'ISFJ',
    bad_mbti = 'INFJ'
WHERE
    id = 'ESTP';

UPDATE mbti_metadata
SET
    best_mbti = 'ISTJ',
    bad_mbti = 'INTJ'
WHERE
    id = 'ESFP';

UPDATE mbti_metadata
SET
    best_mbti = 'INFJ',
    bad_mbti = 'ISTJ'
WHERE
    id = 'ENFP';

UPDATE mbti_metadata
SET
    best_mbti = 'INTJ',
    bad_mbti = 'ISFJ'
WHERE
    id = 'ENTP';

UPDATE mbti_metadata
SET
    best_mbti = 'ISTP',
    bad_mbti = 'INFP'
WHERE
    id = 'ESTJ';

UPDATE mbti_metadata
SET
    best_mbti = 'ISFP',
    bad_mbti = 'INTP'
WHERE
    id = 'ESFJ';

UPDATE mbti_metadata
SET
    best_mbti = 'INFP',
    bad_mbti = 'ISTP'
WHERE
    id = 'ENFJ';

UPDATE mbti_metadata
SET
    best_mbti = 'INTP',
    bad_mbti = 'ISFP'
WHERE
    id = 'ENTJ';