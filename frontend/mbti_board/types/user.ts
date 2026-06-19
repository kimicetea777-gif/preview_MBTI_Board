export type MbtiVerified = "TESTED" | "SELF" | "DEFAULT";

export const MBTI_VERIFIED_META: Record<MbtiVerified, { color: string; label: string }> = {
  TESTED:  { color: "#2E5C40", label: "검사 인증" },
  SELF:    { color: "#7DB27A", label: "자기 입력" },
  DEFAULT: { color: "#A8A29E", label: "미인증"   },
};

export const USER_ROLE = {
  ADMIN: "ADMIN",
  TESTER: "TESTER",
  USER: "USER",
  GUEST: "GUEST",
} as const;

export type UserRole = "ADMIN" | "TESTER" | "USER" | "GUEST";

export type UserState =
  | "NORMAL"
  | "PENDING"
  | "INACTIVE"
  | "WITHDRAWN"
  | "LOCKED"
  | "SHADOW_BANNED"
  | "PERMANENT_BAN";

export interface User {
  id: string;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  mbtiId: string;
  mbtiVerified: MbtiVerified;
  role: UserRole;
  state: UserState;
  createdAt: string;
}

export interface UpdateUserRequest {
  nickname?: string;
  profileImageUrl?: string | null;
  mbtiId?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface MyProfileResponse {
  id: string;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  mbtiId: string;
  mbtiVerified: MbtiVerified;
  role: UserRole;
  state: UserState;
  isSocialOnly: boolean;
  hasPhone: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface UserActivityStats {
  postCount: number;
  commentCount: number;
  latestTestMbtiId: string | null;
  latestTestDate: string | null;
}

export interface MbtiTestHistory {
  id: number;
  mbtiId: string;
  eiScore: number;
  snScore: number;
  tfScore: number;
  jpScore: number;
  testVersion: string;
  createdAt: string;
}
