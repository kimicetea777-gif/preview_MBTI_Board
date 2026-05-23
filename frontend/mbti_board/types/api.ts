export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  errorCode: string | null;
}

export interface PageResponse<T> {
  content: T[];
  number: number;    // Spring Page의 현재 페이지 번호 (0-indexed)
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
