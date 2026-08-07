"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore";
import { subscribeAuthChannel } from "@/lib/authChannel";
import { ROUTES } from "@/constants/routes";

/** 탭 간 BroadcastChannel로 로그아웃 상태를 동기화한다 — 로그인 상태 자체는 세션 힌트 쿠키에서 직접 읽으므로 마운트 시 복구 로직이 필요 없다 */
export default function AuthSyncProvider({ children }: { children: React.ReactNode }) {
  const setLoggedIn = useAuthStore((s) => s.setLoggedIn);

  useEffect(() => {
    const unsubscribe = subscribeAuthChannel(() => {
      setLoggedIn(false);
      window.location.href = ROUTES.login;
    });

    return unsubscribe;
  }, [setLoggedIn]);

  return <>{children}</>;
}
