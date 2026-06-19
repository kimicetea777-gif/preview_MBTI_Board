// 클라이언트 전용 — IconType(React 컴포넌트 레퍼런스) 포함으로 직렬화 불가
import type { MbtiPersonality } from "@/types/mbti";
import { MBTI_ID } from "@/types/mbti";
import {
  LuShield, LuClipboardList, LuHandshake, LuTarget,
  LuHeart, LuBookMarked, LuGift, LuAnchor,
  LuEye, LuFlame, LuHeartHandshake, LuStar,
  LuBrain, LuCompass, LuTrendingUp, LuSword,
  LuBinary, LuWrench, LuShieldCheck, LuWind,
  LuLeaf, LuBird, LuSunrise,
  LuSparkles, LuRefreshCw,
  LuCalculator, LuLightbulb, LuFlaskConical,
  LuZap, LuScan, LuUsers, LuSmile, LuCrown,
} from "react-icons/lu";

export const MBTI_PERSONALITIES: Record<string, MbtiPersonality[]> = {
  [MBTI_ID.NONE]: [],

  [MBTI_ID.ISTJ]: [
    { label: "철저한 책임감", description: "맡은 일은 반드시 완수하며, 의무를 소중히 여깁니다.", icon: LuShield },
    { label: "뛰어난 꼼꼼함", description: "세부사항을 놓치지 않고 정확하게 처리합니다.", icon: LuClipboardList },
    { label: "흔들리지 않는 신뢰", description: "한번 맺은 약속은 끝까지 지키는 사람입니다.", icon: LuHandshake },
    { label: "실용적 판단력", description: "경험을 바탕으로 현실적이고 효율적인 해결책을 찾습니다.", icon: LuTarget },
  ],

  [MBTI_ID.ISFJ]: [
    { label: "헌신적인 보살핌", description: "타인을 위해 기꺼이 자신의 시간과 에너지를 내어줍니다.", icon: LuHeart },
    { label: "뛰어난 기억력", description: "소중한 사람들의 사소한 것까지 기억하고 챙깁니다.", icon: LuBookMarked },
    { label: "완벽한 배려", description: "상대가 필요한 것을 먼저 알아채고 행동합니다.", icon: LuGift },
    { label: "묵묵한 성실함", description: "조용하지만 꾸준히 주변을 지탱하는 힘을 가집니다.", icon: LuAnchor },
  ],

  [MBTI_ID.INFJ]: [
    { label: "깊은 통찰력", description: "상황의 본질과 사람의 내면을 직관적으로 파악합니다.", icon: LuEye },
    { label: "강한 신념", description: "옳다고 믿는 것을 위해 끝까지 행동합니다.", icon: LuFlame },
    { label: "섬세한 공감", description: "타인의 감정과 고통을 깊이 이해하고 함께합니다.", icon: LuHeartHandshake },
    { label: "완벽을 향한 열망", description: "자신과 세상에 대해 높은 기준을 세우고 추구합니다.", icon: LuStar },
  ],

  [MBTI_ID.INTJ]: [
    { label: "전략적 사고", description: "복잡한 상황에서도 최적의 방법을 계획하고 실행합니다.", icon: LuBrain },
    { label: "강한 독립심", description: "외부의 압력에 흔들리지 않고 자신의 판단을 신뢰합니다.", icon: LuCompass },
    { label: "높은 기준", description: "자신과 타인에게 높은 수준의 능력과 노력을 요구합니다.", icon: LuTrendingUp },
    { label: "결단력", description: "분석이 끝나면 과감하게 결정하고 책임집니다.", icon: LuSword },
  ],

  [MBTI_ID.ISTP]: [
    { label: "논리적 분석", description: "감정보다 사실과 논리로 상황을 판단합니다.", icon: LuBinary },
    { label: "뛰어난 손재주", description: "도구와 기계를 직관적으로 다루는 능력을 가집니다.", icon: LuWrench },
    { label: "위기 대처 능력", description: "압박 상황에서도 침착하게 최선의 해결책을 찾습니다.", icon: LuShieldCheck },
    { label: "자유로운 독립성", description: "규칙보다 자신의 판단에 따라 유연하게 행동합니다.", icon: LuWind },
  ],

  [MBTI_ID.ISFP]: [
    { label: "섬세한 감수성", description: "세상의 아름다움과 감동을 누구보다 깊이 느낍니다.", icon: LuLeaf },
    { label: "자유로운 영혼", description: "틀에 얽매이지 않고 자신만의 방식으로 살아갑니다.", icon: LuBird },
    { label: "따뜻한 친절함", description: "조용하지만 진심을 다해 타인을 배려합니다.", icon: LuHeart },
    { label: "현재 지향적 삶", description: "지금 이 순간에 집중하며 삶을 충만하게 만듭니다.", icon: LuSunrise },
  ],

  [MBTI_ID.INFP]: [
    { label: "깊은 공감 능력", description: "타인의 감정과 상황을 자신의 것처럼 이해합니다.", icon: LuHeartHandshake },
    { label: "창의적 상상력", description: "풍부한 내면에서 독창적인 아이디어를 만들어냅니다.", icon: LuSparkles },
    { label: "강한 가치관", description: "자신이 옳다고 믿는 것을 절대 타협하지 않습니다.", icon: LuFlame },
    { label: "유연한 열린 사고", description: "다양한 관점을 수용하고 새로운 가능성을 탐구합니다.", icon: LuRefreshCw },
  ],

  [MBTI_ID.INTP]: [
    { label: "논리적 사고", description: "감정보다 이성과 분석으로 문제의 본질에 접근합니다.", icon: LuCalculator },
    { label: "창의적 문제 해결", description: "기존의 틀을 벗어나 독창적인 해결책을 고안합니다.", icon: LuLightbulb },
    { label: "끝없는 지식 탐구", description: "호기심을 바탕으로 다양한 분야를 깊이 파고듭니다.", icon: LuFlaskConical },
    { label: "독립적 사고", description: "권위보다 논리와 증거를 바탕으로 판단합니다.", icon: LuCompass },
  ],

  [MBTI_ID.ESTP]: [
    { label: "강한 추진력", description: "생각보다 행동이 앞서며 빠르게 결과를 만들어냅니다.", icon: LuZap },
    { label: "뛰어난 관찰력", description: "주변 상황과 사람들을 날카롭게 파악합니다.", icon: LuScan },
    { label: "유연한 적응력", description: "어떤 환경에서도 빠르게 적응하고 기회를 찾습니다.", icon: LuRefreshCw },
    { label: "실용적 해결", description: "이론보다 실제 작동하는 해결책을 선호합니다.", icon: LuTarget },
  ],

  [MBTI_ID.ESFP]: [
    { label: "넘치는 에너지", description: "언제나 활기차고 긍정적인 에너지로 주변을 밝힙니다.", icon: LuSparkles },
    { label: "뛰어난 사교성", description: "낯선 사람과도 금방 친해지는 자연스러운 매력을 가집니다.", icon: LuUsers },
    { label: "즉흥적인 유머", description: "순간의 재치로 모두를 웃게 만드는 능력이 있습니다.", icon: LuSmile },
    { label: "현재에 충실", description: "지금 이 순간을 가장 풍성하게 즐길 줄 압니다.", icon: LuSunrise },
  ],

  [MBTI_ID.ENFP]: [
    { label: "풍부한 상상력", description: "어디서든 새로운 아이디어와 가능성을 발견합니다.", icon: LuSparkles },
    { label: "열정적인 추진력", description: "관심 있는 일에는 폭발적인 에너지를 쏟아냅니다.", icon: LuFlame },
    { label: "뛰어난 공감력", description: "타인의 감정을 빠르게 파악하고 진심으로 연결됩니다.", icon: LuHeart },
    { label: "창의적 사고", description: "기존의 틀을 넘는 참신한 시각으로 세상을 바라봅니다.", icon: LuLightbulb },
  ],

  [MBTI_ID.ENTP]: [
    { label: "창의적인 발상", description: "기존 방식에 의문을 제기하고 혁신적인 아이디어를 냅니다.", icon: LuLightbulb },
    { label: "날카로운 논리", description: "복잡한 개념도 빠르게 파악하고 논리적으로 분석합니다.", icon: LuBinary },
    { label: "유연한 적응력", description: "변화를 두려워하지 않고 오히려 기회로 삼습니다.", icon: LuRefreshCw },
    { label: "강한 지적 호기심", description: "다양한 분야에서 새로운 것을 배우고 탐구합니다.", icon: LuFlaskConical },
  ],

  [MBTI_ID.ESTJ]: [
    { label: "강한 리더십", description: "명확한 방향을 제시하고 구성원을 효율적으로 이끕니다.", icon: LuCrown },
    { label: "철저한 계획", description: "목표를 향한 세밀한 계획을 수립하고 실행합니다.", icon: LuClipboardList },
    { label: "강한 책임감", description: "맡은 역할을 끝까지 완수하고 결과에 책임을 집니다.", icon: LuShield },
    { label: "탁월한 실행력", description: "결정된 사항을 신속하고 정확하게 실행합니다.", icon: LuZap },
  ],

  [MBTI_ID.ESFJ]: [
    { label: "따뜻한 배려심", description: "타인의 필요와 감정을 세심하게 살피고 행동합니다.", icon: LuHeart },
    { label: "뛰어난 사교성", description: "다양한 사람들과 자연스럽게 어울리며 관계를 맺습니다.", icon: LuUsers },
    { label: "강한 책임감", description: "공동체를 위한 의무를 성실하게 이행합니다.", icon: LuShield },
    { label: "화합 추구", description: "갈등을 중재하고 모두가 조화롭게 지낼 수 있도록 돕습니다.", icon: LuHandshake },
  ],

  [MBTI_ID.ENFJ]: [
    { label: "깊은 공감 능력", description: "타인의 감정을 섬세하게 읽고 진심으로 위로합니다.", icon: LuHeartHandshake },
    { label: "카리스마 있는 리더", description: "비전을 제시하고 사람들을 한마음으로 모읍니다.", icon: LuCrown },
    { label: "직관적인 통찰", description: "사건의 본질과 미래의 가능성을 꿰뚫어 봅니다.", icon: LuEye },
    { label: "화합의 매개자", description: "갈등을 중재하고 평화로운 환경을 조성합니다.", icon: LuHandshake },
  ],

  [MBTI_ID.ENTJ]: [
    { label: "강력한 리더십", description: "자신감 있게 방향을 제시하고 구성원을 이끕니다.", icon: LuCrown },
    { label: "전략적 사고", description: "장기적 관점에서 최선의 계획을 수립하고 실행합니다.", icon: LuBrain },
    { label: "목표 지향적", description: "설정한 목표를 향해 흔들리지 않고 전진합니다.", icon: LuTarget },
    { label: "강한 결단력", description: "복잡한 상황에서도 신속하고 과감하게 결정합니다.", icon: LuSword },
  ],
};
