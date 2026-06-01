import { isAxiosError } from "axios";

export function getApiErrorMessage(error: unknown, fallback = "오류가 발생했습니다."): string {
  if (isAxiosError(error)) return error.response?.data?.message ?? fallback;
  return fallback;
}

export function getApiErrorStatus(error: unknown): number | undefined {
  if (isAxiosError(error)) return error.response?.status;
  return undefined;
}
