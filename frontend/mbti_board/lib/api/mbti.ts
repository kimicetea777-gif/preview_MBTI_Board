import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { TotalQuestion, MbtiTestResult, MbtiTestScores, FunResponseSubmit, FunStatsResponse } from "@/types/mbti";

export async function fetchMbtiTestQuestions(): Promise<TotalQuestion[]> {
  const res = await apiClient.get<ApiResponse<TotalQuestion[]>>(API.mbti.testQuestions);
  return res.data.data;
}

export async function submitMbtiTest(scores: MbtiTestScores): Promise<MbtiTestResult> {
  const res = await apiClient.post<ApiResponse<MbtiTestResult>>(API.mbti.testSubmit, scores);
  return res.data.data;
}

export async function submitFunResponses(data: FunResponseSubmit): Promise<void> {
  await apiClient.post(API.mbti.funResponses, data);
}

export async function fetchFunStats(): Promise<FunStatsResponse[]> {
  const res = await apiClient.get<ApiResponse<FunStatsResponse[]>>(API.mbti.funStats);
  return res.data.data;
}
