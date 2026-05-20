import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { MbtiMetadata } from "@/types/mbti";

export async function fetchMbtiList(): Promise<MbtiMetadata[]> {
  const res = await apiClient.get<ApiResponse<MbtiMetadata[]>>(API.mbti.list);
  return res.data.data;
}
