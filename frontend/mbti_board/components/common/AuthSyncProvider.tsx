"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore";
import { subscribeAuthChannel } from "@/lib/authChannel";
import axios from "axios";
import { ROUTES } from "@/lib/routes";
import { API } from "@/lib/apiEndpoints";

/**
 * 앱 최초 마운트 시 silent refresh로 AT 복구,
 * 탭 간 BroadcastChannel로 AT/로그아웃 상태 동기화
 */
export default function AuthSyncProvider({ children }: { children: React.ReactNode }) {
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  useEffect(() => {
    // 탭 간 동기화 구독
    const unsubscribe = subscribeAuthChannel(
      (token) => setAccessToken(token),
      () => {
        setAccessToken(null);
        window.location.href = ROUTES.login;
      },
    );

    // 새 탭 or 새로고침: AT가 없고 session=1 쿠키가 있으면 silent refresh 시도
    // apiClient 대신 axios 직접 사용 — 401 인터셉터 개입 방지
    const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
    const hasToken = useAuthStore.getState().getAccessToken();
    const hasSession = typeof document !== "undefined" && document.cookie.split(";").some((c) => c.trim() === "session=1");
    if (!hasToken && hasSession) {
      axios
        .post<{ data: { accessToken: string } }>(`${BASE_URL}${API.auth.reissue}`, null, { withCredentials: true })
        .then(({ data }) => setAccessToken(data.data.accessToken))
        .catch(() => {}); // RT 없으면 비로그인 상태로 유지
    }

    return unsubscribe;
  }, [setAccessToken]);

  return <>{children}</>;
}
