"use client";

import { memo } from "react";
import { OAUTH2_REDIRECT_KEY } from "@/components/auth/OAuth2CallbackContent";
import { toSafeRedirect } from "@/lib/safeRedirect";
import { PENDING_SOCIAL_PROVIDER_KEY } from "@/lib/loginUtils";

const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const PROVIDER_CONFIG = {
  google: {
    label: "Google로 계속하기",
    path: "/oauth2/authorization/google",
    className:
      "border border-gray-300 text-gray-700 hover:bg-gray-50",
  },
} as const;

type Provider = keyof typeof PROVIDER_CONFIG;

interface Props {
  provider: Provider;
  redirectTo: string;
  isLastUsed?: boolean;
}

export default memo(function SocialLoginButton({ provider, redirectTo, isLastUsed }: Props) {
  const { label, path, className } = PROVIDER_CONFIG[provider];

  const handleClick = () => {
    sessionStorage.setItem(OAUTH2_REDIRECT_KEY, toSafeRedirect(redirectTo));
    sessionStorage.setItem(PENDING_SOCIAL_PROVIDER_KEY, provider);
    window.location.href = `${BACKEND_URL}${path}`;
  };

  return (
    <div className="relative">
      {isLastUsed && (
        <div className="absolute bottom-[calc(100%-6px)] right-2 z-10 pointer-events-none animate-callout origin-bottom-right">
          <div className="relative bg-blue-500 text-white text-[11px] px-2.5 py-1 rounded-sm whitespace-nowrap shadow-sm">
            마지막 로그인
            <span className="absolute -bottom-1.25 right-5 w-2.5 h-2.5 bg-blue-500 rotate-45" />
          </div>
        </div>
      )}
      <button
        type="button"
        onClick={handleClick}
        className={`flex w-full items-center justify-center rounded-lg py-2 text-sm transition-colors ${className}`}
      >
        {label}
      </button>
    </div>
  );
});
