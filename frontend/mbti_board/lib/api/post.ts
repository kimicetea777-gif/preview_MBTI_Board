import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  PostSummaryResponse,
  PostDetailResponse,
  PostListParams,
  MyPostListParams,
  PostCreateRequest,
  VotePostCreateRequest,
  DebatePostCreateRequest,
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

export async function createVotePost(data: VotePostCreateRequest): Promise<PostDetailResponse> {
  const res = await apiClient.post<ApiResponse<PostDetailResponse>>(API.posts.voteCreate, data);
  return res.data.data;
}

export async function createDebatePost(data: DebatePostCreateRequest): Promise<PostDetailResponse> {
  const res = await apiClient.post<ApiResponse<PostDetailResponse>>(API.posts.debateCreate, data);
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

export async function fetchMyPosts(
  params: MyPostListParams
): Promise<PageResponse<PostSummaryResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    API.users.mePosts,
    { params }
  );
  return res.data.data;
}
