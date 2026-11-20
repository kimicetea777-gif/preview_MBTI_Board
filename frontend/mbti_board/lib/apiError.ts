import { AxiosError, isAxiosError } from "axios";
import { API_TIMEOUT_MESSAGE } from "@/constants/timeouts";

/**
 * 에러 화면(ApiErrorBoundary)에 보여 줄 문구.
 * axios의 error.message("Request failed with status code 500", "Network Error")나 렌더링 버그의 JS 메시지는
 * 사용자에게 의미가 없으므로 그대로 내보내지 않는다. 서버가 보낸 message가 있으면 그것을 우선한다.
 */
export function getErrorDisplayMessage(error: unknown): string {
  if (isAxiosError(error)) {
    const serverMessage = error.response?.data?.message;
    if (serverMessage) return serverMessage;
    if (error.code === AxiosError.ECONNABORTED || error.code === AxiosError.ETIMEDOUT) return API_TIMEOUT_MESSAGE;
    if (!error.response) return "서버에 연결할 수 없습니다. 네트워크 상태를 확인한 뒤 다시 시도해 주세요.";
    return "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
  }
  return "알 수 없는 오류가 발생했습니다.";
}

export function getApiErrorMessage(error: unknown, fallback = "오류가 발생했습니다."): string {
  if (isAxiosError(error)) return error.response?.data?.message ?? fallback;
  return fallback;
}

export function getApiErrorStatus(error: unknown): number | undefined {
  if (isAxiosError(error)) return error.response?.status;
  return undefined;
}
