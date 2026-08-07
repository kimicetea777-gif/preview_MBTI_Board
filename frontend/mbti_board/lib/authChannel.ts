"use client";

// 같은 브라우저의 다른 탭들 사이에 로그아웃 상태를 동기화하기 위한 BroadcastChannel 래퍼.
// 한 탭에서 로그아웃하면 나머지 탭들도 즉시 반영되어야 하므로 사용.

const AUTH_EVENT = {
  LOGOUT: "LOGOUT",
} as const;

type AuthMessage = { type: typeof AUTH_EVENT.LOGOUT };

// 채널 인스턴스를 모듈 스코프에서 하나만 유지 (탭당 1개)
let channel: BroadcastChannel | null = null;

function getChannel(): BroadcastChannel | null {
  // SSR 환경 및 BroadcastChannel 미지원 브라우저(구형 인앱 브라우저 등)에서는 사용 불가
  if (typeof window === "undefined" || typeof BroadcastChannel === "undefined") return null;
  if (!channel) channel = new BroadcastChannel("auth");
  return channel;
}

// 다른 탭들에게 로그아웃 사실을 전파
export function broadcastLogout(): void {
  getChannel()?.postMessage({ type: AUTH_EVENT.LOGOUT } satisfies AuthMessage);
}

// 다른 탭에서 발생한 로그아웃을 구독. 반환값은 구독 해제 함수(cleanup)
export function subscribeAuthChannel(onLogout: () => void): () => void {
  const ch = getChannel();
  if (!ch) return () => {};

  const handler = (e: MessageEvent<AuthMessage>) => {
    if (e.data.type === AUTH_EVENT.LOGOUT) onLogout();
  };

  ch.addEventListener("message", handler);
  return () => ch.removeEventListener("message", handler);
}
