"use client";

import axios from "axios";
import { useAuthStore } from "@/store/authStore";
import { broadcastLogout, broadcastTokenRefreshed } from "@/lib/authChannel";
import { ROUTES } from "@/lib/routes";
import { API } from "@/lib/apiEndpoints";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const REFRESH_URL = `${BASE_URL}${API.auth.refresh}`;

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

// 401 수신 시 silent refresh 후 원 요청 재시도
apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

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

      try {
        const newToken = await refreshPromise;
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch {
        useAuthStore.getState().setAccessToken(null);
        broadcastLogout();

        if (typeof window !== "undefined") {
          window.location.href = ROUTES.login;
        }
      }
    }

    return Promise.reject(error);
  },
);
