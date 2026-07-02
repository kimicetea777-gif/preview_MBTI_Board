// GET /api/notices/active 응답
export interface NoticeResponse {
  id: number;
  title: string;
  content: string;
  createdAt: string;
}

// GET /api/admin/notices 응답
export interface AdminNoticeResponse {
  id: number;
  title: string;
  content: string;
  isActive: boolean;
  priority: number;
  startsAt: string | null;
  endsAt: string | null;
  createdAt: string;
  updatedAt: string;
}

// POST /api/admin/notices 요청
export interface NoticeCreateRequest {
  title: string;
  content: string;
  isActive: boolean;
  priority: number;
  startsAt: string | null;
  endsAt: string | null;
}

// PATCH /api/admin/notices/{id} 요청
export type NoticeUpdateRequest = NoticeCreateRequest;

export const NOTICE_HIDDEN_KEY = "notice_hidden_until";
