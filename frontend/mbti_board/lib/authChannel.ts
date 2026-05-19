"use client";

const AUTH_EVENT = {
  TOKEN_REFRESHED: "TOKEN_REFRESHED",
  LOGOUT: "LOGOUT",
} as const;

type AuthMessage =
  | { type: typeof AUTH_EVENT.TOKEN_REFRESHED; accessToken: string }
  | { type: typeof AUTH_EVENT.LOGOUT };

let channel: BroadcastChannel | null = null;

function getChannel(): BroadcastChannel | null {
  if (typeof window === "undefined") return null;
  if (!channel) channel = new BroadcastChannel("auth");
  return channel;
}

export function broadcastTokenRefreshed(accessToken: string): void {
  getChannel()?.postMessage({ type: AUTH_EVENT.TOKEN_REFRESHED, accessToken } satisfies AuthMessage);
}

export function broadcastLogout(): void {
  getChannel()?.postMessage({ type: AUTH_EVENT.LOGOUT } satisfies AuthMessage);
}

export function subscribeAuthChannel(
  onTokenRefreshed: (accessToken: string) => void,
  onLogout: () => void,
): () => void {
  const ch = getChannel();
  if (!ch) return () => {};

  const handler = (e: MessageEvent<AuthMessage>) => {
    if (e.data.type === AUTH_EVENT.TOKEN_REFRESHED) onTokenRefreshed(e.data.accessToken);
    if (e.data.type === AUTH_EVENT.LOGOUT) onLogout();
  };

  ch.addEventListener("message", handler);
  return () => ch.removeEventListener("message", handler);
}
