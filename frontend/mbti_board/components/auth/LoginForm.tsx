"use client";

import { useState, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { login } from "@/lib/api/auth";
import { ROUTES } from "@/lib/routes";
import { toSafeRedirect } from "@/lib/safeRedirect";
import { getLastLoginMethod, setLastLoginMethod } from "@/lib/lastLoginMethod";
import { getApiErrorMessage } from "@/lib/apiError";
import { RiLockPasswordLine } from "react-icons/ri";
import Logo from "@/components/common/Logo";
import SocialLoginButton from "@/components/auth/SocialLoginButton";
import EmailInput from "@/components/common/EmailInput";
import PasswordInput from "@/components/common/PasswordInput";

interface Props {
  redirectTo: string;
}

export default function LoginForm({ redirectTo }: Props) {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const lastMethod = useSyncExternalStore(
    () => () => {},
    getLastLoginMethod,
    () => null,
  );

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setLastLoginMethod("email");
      setAccessToken(data.accessToken);
      router.push(toSafeRedirect(redirectTo));
    },
  });

  const errorMsg = mutation.isError
    ? getApiErrorMessage(mutation.error, "이메일 또는 비밀번호가 올바르지 않습니다.")
    : null;

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    mutation.mutate({ email, password });
  };

  return (
    <section className={`max-w-md mx-auto${mutation.isPending ? " cursor-not-allowed" : ""}`}>
      

      <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">

        {/* ------------------------------------ Logo ------------------------------------ */}
        <div className="flex justify-center mb-6">
          <Logo size="lg" />
        </div>

        {/* ------------------------------------ Form ------------------------------------ */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* ------------------------------------ Input-Email ------------------------------------ */}
          <EmailInput
            id="login-email"
            value={email}
            onChange={(e) => { setEmail(e.target.value); mutation.reset(); }}
            onClear={() => setEmail("")}
            required
            disabled={mutation.isPending}
          />

          {/* ------------------------------------ Input-Password ------------------------------------ */}
          <PasswordInput
            id="login-password"
            label="비밀번호"
            icon={<RiLockPasswordLine />}
            value={password}
            onChange={(e) => { setPassword(e.target.value); mutation.reset(); }}
            required
            disabled={mutation.isPending}
          />

          {/* ------------------------------------ Error-Massage ------------------------------------ */}
          {errorMsg && <p className="text-xs text-red-500">{errorMsg}</p>}

          {/* ------------------------------------ Submit-Button ------------------------------------ */}
          <div className="relative">
            {lastMethod === "email" && (
              <div className="absolute bottom-[calc(100%-6px)] right-2 z-10 pointer-events-none animate-callout origin-bottom-right">
                <div className="relative bg-blue-500 text-white text-[11px] px-2.5 py-1 rounded-sm whitespace-nowrap shadow-sm">
                  지난번에 이용했어요
                  <span className="absolute -bottom-1.25 right-5 w-2.5 h-2.5 bg-blue-500 rotate-45" />
                </div>
              </div>
            )}
            <button
              type="submit"
              disabled={mutation.isPending}
              className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
            >
              {mutation.isPending ? "로그인 중..." : "로그인"}
            </button>
          </div>
        </form>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-200" />
          </div>
          <div className="relative flex justify-center">
            <span className="bg-white px-2 text-xs text-gray-400">또는 소셜 로그인</span>
          </div>
        </div>

        {/* ------------------------------------ Button-Social-Login ------------------------------------ */}
        <nav className="space-y-2">
          <SocialLoginButton provider="google" redirectTo={redirectTo} isLastUsed={lastMethod === "google"} />
          <SocialLoginButton provider="kakao"  redirectTo={redirectTo} isLastUsed={lastMethod === "kakao"} />
        </nav>

        {/* ------------------------------------ Link-SignUp.page ------------------------------------ */}
        <p className="text-center text-xs text-gray-500">
          계정이 없으신가요?{" "}
          <Link href={ROUTES.signup} className="text-gray-900">
            <strong>회원가입</strong>
          </Link>
        </p>

        {/* ------------------------------------ Link-Find.page ------------------------------------ */}
        <p className="text-center text-xs text-gray-400">
          <Link href={ROUTES.accountFind} className="hover:text-gray-600 transition-colors text-xs right-0">
            이메일 찾기
          </Link>
          {" | "}
          <Link href={ROUTES.accountFind + "?tab=password"} className="hover:text-gray-600 transition-colors">
            비밀번호 찾기
          </Link>
        </p>
      </article>
    </section>
  );
}
