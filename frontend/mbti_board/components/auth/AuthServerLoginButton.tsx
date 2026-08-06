"use client";

import { OAUTH2_REDIRECT_KEY } from "@/components/auth/OAuth2CallbackContent";
import { toSafeRedirect } from "@/lib/safeRedirect";

const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
// 경로 끝의 "thebluebutter"는 backend application.yaml의
// spring.security.oauth2.client.registration 키와 정확히 일치해야 함 — 하나만 바꾸면 로그인이 깨짐.
const AS_AUTHORIZATION_PATH = "/oauth2/authorization/thebluebutter";

interface Props {
  redirectTo: string;
}

export default function AuthServerLoginButton({ redirectTo }: Props) {
  const handleClick = () => {
    sessionStorage.setItem(OAUTH2_REDIRECT_KEY, toSafeRedirect(redirectTo));  // OAuth2CallbackContent.tsx에서 꺼내서 redirect에 사용
    window.location.href = `${BACKEND_URL}${AS_AUTHORIZATION_PATH}`;  // 백엔드에 OAuth2 Authorization Request 생성 요청
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
    >
      로그인 / 회원가입
    </button>
  );
}
