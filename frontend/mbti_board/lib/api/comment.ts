import { apiClient } from "../apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { Comment, CreateCommentRequest, UpdateCommentRequest, ReactionRequest, ReactionResponse } from "@/types/comment";

export async function fetchComments(postId: number): Promise<Comment[]> {
  const res = await apiClient.get<ApiResponse<Comment[]>>(API.comments.byPostId(postId));
  return res.data.data;
}

export async function createComment(postId: number, req: CreateCommentRequest): Promise<Comment> {
  const res = await apiClient.post<ApiResponse<Comment>>(API.comments.byPostId(postId), req);
  return res.data.data;
}

export async function updateComment(postId: number, commentId: number, req: UpdateCommentRequest): Promise<Comment> {
  const res = await apiClient.patch<ApiResponse<Comment>>(API.comments.byId(postId, commentId), req);
  return res.data.data;
}

export async function deleteComment(postId: number, commentId: number, guestPassword?: string): Promise<void> {
  await apiClient.delete(API.comments.byId(postId, commentId), {
    headers: guestPassword ? { "X-Guest-Password": guestPassword } : undefined,
  });
}

export async function toggleReaction(commentId: number, req: ReactionRequest): Promise<ReactionResponse> {
  const res = await apiClient.post<ApiResponse<ReactionResponse>>(API.comments.reactions(commentId), req);
  return res.data.data;
}
