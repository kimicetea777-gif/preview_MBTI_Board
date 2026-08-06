import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse } from "@/types/api";

export interface LogoutResponse {
  authServerLogoutUrl: string | null;
}

export async function logout(): Promise<LogoutResponse> {
  const { data } = await apiClient.post<ApiResponse<LogoutResponse>>(API.auth.logout);
  return data.data;
}

export async function logoutAll(): Promise<LogoutResponse> {
  const { data } = await apiClient.post<ApiResponse<LogoutResponse>>(API.auth.logoutAll);
  return data.data;
}
