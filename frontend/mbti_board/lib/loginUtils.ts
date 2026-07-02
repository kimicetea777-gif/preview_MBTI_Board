export const PENDING_SOCIAL_PROVIDER_KEY = "pending_social_provider";

const LAST_LOGIN_METHOD_KEY = "last_login_method";

export const LOGIN_METHOD = {
  EMAIL: "email",
  GOOGLE: "google",
  KAKAO: "kakao",
} as const;

export type LoginMethod = typeof LOGIN_METHOD[keyof typeof LOGIN_METHOD];

export function getLastLoginMethod(): LoginMethod | null {
  if (typeof window === "undefined") return null;
  return (localStorage.getItem(LAST_LOGIN_METHOD_KEY) as LoginMethod) ?? null;
}

export function setLastLoginMethod(method: LoginMethod): void {
  localStorage.setItem(LAST_LOGIN_METHOD_KEY, method);
}

export const LOGIN_METHOD_LABEL: Record<LoginMethod, string> = {
  [LOGIN_METHOD.EMAIL]: "이메일",
  [LOGIN_METHOD.GOOGLE]: "Google",
  [LOGIN_METHOD.KAKAO]: "Kakao",
};
