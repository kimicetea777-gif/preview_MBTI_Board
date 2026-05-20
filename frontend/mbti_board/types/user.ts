export type MbtiVerified = "TESTED" | "SELF" | "DEFAULT";

export const MBTI_VERIFIED_COLOR: Record<MbtiVerified, string> = {
  TESTED: "#2E5C40",
  SELF: "#7DB27A",
  DEFAULT: "#A8A29E",
};

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

export interface MyProfileResponse {
  id: string;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  mbtiId: string;
  mbtiColorCode: string;
  mbtiVerified: MbtiVerified;
  mbtiVerifiedColorCode: string;
  role: UserRole;
  state: UserState;
  isSocialOnly: boolean;
  createdAt: string;
}
