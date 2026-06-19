import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { MyProfileResponse, UpdateUserRequest, UserActivityStats, ChangePasswordRequest, MbtiTestHistory } from "@/types/user";
import type { MyPostPreview } from "@/types/post";

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

export async function fetchMyActivityStats(): Promise<UserActivityStats> {
  const res = await apiClient.get<ApiResponse<UserActivityStats>>(API.users.meStats);
  return res.data.data;
}

export async function fetchMyPostsPreview(): Promise<MyPostPreview[]> {
  const res = await apiClient.get<ApiResponse<MyPostPreview[]>>(API.users.mePostsPreview);
  return res.data.data;
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await apiClient.patch<ApiResponse<void>>(API.users.mePassword, data);
}

export async function updatePhone(phone: string): Promise<void> {
  await apiClient.patch<ApiResponse<void>>(API.users.mePhone, { phone });
}

export async function removePhone(): Promise<void> {
  await apiClient.delete<ApiResponse<void>>(API.users.mePhone);
}

export async function fetchMyMbtiHistory(): Promise<MbtiTestHistory[]> {
  const res = await apiClient.get<ApiResponse<MbtiTestHistory[]>>(API.users.mbtiHistory);
  return res.data.data;
}
