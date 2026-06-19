"use client";

import { isBrowser } from "@/lib/utils";
import { create } from "zustand";

// RT 수명(7일)과 동일하게 설정 — 미들웨어가 읽는 힌트 쿠키 (보안 정보 없음)
const SESSION_COOKIE_MAX_AGE = 604800;

function setSessionCookie(active: boolean) {
  if (!isBrowser()) return;
  if (active) {
    document.cookie = `session=1; path=/; max-age=${SESSION_COOKIE_MAX_AGE}; SameSite=Lax`;
  } else {
    document.cookie = "session=; path=/; max-age=0; SameSite=Lax";
  }
}

interface AuthState {
  accessToken: string | null;
  setAccessToken: (token: string | null) => void;
  getAccessToken: () => string | null;
  isLoggedIn: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  setAccessToken: (token) => {
    setSessionCookie(token !== null);
    set({ accessToken: token });
  },
  getAccessToken: () => get().accessToken,
  isLoggedIn: () => !!get().accessToken,
}));
