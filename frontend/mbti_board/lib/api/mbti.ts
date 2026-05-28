import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { MbtiMetadata, MbtiQuestion, MbtiTestResult, MbtiTestScores } from "@/types/mbti";

export async function fetchMbtiList(): Promise<MbtiMetadata[]> {
  const res = await apiClient.get<ApiResponse<MbtiMetadata[]>>(API.mbti.list);
  return res.data.data;
}

export async function fetchMbtiById(id: string): Promise<MbtiMetadata> {
  const res = await apiClient.get<ApiResponse<MbtiMetadata>>(API.mbti.byId(id));
  return res.data.data;
}

export async function fetchMbtiTestQuestions(): Promise<MbtiQuestion[]> {
  const res = await apiClient.get<ApiResponse<MbtiQuestion[]>>(API.mbti.testQuestions);
  return res.data.data;
}

export async function submitMbtiTest(scores: MbtiTestScores): Promise<MbtiTestResult> {
  const res = await apiClient.post<ApiResponse<MbtiTestResult>>(API.mbti.testSubmit, scores);
  return res.data.data;
}
