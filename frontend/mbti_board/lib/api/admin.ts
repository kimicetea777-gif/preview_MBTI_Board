import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { AdminMbtiQuestion, AdminFunQuestion } from "@/types/mbti";
import type {
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
  AdminUserSummaryResponse,
  AdminUserListParams,
  AdminUserStatsResponse,
  AdminPostSummaryResponse,
  AdminPostStatsResponse,
  AdminPostVisibilityRequest,
  AdminPostListParams,
  AdminCommentSummaryResponse,
  AdminCommentStatsResponse,
  AdminCommentVisibilityRequest,
  AdminCommentListParams,
  AdminMbtiTestStatsResponse,
} from "@/types/admin";

export async function fetchAdminUserStats(): Promise<AdminUserStatsResponse> {
  const res = await apiClient.get<ApiResponse<AdminUserStatsResponse>>(API.admin.userStats);
  return res.data.data;
}

export async function createAdminUser(data: AdminCreateUserRequest): Promise<void> {
  await apiClient.post(API.admin.users, data);
}

export async function fetchAdminUsers(
  params: AdminUserListParams
): Promise<PageResponse<AdminUserSummaryResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<AdminUserSummaryResponse>>>(
    API.admin.users, { params }
  );
  return res.data.data;
}

export async function updateAdminUser(id: string, data: AdminUpdateUserRequest): Promise<void> {
  await apiClient.patch(API.admin.user(id), data);
}

export async function fetchAdminPosts(
  params: AdminPostListParams
): Promise<PageResponse<AdminPostSummaryResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<AdminPostSummaryResponse>>>(
    API.admin.posts, { params }
  );
  return res.data.data;
}

export async function changePostVisibility(
  id: number,
  data: AdminPostVisibilityRequest
): Promise<void> {
  await apiClient.patch(API.admin.postVisibility(id), data);
}

export async function deleteAdminPost(id: number): Promise<void> {
  await apiClient.delete(API.admin.post(id));
}

export async function fetchAdminComments(
  params: AdminCommentListParams
): Promise<PageResponse<AdminCommentSummaryResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<AdminCommentSummaryResponse>>>(
    API.admin.comments, { params }
  );
  return res.data.data;
}

export async function changeCommentVisibility(
  id: number,
  data: AdminCommentVisibilityRequest
): Promise<void> {
  await apiClient.patch(API.admin.commentVisibility(id), data);
}

export async function fetchAdminPostStats(): Promise<AdminPostStatsResponse> {
  const res = await apiClient.get<ApiResponse<AdminPostStatsResponse>>(API.admin.postStats);
  return res.data.data;
}

export async function fetchAdminCommentStats(): Promise<AdminCommentStatsResponse> {
  const res = await apiClient.get<ApiResponse<AdminCommentStatsResponse>>(API.admin.commentStats);
  return res.data.data;
}

export async function fetchAdminMbtiTestStats(): Promise<AdminMbtiTestStatsResponse> {
  const res = await apiClient.get<ApiResponse<AdminMbtiTestStatsResponse>>(API.admin.mbtiStats);
  return res.data.data;
}

export async function fetchAdminMbtiQuestions(): Promise<AdminMbtiQuestion[]> {
  const res = await apiClient.get<ApiResponse<AdminMbtiQuestion[]>>(API.admin.mbtiQuestions);
  return res.data.data;
}

export async function updateAdminMbtiQuestion(
  id: number,
  data: { title: string; category: string; isReverse: boolean }
): Promise<void> {
  await apiClient.patch(API.admin.mbtiQuestion(id), data);
}

export async function changeAdminMbtiQuestionVisibility(
  id: number,
  visible: boolean
): Promise<void> {
  await apiClient.patch(API.admin.mbtiQuestionVisibility(id), { visible });
}

export async function fetchAdminFunQuestions(): Promise<AdminFunQuestion[]> {
  const res = await apiClient.get<ApiResponse<AdminFunQuestion[]>>(API.admin.funQuestions);
  return res.data.data;
}

export async function updateAdminFunQuestion(
  id: number,
  data: { title: string; options: { id: number; optionText: string }[] }
): Promise<void> {
  await apiClient.patch(API.admin.funQuestion(id), data);
}

export async function changeAdminFunQuestionVisibility(
  id: number,
  visible: boolean
): Promise<void> {
  await apiClient.patch(API.admin.funQuestionVisibility(id), { visible });
}
