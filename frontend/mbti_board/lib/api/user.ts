import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { MyProfileResponse, UpdateUserRequest, UserActivityStats, MbtiTestHistory, NicknameCheckResponse } from "@/types/user";
import type { MyPostPreview } from "@/types/post";

export async function checkNickname(nickname: string): Promise<NicknameCheckResponse> {
  const res = await apiClient.get<ApiResponse<NicknameCheckResponse>>(API.users.checkNickname, {
    params: { nickname },
  });
  return res.data.data;
}

export async function fetchMyProfile(): Promise<MyProfileResponse> {
  const res = await apiClient.get<ApiResponse<MyProfileResponse>>(API.users.me);
  return res.data.data;
}

export async function updateMyProfile(data: UpdateUserRequest): Promise<MyProfileResponse> {
  const res = await apiClient.patch<ApiResponse<MyProfileResponse>>(API.users.me, data);
  return res.data.data;
}

export async function fetchMyActivityStats(): Promise<UserActivityStats> {
  const res = await apiClient.get<ApiResponse<UserActivityStats>>(API.users.meStats);
  return res.data.data;
}

export async function fetchMyPostsPreview(): Promise<MyPostPreview[]> {
  const res = await apiClient.get<ApiResponse<MyPostPreview[]>>(API.users.mePostsPreview);
  return res.data.data;
}

export async function fetchMyMbtiHistory(): Promise<MbtiTestHistory[]> {
  const res = await apiClient.get<ApiResponse<MbtiTestHistory[]>>(API.users.mbtiHistory);
  return res.data.data;
}
