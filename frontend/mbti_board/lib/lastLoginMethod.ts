export const PENDING_SOCIAL_PROVIDER_KEY = "pending_social_provider";

const LAST_LOGIN_METHOD_KEY = "last_login_method";

export type LoginMethod = "email" | "google" | "kakao";

export function getLastLoginMethod(): LoginMethod | null {
  if (typeof window === "undefined") return null;
  return (localStorage.getItem(LAST_LOGIN_METHOD_KEY) as LoginMethod) ?? null;
}

export function setLastLoginMethod(method: LoginMethod): void {
  localStorage.setItem(LAST_LOGIN_METHOD_KEY, method);
}
