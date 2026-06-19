export const MBTI_ID = {
  NONE: "NONE",
  UNKNOWN: "UNKNOWN",
  ISTJ: "ISTJ",
  ISFJ: "ISFJ",
  INFJ: "INFJ",
  INTJ: "INTJ",
  ISTP: "ISTP",
  ISFP: "ISFP",
  INFP: "INFP",
  INTP: "INTP",
  ESTP: "ESTP",
  ESFP: "ESFP",
  ENFP: "ENFP",
  ENTP: "ENTP",
  ESTJ: "ESTJ",
  ESFJ: "ESFJ",
  ENFJ: "ENFJ",
  ENTJ: "ENTJ",
} as const;

export type MbtiIdType = typeof MBTI_ID[keyof typeof MBTI_ID];

export const NONE_MBTI_ID = MBTI_ID.NONE;
export const UNKNOWN_MBTI_ID = MBTI_ID.UNKNOWN;
export const MBTI_ALL = "ALL";

export const MBTI_GROUPS = [
  { label: "분석가 (Analysts)", description: "이성적이고 직관적인 탁월한 전략가들",  ids: [MBTI_ID.INTJ, MBTI_ID.INTP, MBTI_ID.ENTJ, MBTI_ID.ENTP] },
  { label: "외교관 (Diplomats)", description: "공감 능력이 뛰어난 열정적인 이상주의자들", ids: [MBTI_ID.INFJ, MBTI_ID.INFP, MBTI_ID.ENFJ, MBTI_ID.ENFP] },
  { label: "관리자 (Sentinels)", description: "실용적이고 신뢰할 수 있는 조직가들",   ids: [MBTI_ID.ISTJ, MBTI_ID.ISFJ, MBTI_ID.ESTJ, MBTI_ID.ESFJ] },
  { label: "탐험가 (Explorers)", description: "자유롭고 유연한 실험적 탐험가들",      ids: [MBTI_ID.ISTP, MBTI_ID.ISFP, MBTI_ID.ESTP, MBTI_ID.ESFP] },
] as const;

export const MBTI_16 = MBTI_GROUPS.flatMap((g) => [...g.ids]);

export const MBTI_PENDING_SCORES_KEY = "mbti_pending_scores";

// icon 필드는 React 컴포넌트 레퍼런스 — 클라이언트 전용 (직렬화 불가)
import type { IconType } from "react-icons";

export interface MbtiDescript {
  title: string;
  content: string;
}

export interface MbtiPersonality {
  label: string;
  description: string;
  icon: IconType;
}

export interface MbtiMetadata {
  id: string;
  title: string;
  colorCode: string;
  imageUrl: string | null;
  imageBackUrl: string | null;
  icon: IconType;
  descript: MbtiDescript;
  personalities: MbtiPersonality[];
  bestMbti: string | null;
  badMbti: string | null;
}

// ── 검사 문항 (Discriminated Union) ─────────────────────────────────────────

interface BaseQuestion {
  id: number;
  title: string;
}

export interface MbtiQuestion extends BaseQuestion {
  type: "MBTI";
  category: "EI" | "SN" | "TF" | "JP";
  isReverse: boolean;
}

export interface AdminMbtiQuestion {
  id: number;
  category: "EI" | "SN" | "TF" | "JP";
  title: string;
  isReverse: boolean;
  isVisible: boolean;
  updatedAt: string;
}

export interface AdminFunOption {
  id: number;
  optionText: string;
  displayOrder: number;
}

export interface AdminFunQuestion {
  id: number;
  title: string;
  options: AdminFunOption[];
  isVisible: boolean;
  updatedAt: string;
}

export interface FunQuestion extends BaseQuestion {
  type: "FUN";
  options: FunQuestionOption[];
}

export interface FunQuestionOption {
  id: number;
  optionText: string;
  displayOrder: number;
}

/** GET /api/mbti/test/questions 응답 타입 */
export type TotalQuestion = MbtiQuestion | FunQuestion;

// ── 검사 점수 ────────────────────────────────────────────────────────────────

export interface MbtiTestScores {
  eiScore: number;
  snScore: number;
  tfScore: number;
  jpScore: number;
}

export interface MbtiTestResult extends MbtiTestScores {
  mbtiId: string;
}

// ── FN 응답 제출·통계 ─────────────────────────────────────────────────────────

export interface FunResponseSubmit {
  mbtiId: string;
  optionIds: number[];
}

export interface FunStatsResponse {
  questionId: number;
  title: string;
  options: FunOptionStats[];
}

export interface FunOptionStats {
  optionId: number;
  optionText: string;
  displayOrder: number;
  totalCount: number;
  mbtiCounts: FunMbtiCount[];
}

export interface FunMbtiCount {
  mbtiId: string;
  count: number;
}
