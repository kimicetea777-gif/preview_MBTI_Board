"use client";

import axios from "axios";
import { useAuthStore } from "@/store/authStore";
import { broadcastLogout } from "@/lib/authChannel";
import { ROUTES } from "@/constants/routes";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // 세션 쿠키 자동 전송 — 서버가 매 요청마다 AS 토큰 갱신을 조용히 처리
});

// 서버가 이미 세션 갱신을 시도한 뒤의 401이므로 재시도 없이 바로 로그아웃 신호로 처리
apiClient.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().setLoggedIn(false);
      broadcastLogout();

      if (typeof window !== "undefined" && window.location.pathname !== ROUTES.login) {
        window.location.href = ROUTES.login;
      }
    }

    return Promise.reject(error);
  },
);
