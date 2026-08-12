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
  ACCOUNT_WITHDRAWAL_PENDING: "연동이 해제된 계정입니다. 계정 관리 페이지에서 다시 연결하면 그대로 복구됩니다. 계정 복구까지 최대 하루 정도 걸릴 수 있습니다.",
  OAUTH2_ERROR: "로그인 중 오류가 발생했습니다.",
};

interface Props {
  error: string | null;
}

export default function OAuth2CallbackContent({ error }: Props) {
  const router = useRouter();
  const setLoggedIn = useAuthStore((s) => s.setLoggedIn);

  useEffect(() => {
    if (error) return;

    // 백엔드가 이 리다이렉트 응답에 세션 쿠키를 이미 심었으므로 여기선 로그인 상태만 반영하고 이동
    setLoggedIn(true);
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
