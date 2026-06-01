"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/store/authStore";
import { ROUTES } from "@/lib/routes";
import { toSafeRedirect } from "@/lib/safeRedirect";
import { setLastLoginMethod, PENDING_SOCIAL_PROVIDER_KEY, type LoginMethod } from "@/lib/lastLoginMethod";

export const OAUTH2_REDIRECT_KEY = "oauth2_redirect";

const ERROR_MESSAGES: Record<string, string> = {
  DUPLICATE_EMAIL: "이미 이메일/비밀번호로 가입된 계정이 있습니다.\n해당 이메일로 로그인해 주세요.",
  OAUTH2_EMAIL_REQUIRED: "이메일 제공에 동의해야 소셜 로그인이 가능합니다.",
  OAUTH2_ERROR: "로그인 중 오류가 발생했습니다.",
};

interface Props {
  accessToken: string | null;
  error: string | null;
}

export default function OAuth2CallbackContent({ accessToken, error }: Props) {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  useEffect(() => {
    if (error || !accessToken) return;
    setAccessToken(accessToken);
    const raw = sessionStorage.getItem(OAUTH2_REDIRECT_KEY) ?? "/";
    sessionStorage.removeItem(OAUTH2_REDIRECT_KEY);
    const provider = sessionStorage.getItem(PENDING_SOCIAL_PROVIDER_KEY) as LoginMethod | null;
    if (provider) {
      setLastLoginMethod(provider);
      sessionStorage.removeItem(PENDING_SOCIAL_PROVIDER_KEY);
    }
    router.replace(toSafeRedirect(raw));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (error) {
    const message = ERROR_MESSAGES[error] ?? ERROR_MESSAGES.OAUTH2_ERROR;
    return (
      <section className="max-w-md mx-auto text-center py-16 space-y-4">
        <p className="text-sm font-medium text-red-600 whitespace-pre-line">{message}</p>
        <Link
          href={ROUTES.login}
          className="inline-block rounded-lg bg-gray-900 px-5 py-2 text-sm text-white hover:bg-gray-700 transition-colors"
        >
          로그인 페이지로
        </Link>
      </section>
    );
  }

  return (
    <section className="text-center">
      <p className="text-sm text-gray-500">로그인 처리 중...</p>
    </section>
  );
}
