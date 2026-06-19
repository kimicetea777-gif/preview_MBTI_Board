import type { UserRole, UserState, MbtiVerified } from "./user";
import type { PostType } from "./post";

// GET /api/admin/users 응답
export interface AdminUserSummaryResponse {
  id: string;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  role: UserRole;
  state: UserState;
  mbtiId: string;
  mbtiVerified: MbtiVerified;
  isSocialLogin: boolean;
  hasPhone: boolean;
  lastLoginAt: string | null;
  updatedAt: string;
  createdAt: string;
}

// POST /api/admin/users 요청
export interface AdminCreateUserRequest {
  email: string;
  password: string;
  nickname: string;
  mbtiId: string;
  role: UserRole;
  state: UserState;
}

// PATCH /api/admin/users/{id} 요청 — null 필드는 변경 안 함
export interface AdminUpdateUserRequest {
  nickname?: string;
  clearProfileImage?: boolean;
  role?: UserRole;
  state?: UserState;
}

// GET /api/admin/posts 응답
export interface AdminPostSummaryResponse {
  id: number;
  typeCode: PostType;
  title: string;
  authorNickname: string;
  authorMbtiId: string;
  mbtiVerified: MbtiVerified;
  targetMbtiId: string | null;
  viewCount: number;
  commentCount: number;
  isVisible: boolean;
  createdAt: string;
}

// PATCH /api/admin/posts/{id}/visibility 요청
export interface AdminPostVisibilityRequest {
  visible: boolean;
}

// GET /api/admin/comments 응답
export interface AdminCommentSummaryResponse {
  id: number;
  postId: number;
  postTypeCode: string;
  parentId: number | null;
  content: string;
  authorNickname: string;
  authorMbtiId: string;
  mbtiVerified: MbtiVerified;
  likeCount: number;
  dislikeCount: number;
  isGuest: boolean;
  isVisible: boolean;
  createdAt: string;
}

// PATCH /api/admin/comments/{id}/visibility 요청
export interface AdminCommentVisibilityRequest {
  visible: boolean;
}

// GET /api/admin/users/stats 응답
export interface CountEntry {
  name: string;
  value: number;
}

export interface DailyCountEntry {
  date: string;
  value: number;
}

export interface RoleTrendEntry {
  date: string;
  adminCount: number;
  testerCount: number;
}

export interface AdminUserStatsResponse {
  totalUsers: number;
  inactiveCount: number;
  lastMonthInactiveCount: number;
  socialLoginCount: number;
  localLoginCount: number;
  thisMonthNewUsers: number;
  lastMonthNewUsers: number;
  phoneVerifiedCount: number;
  longPendingCount: number;
  byState: CountEntry[];
  byRole: CountEntry[];
  byMbti: CountEntry[];
  byMbtiVerified: CountEntry[];
  signupTrend: DailyCountEntry[];
  withdrawalTrend: DailyCountEntry[];
  roleTrend: RoleTrendEntry[];
}

export interface AdminPostStatsResponse {
  totalPosts: number;
  hiddenCount: number;
  thisMonthNewPosts: number;
  lastMonthNewPosts: number;
  byType: CountEntry[];
  byMbti: CountEntry[];
  postTrend: DailyCountEntry[];
}

export interface AdminCommentStatsResponse {
  totalComments: number;
  hiddenCount: number;
  thisMonthNewComments: number;
  lastMonthNewComments: number;
  totalLikes: number;
  totalDislikes: number;
  byMbti: CountEntry[];
  commentTrend: DailyCountEntry[];
}

export interface AdminMbtiTestStatsResponse {
  totalTests: number;
  thisMonthTests: number;
  lastMonthTests: number;
  avgEiScore: number;
  avgSnScore: number;
  avgTfScore: number;
  avgJpScore: number;
  byResult: CountEntry[];
  byRetakeCount: CountEntry[];
  testTrend: DailyCountEntry[];
}

export const USER_SORT_FIELD = {
  CREATED_AT: "CREATED_AT",
  UPDATED_AT: "UPDATED_AT",
  LAST_LOGIN_AT: "LAST_LOGIN_AT",
} as const;
export type UserSortField = (typeof USER_SORT_FIELD)[keyof typeof USER_SORT_FIELD];

export const SORT_DIR = {
  ASC: "ASC",
  DESC: "DESC",
} as const;
export type SortDir = (typeof SORT_DIR)[keyof typeof SORT_DIR];

export const USER_SORT_FIELD_LABELS: Record<UserSortField, string> = {
  CREATED_AT: "가입일",
  UPDATED_AT: "수정일",
  LAST_LOGIN_AT: "최근 로그인",
};

// Query params
export interface AdminUserListParams {
  email?: string;
  state?: UserState;
  role?: UserRole;
  sortBy?: UserSortField;
  sortDir?: SortDir;
  page?: number;
  size?: number;
}

export interface AdminPostListParams {
  typeCode?: PostType;
  isVisible?: boolean;
  authorNickname?: string;
  sortDir?: SortDir;
  page?: number;
  size?: number;
}

export interface AdminCommentListParams {
  postId?: number;
  isVisible?: boolean;
  authorNickname?: string;
  sortDir?: SortDir;
  page?: number;
  size?: number;
}

export const USER_STATE_LABELS: Record<UserState, string> = {
  NORMAL: "정상",
  PENDING: "대기",
  INACTIVE: "비활성",
  WITHDRAWN: "탈퇴",
  LOCKED: "잠금",
  SHADOW_BANNED: "쉐도우밴",
  PERMANENT_BAN: "영구정지",
};

export const USER_STATE_COLORS: Record<UserState, string> = {
  NORMAL: "#4A7C59",
  PENDING: "#B07D2A",
  INACTIVE: "#7A7A7A",
  WITHDRAWN: "#5A5A5A",
  LOCKED: "#A05030",
  SHADOW_BANNED: "#6A4A8A",
  PERMANENT_BAN: "#9B2A2A",
};

export const USER_STATE_DESCRIPTIONS: Record<UserState, string> = {
  NORMAL: "정상 상태",
  PENDING: "가입 대기 — 이메일 인증이 필요하거나 가입 절차가 마무리되지 않은 상태",
  INACTIVE: "휴면 상태 — 장기간 접속하지 않아 데이터가 아카이브된 상태",
  WITHDRAWN: "탈퇴 상태 — 사용자가 탈퇴를 요청하여 데이터 삭제 대기 중인 상태",
  LOCKED: "일시 잠금 — 비정상적인 접근 감지 시 본인 확인을 위해 일시적으로 잠긴 상태",
  SHADOW_BANNED: "쉐도우 밴 — 유저는 플레이 가능하지만, 로그 감시",
  PERMANENT_BAN: "영구 정지 — 운영 정책 위반으로 영구적으로 접근이 차단된 상태",
};

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: "어드민",
  TESTER: "테스터",
  USER: "일반",
  GUEST: "게스트",
};

export const USER_ROLE_COLORS: Record<UserRole, string> = {
  ADMIN: "#7A3B1E",
  TESTER: "#2A5A7A",
  USER: "#3A6B4A",
  GUEST: "#6B6B6B",
};
