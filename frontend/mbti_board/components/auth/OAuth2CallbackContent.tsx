"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/store/authStore";
import { ROUTES } from "@/constants/routes";
import { toSafeRedirect } from "@/lib/safeRedirect";

export const OAUTH2_REDIRECT_KEY = "oauth2_redirect";

const ERROR_MESSAGES: Record<string, string> = {
  ACCOUNT_LOCKED: "잠긴 계정입니다. 관리자에게 문의해 주세요.",
  ACCOUNT_BANNED: "이용이 제한된 계정입니다. 관리자에게 문의해 주세요.",
  ACCOUNT_INACTIVE: "비활성화된 계정입니다. 관리자에게 문의해 주세요.",
  ACCOUNT_WITHDRAWN: "탈퇴한 계정입니다. 다시 로그인하면 재가입할 수 있습니다.",
  OAUTH2_ERROR: "로그인 중 오류가 발생했습니다.",
};

interface Props {
  error: string | null;
}

export default function OAuth2CallbackContent({ error }: Props) {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  useEffect(() => {
    if (error) return;

    const params = new URLSearchParams(window.location.hash.slice(1));
    const accessToken = params.get("accessToken");
    if (!accessToken) return;

    // AS 로그인은 별도 체크박스가 없고 백엔드도 항상 유지 로그인으로 RT를 발급함
    setAccessToken(accessToken, true);
    const raw = sessionStorage.getItem(OAUTH2_REDIRECT_KEY) ?? ROUTES.home;
    sessionStorage.removeItem(OAUTH2_REDIRECT_KEY);
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
