import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { VoteResult } from "@/types/post";

export async function castVote(postId: number, optionId: number): Promise<VoteResult> {
  const res = await apiClient.post<ApiResponse<VoteResult>>(API.votes.cast(postId), { optionId });
  return res.data.data;
}

export async function fetchVoteResult(postId: number): Promise<VoteResult> {
  const res = await apiClient.get<ApiResponse<VoteResult>>(API.votes.result(postId));
  return res.data.data;
}
