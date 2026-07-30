"use client";

import { isBrowser } from "@/lib/utils";
import { create } from "zustand";

// RT 수명(7일)과 동일하게 설정 — 미들웨어가 읽는 힌트 쿠키 (보안 정보 없음)
const SESSION_COOKIE_MAX_AGE = 604800;
// 로그인 시 선택한 자동 로그인 여부 — silent refresh 등 rememberMe를 모르는 호출에서 재사용 (보안 정보 없음)
const REMEMBER_ME_KEY = "rememberMe";

function getStoredRememberMe(): boolean {
  if (!isBrowser()) return true;
  return localStorage.getItem(REMEMBER_ME_KEY) !== "false";
}

/** RT 쿠키의 영속성(Max-Age 유무)과 동일하게 session 힌트 쿠키도 맞춰서 발급/삭제 */
function setSessionCookie(active: boolean, rememberMe: boolean) {
  if (!isBrowser()) return;
  if (active) {
    localStorage.setItem(REMEMBER_ME_KEY, String(rememberMe));
    const maxAge = rememberMe ? `max-age=${SESSION_COOKIE_MAX_AGE}; ` : "";
    document.cookie = `session=1; path=/; ${maxAge}SameSite=Lax`;
  } else {
    localStorage.removeItem(REMEMBER_ME_KEY);
    document.cookie = "session=; path=/; max-age=0; SameSite=Lax";
  }
}

interface AuthState {
  accessToken: string | null;
  /** rememberMe 생략 시(silent refresh 등) 로그인 시점에 저장된 값을 재사용 */
  setAccessToken: (token: string | null, rememberMe?: boolean) => void;
  getAccessToken: () => string | null;
  isLoggedIn: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  setAccessToken: (token, rememberMe) => {
    setSessionCookie(token !== null, rememberMe ?? getStoredRememberMe());
    set({ accessToken: token });
  },
  getAccessToken: () => get().accessToken,
  isLoggedIn: () => !!get().accessToken,
}));
