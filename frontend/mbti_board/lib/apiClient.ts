"use client";

import axios from "axios";
import { useAuthStore } from "@/store/authStore";
import { broadcastLogout, broadcastTokenRefreshed } from "@/lib/authChannel";
import { ROUTES } from "@/constants/routes";
import { API } from "@/constants/apiEndpoints";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const REFRESH_URL = `${BASE_URL}${API.auth.reissue}`;

export const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // RT HttpOnly Cookie 자동 전송
});

// AT를 Authorization 헤더에 주입
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshPromise: Promise<string> | null = null;

// session 힌트 쿠키 존재 여부 — 있으면 RT 쿠키도 유효할 가능성이 있어 silent refresh 시도 대상
function hasSessionCookie(): boolean {
  return (
    typeof document !== "undefined" &&
    document.cookie.split(";").some((c) => c.trim() === "session=1")
  );
}

// silent refresh 진행 중이면 그 Promise를 공유(중복 요청 방지)
// 주의: AuthSyncProvider의 마운트 시 silent refresh도 반드시 이 함수를 통해야 함 —
//       별도 axios 호출로 분리하면 두 요청이 같은(구) RT 쿠키로 동시에 서버에 도달해
//       하나는 로테이션 성공, 하나는 RT Reuse Detection으로 전체 세션 로그아웃되는 경합이 발생함
export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = axios
      .post(REFRESH_URL, null, { withCredentials: true })
      .then(({ data }) => {
        const newToken: string = data.data.accessToken;
        useAuthStore.getState().setAccessToken(newToken);
        broadcastTokenRefreshed(newToken);
        return newToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

// 401 수신 시 silent refresh 후 원 요청 재시도
apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry && hasSessionCookie()) {
      originalRequest._retry = true;

      try {
        const newToken = await refreshAccessToken();
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch {
        useAuthStore.getState().setAccessToken(null);
        broadcastLogout();

        if (typeof window !== "undefined" && window.location.pathname !== ROUTES.login) {
          window.location.href = ROUTES.login;
        }
      }
    }

    return Promise.reject(error);
  },
);
