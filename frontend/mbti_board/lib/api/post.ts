import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  PostSummaryResponse,
  PostDetailResponse,
  PostListParams,
  PostCreateRequest,
  PostUpdateRequest,
} from "@/types/post";

export async function fetchPosts(
  params: PostListParams
): Promise<PageResponse<PostSummaryResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    API.posts.list,
    { params }
  );
  return res.data.data;
}

export async function fetchPostById(id: number): Promise<PostDetailResponse> {
  const res = await apiClient.get<ApiResponse<PostDetailResponse>>(API.posts.byId(id));
  return res.data.data;
}

export async function createPost(data: PostCreateRequest): Promise<PostDetailResponse> {
  const res = await apiClient.post<ApiResponse<PostDetailResponse>>(API.posts.list, data);
  return res.data.data;
}

export async function updatePost(
  id: number,
  data: PostUpdateRequest
): Promise<PostDetailResponse> {
  const res = await apiClient.patch<ApiResponse<PostDetailResponse>>(API.posts.byId(id), data);
  return res.data.data;
}

export async function deletePost(id: number, guestPassword?: string): Promise<void> {
  await apiClient.delete(API.posts.byId(id), {
    headers: guestPassword ? { "X-Guest-Password": guestPassword } : {},
  });
}
