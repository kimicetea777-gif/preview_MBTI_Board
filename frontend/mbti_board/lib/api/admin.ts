import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdminUserSummaryResponse,
  AdminUserStateRequest,
  AdminUserRoleRequest,
  AdminUserListParams,
  AdminPostSummaryResponse,
  AdminPostVisibilityRequest,
  AdminPostListParams,
  AdminCommentSummaryResponse,
  AdminCommentVisibilityRequest,
  AdminCommentListParams,
} from "@/types/admin";

export async function fetchAdminUsers(
  params: AdminUserListParams
): Promise<PageResponse<AdminUserSummaryResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<AdminUserSummaryResponse>>>(
    API.admin.users, { params }
  );
  return res.data.data;
}

export async function changeUserState(id: string, data: AdminUserStateRequest): Promise<void> {
  await apiClient.patch(API.admin.userState(id), data);
}

export async function changeUserRole(id: string, data: AdminUserRoleRequest): Promise<void> {
  await apiClient.patch(API.admin.userRole(id), data);
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
