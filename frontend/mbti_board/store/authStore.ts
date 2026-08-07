"use client";

import { create } from "zustand";

const SESSION_HINT_COOKIE = "session=1";

// 실제 세션 값은 HttpOnly 쿠키라 JS가 못 읽음 — 로그인 여부만 알려주는 비-HttpOnly 힌트 쿠키를 대신 읽는다
// (보안 정보 없음, 서버의 SessionCookieUtils가 실제 세션 쿠키와 같이 발급/삭제)
function hasSessionHint(): boolean {
  return typeof document !== "undefined" && document.cookie.split(";").some((c) => c.trim() === SESSION_HINT_COOKIE);
}

// 서버가 세션을 강제로 끊었지만(밴 등) 이 브라우저에 Set-Cookie로 알려줄 기회가 없었던 경우를 대비해
// 로그아웃으로 확정되는 시점엔 힌트 쿠키도 클라이언트에서 같이 지운다 — 안 지우면 다음 새로고침 때
// hasSessionHint()가 여전히 true를 읽어 "로그인됨" UI가 잠깐 다시 뜨는 문제가 생김
function clearSessionHint() {
  if (typeof document === "undefined") return;
  document.cookie = "session=; path=/; max-age=0; SameSite=Lax";
}

interface AuthState {
  loggedIn: boolean;
  setLoggedIn: (loggedIn: boolean) => void;
  isLoggedIn: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  loggedIn: hasSessionHint(),
  setLoggedIn: (loggedIn) => {
    if (!loggedIn) clearSessionHint();
    set({ loggedIn });
  },
  isLoggedIn: () => get().loggedIn,
}));
