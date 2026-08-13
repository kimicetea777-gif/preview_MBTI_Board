"use client";

import { create } from "zustand";

const SESSION_HINT_COOKIE = "session=1";

// 백엔드(SessionCookieUtils)가 힌트 쿠키를 발급할 때 쓰는 Domain과 반드시 같은 값이어야 함
// (prod: .thebluebutter.com, 로컬: 빈 값 = host-only) — 안 맞으면 아래 clearSessionHint()가
// document.cookie로 지우려 해도 RFC 6265상 name+Path+Domain이 안 맞아 조용히 no-op됨
const COOKIE_DOMAIN = process.env.NEXT_PUBLIC_COOKIE_DOMAIN ?? "";

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
  const domainAttr = COOKIE_DOMAIN ? `; domain=${COOKIE_DOMAIN}` : "";
  document.cookie = `session=; path=/; max-age=0; SameSite=Lax${domainAttr}`;
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
