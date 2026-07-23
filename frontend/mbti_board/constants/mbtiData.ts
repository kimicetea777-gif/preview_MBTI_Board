// 클라이언트 전용 — IconType(React 컴포넌트 레퍼런스) 포함으로 직렬화 불가
import type { MbtiMetadata } from "@/types/mbti";
import { MBTI_ID } from "@/types/mbti";
import { MBTI_PERSONALITIES } from "@/constants/mbtiPersonalities";
import {
  LuCircle, LuShield, LuHeart, LuEye, LuBrain, LuWrench, LuLeaf,
  LuFeather, LuFlaskConical, LuZap, LuSparkles, LuSun, LuLightbulb,
  LuScale, LuUsers, LuSprout, LuCrown,
} from "react-icons/lu";

type MbtiBase = Omit<MbtiMetadata, "personalities">;

const MBTI_BASE_LIST: MbtiBase[] = [
  // {
  //   id: MBTI_ID.NONE,
  //   title: "MBTI 미설정",
  //   colorCode: "#A8A29E",
  //   imageUrl: null,
  //   icon: LuCircle,
  //   descript: { 
  //     title: "아직 유형이 없어요", 
  //     content: "MBTI 검사를 통해 자신의 성격 유형을 알아보세요. 검사는 총 32문항으로 구성되어 있으며, 약 5분 정도 소요됩니다." },
  //   bestMbti: null,
  //   badMbti: null,
  // },
  {
    id: MBTI_ID.ISTJ,
    title: "철저한 원칙으로 세상을 지탱하는 현실주의자",
    colorCode: "#4A6B8A",
    imageUrl: "/images/mbti/icons/ISTJ.webp",
    imageBackUrl: "/images/mbti/iconsb/ISTJ.webp",
    icon: LuShield,
    descript: { 
      title: "단단한 반석 위에 선 파수꾼", 
      content: "당신은 흔들리지 않는 원칙과 철저한 책임감으로 주변의 신뢰를 쌓아가는 사람입니다. 말보다 행동으로 증명하고, 작은 약속 하나도 소홀히 하지 않습니다. 오랜 경험에서 비롯된 지혜와 실용적인 판단력으로 복잡한 상황에서도 정확한 해결책을 찾아냅니다. 당신이 있는 곳은 언제나 질서와 안정이 깃들며, 사람들은 당신의 신뢰감 있는 존재에 든든함을 느낍니다." },
    bestMbti: MBTI_ID.ESFP,
    badMbti: MBTI_ID.ENFP,
  },
  {
    id: MBTI_ID.ISFJ,
    title: "언제나 곁에서 돌봐주는 따뜻한 수호자",
    colorCode: "#6B9E7A",
    imageUrl: "/images/mbti/icons/ISFJ.webp",
    imageBackUrl: "/images/mbti/iconsb/ISFJ.webp",
    icon: LuHeart,
    descript: { 
      title: "조용히 세상을 지탱하는 든든한 버팀목", 
      content: "당신은 말없이 곁에 있어주는 것만으로도 세상을 따뜻하게 만드는 사람입니다. 타인의 필요를 먼저 알아채고, 묵묵히 손을 내밀어 줍니다. 오랜 기억력과 세심한 배려로 소중한 사람들의 작은 것 하나까지 챙기며, 공동체의 화합을 위해 자신을 아낌없이 내어줍니다. 당신 곁에 있는 사람들은 언제나 따뜻한 보호막 안에 있음을 느낍니다." },
    bestMbti: MBTI_ID.ESTP,
    badMbti: MBTI_ID.ENTP,
  },
  {
    id: MBTI_ID.INFJ,
    title: "세상을 더 나은 곳으로 이끄는 통찰력 있는 비전가",
    colorCode: "#7B5B9E",
    imageUrl: "/images/mbti/icons/INFJ.webp",
    imageBackUrl: "/images/mbti/iconsb/INFJ.webp",
    icon: LuEye,
    descript: { 
      title: "어둠 속에서 빛을 보는 예언자", 
      content: "당신은 표면 너머의 진실을 꿰뚫어 보는 깊은 통찰력을 가진 사람입니다. 인류의 더 나은 미래를 위한 강한 신념을 품고 있으며, 그 신념을 행동으로 옮기는 용기도 갖추고 있습니다. 조용하지만 강렬한 존재감으로 주변 사람들에게 영감을 주며, 인간의 복잡한 내면을 섬세하게 이해합니다. 당신은 단순히 꿈을 꾸는 것이 아니라, 세상을 변화시키기 위해 행동합니다." },
    bestMbti: MBTI_ID.ENFP,
    badMbti: MBTI_ID.ESTP,
  },
  {
    id: MBTI_ID.INTJ,
    title: "독자적인 전략으로 목표를 완성하는 건축가",
    colorCode: "#3D4F6A",
    imageUrl: "/images/mbti/icons/INTJ.webp",
    imageBackUrl: "/images/mbti/iconsb/INTJ.webp",
    icon: LuBrain,
    descript: { 
      title: "먼 미래를 설계하는 냉철한 설계자", 
      content: "당신은 복잡한 시스템을 꿰뚫는 전략적 사고와 독립적인 정신을 가진 사람입니다. 타인의 시선에 흔들리지 않고 자신만의 비전을 향해 묵묵히 전진합니다. 방대한 지식과 날카로운 분석력으로 다른 이들이 보지 못하는 패턴을 포착하며, 치밀한 계획으로 목표를 현실로 만들어 냅니다. 당신의 의지 앞에서 불가능은 단지 아직 해결되지 않은 문제일 뿐입니다." },
    bestMbti: MBTI_ID.ENTP,
    badMbti: MBTI_ID.ESFP,
  },
  {
    id: MBTI_ID.ISTP,
    title: "효율을 추구하는 냉철한 문제 해결사",
    colorCode: "#6B7B6B",
    imageUrl: "/images/mbti/icons/ISTP.webp",
    imageBackUrl: "/images/mbti/iconsb/ISTP.webp",
    icon: LuWrench,
    descript: { 
      title: "고요한 폭풍 속의 기술자", 
      content: "당신은 복잡한 메커니즘을 직관적으로 이해하고, 손끝으로 세상을 다루는 능력을 가진 사람입니다. 불필요한 말 대신 행동으로 문제를 해결하며, 위기 상황에서도 냉정함을 잃지 않습니다. 얽매임을 싫어하고 자유롭게 자신의 방식으로 세상을 탐험합니다. 당신의 조용한 유능함은 가장 필요한 순간에 빛을 발합니다." },
    bestMbti: MBTI_ID.ESFJ,
    badMbti: MBTI_ID.ENFJ,
  },
  {
    id: MBTI_ID.ISFP,
    title: "조용히 세상의 아름다움을 느끼는 자유로운 예술가",
    colorCode: "#9E8060",
    imageUrl: "/images/mbti/icons/ISFP.webp",
    imageBackUrl: "/images/mbti/iconsb/ISFP.webp",
    icon: LuLeaf,
    descript: { 
      title: "순간 속에 영원을 담는 감성가", 
      content: "당신은 세상의 아름다움을 누구보다 섬세하게 느끼는 사람입니다. 강요받지 않는 자유로운 환경에서 자신만의 방식으로 감동을 표현하며, 조용하지만 깊은 열정으로 삶을 살아갑니다. 타인의 고통에 공감하는 따뜻한 마음과 현재의 순간에 충실한 태도로 주변에 잔잔한 온기를 전합니다. 당신의 존재 자체가 하나의 아름다운 작품입니다." },
    bestMbti: MBTI_ID.ESFJ,
    badMbti: MBTI_ID.ENTJ,
  },
  {
    id: MBTI_ID.INFP,
    title: "이상을 향해 조용히 타오르는 열정적인 중재자",
    colorCode: "#8B7BAE",
    imageUrl: "/images/mbti/icons/INFP.webp",
    imageBackUrl: "/images/mbti/iconsb/INFP.webp",
    icon: LuFeather,
    descript: { 
      title: "마음속 별을 품은 몽상가", 
      content: "당신은 세상이 더 나아질 수 있다는 믿음을 누구보다 강하게 품고 있는 사람입니다. 겉으로는 조용해 보여도 내면에는 강렬한 가치관과 열정이 타오릅니다. 타인의 아픔에 깊이 공감하며, 자신의 이상과 진정성을 지키기 위해 묵묵히 싸웁니다. 당신의 풍부한 내면 세계는 이 세상에 필요한 공감과 창의성의 원천이 됩니다." },
    bestMbti: MBTI_ID.ENFJ,
    badMbti: MBTI_ID.ESTJ,
  },
  {
    id: MBTI_ID.INTP,
    title: "끝없는 지식을 탐구하는 논리적인 사색가",
    colorCode: "#5B7B9E",
    imageUrl: "/images/mbti/icons/INTP.webp",
    imageBackUrl: "/images/mbti/iconsb/INTP.webp",
    icon: LuFlaskConical,
    descript: { 
      title: "우주의 이치를 풀어내는 철학자", 
      content: "당신은 세상의 모든 것에 왜라는 질문을 던지며 진리를 탐구하는 사람입니다. 복잡한 이론과 추상적인 개념을 즐겨 다루며, 독창적인 사고로 기존의 틀을 넘어서는 새로운 관점을 제시합니다. 혼자만의 시간 속에서 깊은 사유를 즐기며, 진짜 지식과 정확한 논리에 가장 큰 가치를 둡니다. 당신의 지적 호기심은 세상의 지평을 넓히는 힘이 됩니다." },
    bestMbti: MBTI_ID.ENTJ,
    badMbti: MBTI_ID.ESFJ,
  },
  {
    id: MBTI_ID.ESTP,
    title: "현실을 즐기며 순간에 집중하는 대담한 사업가",
    colorCode: "#C46B3A",
    imageUrl: "/images/mbti/icons/ESTP.webp",
    imageBackUrl: "/images/mbti/iconsb/ESTP.webp",
    icon: LuZap,
    descript: { 
      title: "무대 위에서 빛나는 행동파", 
      content: "당신은 살아있는 현재를 가장 강렬하게 경험하는 사람입니다. 날카로운 관찰력과 빠른 판단력으로 기회를 포착하고, 주저 없이 행동으로 옮깁니다. 어떤 상황에서도 유머와 에너지로 주변을 활기차게 만들며, 실제로 눈에 보이는 결과를 만들어냅니다. 당신의 대담함과 추진력은 타인이 망설이는 곳에서 새로운 길을 열어냅니다." },
    bestMbti: MBTI_ID.ISFJ,
    badMbti: MBTI_ID.INFJ,
  },
  {
    id: MBTI_ID.ESFP,
    title: "삶의 무대를 화려하게 장식하는 자유로운 연예인",
    colorCode: "#D4903A",
    imageUrl: "/images/mbti/icons/ESFP.webp",
    imageBackUrl: "/images/mbti/iconsb/ESFP.webp",
    icon: LuSparkles,
    descript: { 
      title: "세상이라는 무대의 영원한 주인공", 
      content: "당신은 삶 자체를 축제로 만드는 타고난 엔터테이너입니다. 넘치는 에너지와 밝은 웃음으로 어디서든 분위기를 바꾸고, 모든 사람이 함께 즐길 수 있는 공간을 만들어냅니다. 현재의 기쁨을 최대한 누리며, 주변 사람들이 지금 이 순간 행복하기를 진심으로 원합니다. 당신의 존재 자체가 세상에 생기와 즐거움을 더합니다." },
    bestMbti: MBTI_ID.ISTJ,
    badMbti: MBTI_ID.INTJ,
  },
  {
    id: MBTI_ID.ENFP,
    title: "가능성을 발견하고 세상을 밝히는 재기발랄한 활동가",
    colorCode: "#E07B5B",
    imageUrl: "/images/mbti/icons/ENFP.webp",
    imageBackUrl: "/images/mbti/iconsb/ENFP.webp",
    icon: LuSun,
    descript: { 
      title: "반짝이는 아이디어로 세상을 물들이는 탐험가", 
      content: "당신은 세상 어디서나 새로운 가능성을 발견하는 낙관적인 탐험가입니다. 풍부한 상상력과 전염성 있는 열정으로 주변 사람들에게 영감을 주며, 인간의 잠재력을 진심으로 믿고 응원합니다. 규칙보다 사람을, 형식보다 진정성을 추구하며, 자신만의 방식으로 세상과 따뜻하게 연결됩니다. 당신이 있는 곳에는 언제나 새로운 바람이 불어옵니다." },
    bestMbti: MBTI_ID.INFJ,
    badMbti: MBTI_ID.ISTJ,
  },
  {
    id: MBTI_ID.ENTP,
    title: "아이디어로 세상을 도전하는 혁신적인 변론가",
    colorCode: "#5B9E8B",
    imageUrl: "/images/mbti/icons/ENTP.webp",
    imageBackUrl: "/images/mbti/iconsb/ENTP.webp",
    icon: LuLightbulb,
    descript: { 
      title: "불꽃 튀는 토론 속의 혁신가", 
      content: "당신은 어떤 개념이든 뒤집어보고 더 나은 방식을 찾아내는 날카로운 혁신가입니다. 지적 자극을 사랑하며, 치열한 토론을 통해 아이디어를 연마하고 새로운 통찰을 얻습니다. 규칙과 관습에 의문을 제기하며, 더 효율적이고 창의적인 해결책을 끊임없이 탐구합니다. 당신의 도전정신은 세상이 더 나아지도록 끊임없이 자극합니다." },
    bestMbti: MBTI_ID.INTJ,
    badMbti: MBTI_ID.ISFJ,
  },
  {
    id: MBTI_ID.ESTJ,
    title: "체계와 질서로 공동체를 이끄는 엄격한 관리자",
    colorCode: "#5B6B8A",
    imageUrl: "/images/mbti/icons/ESTJ.webp",
    imageBackUrl: "/images/mbti/iconsb/ESTJ.webp",
    icon: LuScale,
    descript: { 
      title: "사회의 뼈대를 세우는 질서의 수호자", 
      content: "당신은 혼란 속에서도 질서를 만들어내는 타고난 조직가입니다. 명확한 기준과 체계적인 계획으로 집단을 효율적으로 이끌며, 책임과 의무를 무엇보다 중요하게 여깁니다. 전통과 규범을 존중하고, 구성원 모두가 제 역할을 다할 수 있도록 환경을 조성합니다. 당신의 리더십 아래에서 조직은 가장 효과적으로 작동합니다." },
    bestMbti: MBTI_ID.ISTP,
    badMbti: MBTI_ID.INFP,
  },
  {
    id: MBTI_ID.ESFJ,
    title: "따뜻한 관계로 공동체를 하나로 묶는 사교적인 외교관",
    colorCode: "#C47B8B",
    imageUrl: "/images/mbti/icons/ESFJ.webp",
    imageBackUrl: "/images/mbti/iconsb/ESFJ.webp",
    icon: LuUsers,
    descript: { 
      title: "사람들의 마음을 하나로 이어주는 연결자", 
      content: "당신은 주변 사람들이 행복하고 화목하게 지내는 것에서 가장 큰 보람을 느끼는 사람입니다. 뛰어난 사교성과 따뜻한 배려로 누구와도 쉽게 연결되며, 공동체의 분위기를 밝고 화합적으로 만들어갑니다. 타인의 감정에 민감하게 반응하고, 갈등을 중재하며 모두가 소속감을 느낄 수 있도록 노력합니다. 당신이 있는 곳에는 언제나 따뜻한 인간관계의 꽃이 핍니다." },
    bestMbti: MBTI_ID.ISFP,
    badMbti: MBTI_ID.INTP,
  },
  {
    id: MBTI_ID.ENFJ,
    title: "카리스마로 사람들에게 영감을 주는 선도자",
    colorCode: "#2E7D52",
    imageUrl: "/images/mbti/icons/ENFJ.webp",
    imageBackUrl: "/images/mbti/iconsb/ENFJ.webp",
    icon: LuSprout,
    descript: { 
      title: "숲을 비추는 따뜻한 등불", 
      content: "당신은 숲의 사자처럼 강인한 리더십과 부드러운 성품을 동시에 지닌 사람입니다. 주변 사람들의 잠재력을 발견하고, 그들이 올바른 길로 나아갈 수 있도록 기꺼이 자신의 등불을 들어 길을 밝혀줍니다. 때로는 자신의 필요보다 타인의 성장을 우선시하며, 진심 어린 소통을 통해 공동체의 화합을 이끌어냅니다. 당신이 걷는 길에는 언제나 따뜻한 온기가 감돌며, 지친 영혼들이 쉬어갈 수 있는 든든한 그늘이 되어줍니다." },
    bestMbti: MBTI_ID.INFP,
    badMbti: MBTI_ID.ISTP,
  },
  {
    id: MBTI_ID.ENTJ,
    title: "강한 의지와 전략으로 세상을 이끄는 대담한 통솔자",
    colorCode: "#3A4A5A",
    imageUrl: "/images/mbti/icons/ENTJ.webp",
    imageBackUrl: "/images/mbti/iconsb/ENTJ.webp",
    icon: LuCrown,
    descript: { 
      title: "정상을 향해 거침없이 나아가는 총사령관", 
      content: "당신은 목표를 향해 모든 자원을 결집시키는 타고난 지휘관입니다. 어떤 상황에서도 흔들리지 않는 의지와 전략적 사고로 최적의 경로를 개척하며, 주저하는 이들에게 방향을 제시합니다. 무능함과 비효율을 용납하지 않고, 자신과 주변 모두가 최고의 성과를 낼 수 있도록 끊임없이 밀어붙입니다. 당신의 리더십 아래에서 불가능해 보이던 목표들이 현실이 됩니다." },
    bestMbti: MBTI_ID.INTP,
    badMbti: MBTI_ID.ISFP,
  },
];

/*------------------------------------------------------------------------------------------ */
export const MBTI_MAP: ReadonlyMap<string, MbtiMetadata> = new Map(
  MBTI_BASE_LIST.map((base) => [
    base.id,
    { ...base, personalities: MBTI_PERSONALITIES[base.id] ?? [] },
  ])
);

const MBTI_LIST: MbtiMetadata[] = Array.from(MBTI_MAP.values());

/*------------------------------------------------------------------------------------------ */

export function getAllMbti(): MbtiMetadata[] {
  return MBTI_LIST;
}

export function getMbtiById(id: string | null | undefined): MbtiMetadata | undefined {
  return !id
    ? undefined
    : MBTI_MAP.get(id.toUpperCase());
}

export function getMbtiColor(id: string): string {
  return MBTI_MAP.get(id.toUpperCase())?.colorCode ?? "#A8A29E";
}

export function existsMbti(id: string): boolean {
  const meta = MBTI_MAP.get(id.toUpperCase());
  return !!meta && (meta.id !== MBTI_ID.NONE);
}
