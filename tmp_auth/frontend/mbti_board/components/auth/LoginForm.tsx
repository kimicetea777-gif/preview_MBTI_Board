"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { login } from "@/lib/api/auth";
import { ROUTES } from "@/constants/routes";
import { toSafeRedirect } from "@/lib/safeRedirect";
import { getLastLoginMethod, setLastLoginMethod, LOGIN_METHOD } from "@/lib/loginUtils";
import { useLocalStorageSnapshot } from "@/lib/localStorageUtils";
import { getApiErrorMessage } from "@/lib/apiError";
import { FIND_ACCOUNT_TAB } from "@/types/auth";
import { MdLogin, MdPeopleAlt, MdCheck } from "react-icons/md";
import { RiLockPasswordLine } from "react-icons/ri";
import SocialLoginButtons from "@/components/auth/SocialLoginButtons";
import ErrorMessage from "@/components/common/ErrorMessage";
import AuthCardHeader from "@/components/auth/AuthCardHeader";
import EmailInput from "@/components/common/EmailInput";
import PasswordInput from "@/components/common/PasswordInput";

interface Props {
  redirectTo: string;
}

const GUEST_FEATURES = [
  "자유게시판 글쓰기 가능",
  "댓글 및 대댓글 참여 가능",
  "게시글 조회 투표 현황",
];

export default function LoginForm({ redirectTo }: Props) {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const lastMethod = useLocalStorageSnapshot(getLastLoginMethod, null);

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setLastLoginMethod(LOGIN_METHOD.EMAIL);
      setAccessToken(data.accessToken, rememberMe);
      router.push(toSafeRedirect(redirectTo));
    },
  });

  const errorMsg = mutation.isError
    ? getApiErrorMessage(mutation.error, "이메일 또는 비밀번호가 올바르지 않습니다.")
    : null;

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    mutation.mutate({ email, password, rememberMe });
  };

  return (
    <div className="relative w-full max-w-248 isolate">
      <div className="flex flex-col lg:flex-row gap-6 items-stretch">

        {/* -------------- Left: Promo Box -------------- */}
        <div
          className="flex-1 bg-card border border-outline-variant rounded-[1.5rem] p-8 md:p-12 flex flex-col items-center justify-center text-center gap-6"
          style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.12)" }}
        >
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[#ffdbcb]">
            <MdPeopleAlt className="text-primary text-3xl" />
          </div>

          <div className="space-y-2">
            <h2 className="text-xl font-bold text-primary leading-snug">
              로그인 없이도<br />글쓰기/댓글이 가능해요!
            </h2>
            <p className="text-sm text-on-surface-variant leading-relaxed">
              비회원으로도 커뮤니티에 참여할 수 있어요.
            </p>
          </div>

          <ul className="bg-surface-container-low p-4 rounded-xl border border-outline-variant/40 space-y-3 text-sm text-on-surface-variant text-left w-full max-w-[16rem]">
            {GUEST_FEATURES.map((feature) => (
              <li key={feature} className="flex items-center gap-2">
                <MdCheck className="text-primary text-lg shrink-0" />
                {feature}
              </li>
            ))}
          </ul>

          <Link
            href={ROUTES.board.all}
            className="mt-2 w-full max-w-[16rem] h-12 border border-primary text-primary text-sm font-semibold rounded-md hover:bg-primary/5 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
          >
            로그인 없이 둘러보기
          </Link>
        </div>

        {/* -------------- Right: Login Card -------------- */}
        <article
          className="flex-1 bg-card border border-outline-variant rounded-[1.5rem] overflow-hidden"
          style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.12)" }}
        >
          <AuthCardHeader
            icon={<MdLogin className="text-primary text-3xl" />}
            title="로그인"
            description="계정에 로그인하세요."
          />
          <div className="p-8 md:p-12">

          {/* -------------- Form -------------- */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <EmailInput
              id="login-email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); mutation.reset(); }}
              onClear={() => setEmail("")}
              required
              disabled={mutation.isPending}
            />

            <PasswordInput
              id="login-password"
              label="비밀번호"
              icon={<RiLockPasswordLine />}
              value={password}
              onChange={(e) => { setPassword(e.target.value); mutation.reset(); }}
              required
              disabled={mutation.isPending}
            />

            <label className="flex items-center gap-2 text-sm text-on-surface-variant cursor-pointer select-none">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                disabled={mutation.isPending}
                className="w-4 h-4 rounded border-outline-variant text-primary focus:ring-primary"
              />
              자동 로그인
            </label>

            <ErrorMessage message={errorMsg} />

            {/* -------------- Last Login Alert -------------- */}
            <div className="relative">
              {lastMethod === "email" && (
                <div className="absolute bottom-[calc(100%-6px)] right-2 z-10 pointer-events-none animate-callout origin-bottom-right">
                  <div className="relative bg-blue-500 text-white text-[11px] px-2.5 py-1 rounded-sm whitespace-nowrap shadow-sm">
                    지난번에 이용했어요
                    <span className="absolute -bottom-1.25 right-5 w-2.5 h-2.5 bg-blue-500 rotate-45" />
                  </div>
                </div>
              )}
              {/* -------------- Submit Button -------------- */}
              <button
                type="submit"
                disabled={mutation.isPending}
                className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
              >
                {mutation.isPending ? (
                  <div className="flex items-center gap-2">
                    <span className="w-4 h-4 border-2 border-t-transparent border-white rounded-full animate-spin" />
                    로그인 중...
                  </div>
                ) : (
                  <>로그인 <MdLogin className="text-lg" /></>
                )}
              </button>
            </div>
          </form>

          {/* -------------- Divider -------------- */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-outline-variant" />
            </div>
            <div className="relative flex justify-center">
              <span className="bg-card px-2 text-xs text-muted-foreground">또는 소셜 로그인</span>
            </div>
          </div>

          {/* -------------- Social Login -------------- */}
          <SocialLoginButtons redirectTo={redirectTo} lastUsedMethod={lastMethod} />

          {/* -------------- Footer -------------- */}
          <footer className="mt-3 pt-3 flex flex-col items-center gap-4">
            <p className="text-sm text-on-surface-variant">
              계정이 없으신가요?{" "}
              <Link href={ROUTES.signup} className="font-semibold text-primary hover:text-secondary transition-colors">
                회원가입
              </Link>
            </p>
            <div className="flex items-center gap-3 text-sm text-muted-foreground">
              <Link href={ROUTES.accountFind} className="hover:text-on-surface-variant transition-colors">
                이메일 찾기
              </Link>
              <span className="text-outline-variant">|</span>
              <Link
                href={`${ROUTES.accountFind}?tab=${FIND_ACCOUNT_TAB.PASSWORD}`}
                className="hover:text-on-surface-variant transition-colors"
              >
                비밀번호 찾기
              </Link>
            </div>
          </footer>
          </div>
        </article>

      </div>
    </div>
  );
}
