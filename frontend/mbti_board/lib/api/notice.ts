import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type { NoticeResponse, AdminNoticeResponse, NoticeCreateRequest, NoticeUpdateRequest } from "@/types/notice";

export async function fetchActiveNotices(): Promise<NoticeResponse[]> {
  const res = await apiClient.get<ApiResponse<NoticeResponse[]>>(API.notices.active);
  return res.data.data;
}

export async function fetchAdminNotices(): Promise<AdminNoticeResponse[]> {
  const res = await apiClient.get<ApiResponse<AdminNoticeResponse[]>>(API.admin.notices);
  return res.data.data;
}

export async function createAdminNotice(data: NoticeCreateRequest): Promise<AdminNoticeResponse> {
  const res = await apiClient.post<ApiResponse<AdminNoticeResponse>>(API.admin.notices, data);
  return res.data.data;
}

export async function updateAdminNotice(id: number, data: NoticeUpdateRequest): Promise<AdminNoticeResponse> {
  const res = await apiClient.patch<ApiResponse<AdminNoticeResponse>>(API.admin.notice(id), data);
  return res.data.data;
}

export async function deleteAdminNotice(id: number): Promise<void> {
  await apiClient.delete(API.admin.notice(id));
}
