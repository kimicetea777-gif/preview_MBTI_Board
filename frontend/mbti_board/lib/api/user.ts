import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { MyProfileResponse, UpdateUserRequest } from "@/types/user";

export async function fetchMyProfile(): Promise<MyProfileResponse> {
  const res = await apiClient.get<ApiResponse<MyProfileResponse>>(API.users.me);
  return res.data.data;
}

export async function updateMyProfile(data: UpdateUserRequest): Promise<MyProfileResponse> {
  const res = await apiClient.patch<ApiResponse<MyProfileResponse>>(API.users.me, data);
  return res.data.data;
}

export async function sendWithdrawalOtp(): Promise<void> {
  await apiClient.post<ApiResponse<void>>(API.users.meWithdrawalVerify);
}

export async function withdraw(code: string): Promise<void> {
  await apiClient.delete<ApiResponse<void>>(API.users.meWithdrawal, {
    data: { code },
  });
}
