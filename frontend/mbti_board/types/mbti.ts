export const NONE_MBTI_ID = "NONE";

export const MBTI_GROUPS = [
  { label: "분석가 (Analysts)", ids: ["INTJ", "ENTJ", "INTP", "ENTP"] },
  { label: "외교관 (Diplomats)", ids: ["INFJ", "ENFJ", "INFP", "ENFP"] },
  { label: "관리자 (Sentinels)", ids: ["ISTJ", "ESTJ", "ISFJ", "ESFJ"] },
  { label: "탐험가 (Explorers)", ids: ["ISTP", "ESTP", "ISFP", "ESFP"] },
] as const;

export const MBTI_ALL = "ALL";

export const MBTI_PENDING_SCORES_KEY = "mbti_pending_scores";

export interface MbtiDescription {
  overview: string;
  traits: string[];
  strengths: string[];
  weaknesses: string[];
  compatible: string[];
}

export interface MbtiMetadata {
  id: string;
  iconUrl: string | null;
  colorCode: string;
  summary: string;
  description: MbtiDescription;
}

export interface MbtiQuestion {
  id: number;
  category: string;
  title: string;
  isReverse: boolean;
}

export interface MbtiTestScores {
  eiScore: number;
  snScore: number;
  tfScore: number;
  jpScore: number;
}

export interface MbtiTestResult extends MbtiTestScores {
  mbtiId: string;
}
