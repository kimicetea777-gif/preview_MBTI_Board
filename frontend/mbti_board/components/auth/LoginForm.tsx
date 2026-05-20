"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { login } from "@/lib/api/auth";
import { ROUTES } from "@/lib/routes";
import { OAUTH2_REDIRECT_KEY } from "@/components/auth/OAuth2CallbackContent";

const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

interface Props {
  redirectTo: string;
}

interface ApiError {
  response?: {
    data?: {
      message?: string;
    };
  };
}

export default function LoginForm({ redirectTo }: Props) {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      router.push(redirectTo || "/");
    },
  });

  const errorMsg = mutation.isError
    ? ((mutation.error as ApiError)?.response?.data?.message ?? "이메일 또는 비밀번호가 올바르지 않습니다.")
    : null;

  return (
    <section className="max-w-md mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">로그인</h1>

      <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <form
          onSubmit={(e) => { e.preventDefault(); mutation.mutate({ email, password }); }}
          className="space-y-4"
        >
          <div>
            <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
              이메일
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              placeholder="example@email.com"
              disabled={mutation.isPending}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
            />
          </div>
          <div>
            <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
              비밀번호
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              disabled={mutation.isPending}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
            />
          </div>
          {errorMsg && <p className="text-xs text-red-500">{errorMsg}</p>}
          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
          >
            {mutation.isPending ? "로그인 중..." : "로그인"}
          </button>
        </form>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-200" />
          </div>
          <div className="relative flex justify-center">
            <span className="bg-white px-2 text-xs text-gray-400">또는 소셜 로그인</span>
          </div>
        </div>

        <nav className="space-y-2">
          <button
            type="button"
            onClick={() => {
              sessionStorage.setItem(OAUTH2_REDIRECT_KEY, redirectTo);
              window.location.href = `${BACKEND_URL}/oauth2/authorization/google`;
            }}
            className="flex w-full items-center justify-center rounded-lg border border-gray-300 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Google로 로그인
          </button>
          <button
            type="button"
            onClick={() => {
              sessionStorage.setItem(OAUTH2_REDIRECT_KEY, redirectTo);
              window.location.href = `${BACKEND_URL}/oauth2/authorization/kakao`;
            }}
            className="flex w-full items-center justify-center rounded-lg bg-yellow-300 py-2 text-sm text-gray-900 hover:bg-yellow-400 transition-colors"
          >
            카카오로 로그인
          </button>
        </nav>

        <p className="text-center text-xs text-gray-500">
          계정이 없으신가요?{" "}
          <Link href={ROUTES.signup} className="text-gray-900 underline">
            회원가입
          </Link>
        </p>
      </article>
    </section>
  );
}
