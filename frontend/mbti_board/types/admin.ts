import type { UserRole, UserState, MbtiVerified } from "./user";
import type { PostType } from "./post";

// GET /api/admin/users 응답
export interface AdminUserSummaryResponse {
  id: string;
  email: string;
  nickname: string;
  role: UserRole;
  state: UserState;
  mbtiId: string;
  lastLoginAt: string | null;
  createdAt: string;
}

// PATCH /api/admin/users/{id}/state 요청
export interface AdminUserStateRequest {
  state: UserState;
}

// PATCH /api/admin/users/{id}/role 요청
export interface AdminUserRoleRequest {
  role: UserRole;
}

// GET /api/admin/posts 응답
export interface AdminPostSummaryResponse {
  id: number;
  typeCode: PostType;
  title: string;
  authorNickname: string;
  authorMbtiId: string;
  authorMbtiColor: string;
  mbtiVerified: MbtiVerified;
  mbtiVerifiedColor: string;
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
  parentId: number | null;
  content: string;
  authorNickname: string;
  authorMbtiId: string;
  authorMbtiColor: string;
  mbtiVerified: MbtiVerified;
  mbtiVerifiedColor: string;
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

// Query params
export interface AdminUserListParams {
  email?: string;
  state?: UserState;
  role?: UserRole;
  page?: number;
  size?: number;
}

export interface AdminPostListParams {
  typeCode?: PostType;
  isVisible?: boolean;
  page?: number;
  size?: number;
}

export interface AdminCommentListParams {
  postId?: number;
  isVisible?: boolean;
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

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: "어드민",
  TESTER: "테스터",
  USER: "일반",
  GUEST: "게스트",
};
