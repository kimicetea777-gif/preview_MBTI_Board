"use client";

import { useSyncExternalStore } from "react";
import Link from "next/link";
import { ROUTES } from "@/constants/routes";
import { getLastLoginMethod, LOGIN_METHOD_LABEL } from "@/lib/loginUtils";
import EmailInput from "@/components/common/EmailInput";
import ErrorMessage from "@/components/common/ErrorMessage";
import SocialLoginButtons from "@/components/auth/SocialLoginButtons";
import AuthCardHeader from "@/components/auth/AuthCardHeader";
import { MdPeopleAlt } from "react-icons/md";
import { HiArrowRight } from "react-icons/hi";

interface Props {
  email: string;
  onEmailChange: (email: string) => void;
  onSubmit: () => void;
  isPending: boolean;
  errorMsg: string | null;
}

export default function EmailStep({ email, onEmailChange, onSubmit, isPending, errorMsg }: Props) {
  const lastMethod = useSyncExternalStore(
    () => () => {},
    getLastLoginMethod,
    () => null,
  );

  return (
    <section className="max-w-120 mx-auto isolate relative">
      <article
        className="bg-surface-container-lowest rounded-[1.5rem] border border-surface-variant overflow-hidden relative"
        style={{ boxShadow: "0 20px 40px -15px rgba(49, 23, 10, 0.08)" }}
      >
        <AuthCardHeader
          icon={<MdPeopleAlt className="text-primary text-3xl" />}
          step={{ current: 1, total: 3 }}
          title="회원가입"
          description={<><strong className="font-bold">F의 무한 공감</strong>이 필요한 날,{" "}<strong className="font-bold">T의 냉철한 팩폭</strong>이 필요한 날.<br />당신의 오늘 기분에 딱 맞는 성격 유형을 만나보세요.</>}
        />
        <div className="p-8 md:p-10 -mt-6">
          <form
            onSubmit={(e) => { e.preventDefault(); onSubmit(); }}
            className="space-y-3"
          >
            <div className="space-y-1.5">
              <EmailInput
                id="signup-email"
                value={email}
                onChange={(e) => onEmailChange(e.target.value)}
                required
                disabled={isPending}
                hint="example@mail.com"
                error={!!errorMsg}
              />
              <ErrorMessage message={errorMsg} />
            </div>
            <button
              type="submit"
              disabled={isPending}
              className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              {isPending ? (
                <>
                  <span className="w-4 h-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
                  발송 중...
                </>
              ) : (
                <>
                  인증 메일 발송
                  <HiArrowRight className="text-lg group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </button>
          </form>
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-outline-variant" />
            </div>
            <div className="relative flex justify-center">
              <span className="bg-surface-container-lowest px-3 text-xs text-on-surface-variant">또는 간편 시작</span>
            </div>
          </div>
          <div className="flex max-w-70 mx-auto -mt-2 mb-3">
            {lastMethod && (
              <div className="flex items-center justify-between rounded-lg bg-blue-50 border border-blue-100 px-3 py-2 gap-2">
                <span className="text-xs text-blue-700">
                  마지막에 <strong>{LOGIN_METHOD_LABEL[lastMethod]}</strong>로 로그인하셨어요
                </span>
                <Link href={ROUTES.login} className="text-xs font-medium text-blue-700 underline whitespace-nowrap">
                  로그인 이동
                </Link>
              </div>
            )}
          </div>
          <SocialLoginButtons redirectTo={ROUTES.board.all} />
          <footer className="mt-5 text-center">
            <p className="text-sm text-on-surface-variant">
              이미 계정이 있으신가요?{" "}
              <Link href={ROUTES.login} className="font-bold text-secondary hover:underline underline-offset-4 transition-all">
                로그인
              </Link>
            </p>
          </footer>
        </div>
      </article>
    </section>
  );
}
