import { apiClient } from "../apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { Comment, CreateCommentRequest, ReactionRequest, ReactionResponse } from "@/types/comment";

export async function fetchComments(postId: number): Promise<Comment[]> {
  const res = await apiClient.get<ApiResponse<Comment[]>>(API.comments.byPostId(postId));
  return res.data.data;
}

export async function createComment(postId: number, req: CreateCommentRequest): Promise<Comment> {
  const res = await apiClient.post<ApiResponse<Comment>>(API.comments.byPostId(postId), req);
  return res.data.data;
}

export async function toggleReaction(commentId: number, req: ReactionRequest): Promise<ReactionResponse> {
  const res = await apiClient.post<ApiResponse<ReactionResponse>>(API.comments.reactions(commentId), req);
  return res.data.data;
}
