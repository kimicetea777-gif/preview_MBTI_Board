"use client";

import { useState, useSyncExternalStore } from "react";
import Link from "next/link";
import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchMbtiList } from "@/lib/api/mbti";
import { sendEmailVerification, signup } from "@/lib/api/auth";
import { ROUTES } from "@/lib/routes";

import { STEP, type Step } from "@/app/(auth)/signup/steps";
import { getApiErrorMessage, getApiErrorStatus } from "@/lib/apiError";
import SocialLoginButton from "@/components/auth/SocialLoginButton";
import FloatingLabelInput from "@/components/common/FloatingLabelInput";
import EmailInput from "@/components/common/EmailInput";
import PasswordInput from "@/components/common/PasswordInput";
import MbtiSelect from "@/components/common/MbtiSelect";
import LabeledFilterBox from "@/components/common/LabeledFilterBox";
import { HiArrowLeft } from "react-icons/hi";
import { getLastLoginMethod, type LoginMethod } from "@/lib/lastLoginMethod";
import { PASSWORD_REGEX, PASSWORD_MESSAGE } from "@/lib/validationConstants";

const EV_EMAIL_KEY = "ev_email";

const METHOD_LABEL: Record<LoginMethod, string> = {
  email: "이메일",
  google: "Google",
  kakao: "Kakao",
};

interface Props {
  initialStep: Step;
  initialEmail: string;

}

export default function SignupContent({ initialStep, initialEmail }: Props) {
  const [step, setStep] = useState<Step>(initialStep);
  const [email, setEmail] = useState(initialEmail);
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [mbtiId, setMbtiId] = useState("UNKNOWN");
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const { data: mbtiList = [] } = useQuery({
    queryKey: ["mbtiList"],
    queryFn: fetchMbtiList,
    staleTime: Infinity,
  });

  const sendMutation = useMutation({
    mutationFn: sendEmailVerification,
    onSuccess: () => {
      sessionStorage.setItem(EV_EMAIL_KEY, email);
      setStep(STEP.EMAIL_SENT);
    },
  });

  const signupMutation = useMutation({
    mutationFn: signup,
    onSuccess: () => setStep(STEP.DONE),
  });

  const lastMethod = useSyncExternalStore(
    () => () => {},
    getLastLoginMethod,
    () => null,
  );

  const sendErrorMsg = sendMutation.isError
    ? getApiErrorMessage(sendMutation.error, "이메일 발송에 실패했습니다. 다시 시도해 주세요.")
    : null;

  const signupErrorMsg =
    passwordError ??
    (signupMutation.isError
      ? getApiErrorMessage(signupMutation.error, "회원가입에 실패했습니다. 이메일 인증을 완료했는지 확인해 주세요.")
      : null);

  
  if (step === STEP.EMAIL) {
    return (
      <section className="max-w-md mx-auto">
        <div className="flex items-center justify-between mb-1">
          <h1 className="text-2xl font-bold text-gray-900">회원가입</h1>
          <Link href={ROUTES.login} className="text-gray-400 hover:text-gray-700 transition-colors">
            <HiArrowLeft size={20} />
          </Link>
        </div>
        <p className="text-xs text-gray-400 mb-4">Step 1/3 — 이메일 인증</p>
        
        <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
          <form
            onSubmit={(e) => { e.preventDefault(); sendMutation.mutate({ email }); }}
            className="space-y-4"
          >
            <EmailInput
              id="signup-email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onClear={() => setEmail("")}
              required
              disabled={sendMutation.isPending}
              hint="example@mail.com"
              error={!!sendErrorMsg}
            />
            {sendErrorMsg && <p className="text-xs text-red-500">{sendErrorMsg}</p>}
            <button
              type="submit"
              disabled={sendMutation.isPending}
              className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
            >
              {sendMutation.isPending ? "발송 중..." : "인증 메일 발송"}
            </button>
          </form>
          
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200" />
            </div>
            <div className="relative flex justify-center">
              <span className="bg-white px-2 text-xs text-gray-400">또는 간편 시작</span>
            </div>
          </div>
          <nav className="space-y-2">
            <SocialLoginButton provider="google" redirectTo={ROUTES.board.all} />
            <SocialLoginButton provider="kakao" redirectTo={ROUTES.board.all} />
          </nav>
        </article>
        <div className="mt-3">
          {lastMethod && (
            <div className="flex items-center justify-between rounded-lg bg-blue-50 border border-blue-100 px-3 py-2 gap-2">
              <span className="text-xs text-blue-700">
                마지막에 <strong>{METHOD_LABEL[lastMethod]}</strong>로 로그인하셨어요
              </span>
              <Link href={ROUTES.login} className="text-xs font-medium text-blue-700 underline whitespace-nowrap">
                로그인 이동
              </Link>
            </div>
          )}
        </div>
      </section>
      
    );
  }

  if (step === STEP.EMAIL_SENT) {
    return (
      <section className="max-w-md mx-auto">
        <div className="flex items-center justify-between mb-1">
          <h1 className="text-2xl font-bold text-gray-900">회원가입</h1>
          <button
            type="button"
            onClick={() => { setStep(STEP.EMAIL); sendMutation.reset(); }}
            className="text-gray-400 hover:text-gray-700 transition-colors"
          >
            <HiArrowLeft size={20} />
          </button>
        </div>
        <p className="text-xs text-gray-400 mb-6">Step 2/3 — 이메일 전송 완료</p>
        <article className="bg-white rounded-xl border border-gray-200 p-6 text-center space-y-3">
          <p className="text-3xl">📬</p>
          <p className="text-sx font-medium text-gray-900">인증 메일을 발송했습니다</p>
          <p className="text-x text-gray-500">
            <strong>{email}</strong>로 발송된 링크를 클릭하면 다음 단계로 이동합니다.
            <br />
            링크는 10분간 유효합니다.
          </p>
          <button
            onClick={() => { setStep(STEP.EMAIL); sendMutation.reset(); }}
            className="text-x text-gray-400 underline"
          >
            이메일 다시 입력하기
          </button>
        </article>
      </section>
    );
  }

  if (step === STEP.FORM) {
    return (
      <section className="max-w-md mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">회원가입</h1>
        <p className="text-xs text-gray-400 mb-6">Step 3/3 — 회원 정보 입력</p>
        <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-6">
          <div className={`flex items-center justify-between rounded-lg px-3 py-2 gap-2 border ${getApiErrorStatus(signupMutation.error) === 400 ? "bg-red-50 border-red-300" : "bg-green-50 border-green-200"}`}>
            <span className="text-xs text-gray-600">{email}</span>
            <div className="flex items-center gap-2 shrink-0">
              <span className="text-xs font-medium text-green-700">인증 완료</span>
              <span className="text-gray-300">·</span>
              <button
                type="button"
                onClick={() => { setStep(STEP.EMAIL); sendMutation.reset(); signupMutation.reset(); }}
                className="text-xs text-gray-400 underline hover:text-gray-600 transition-colors"
              >
                재인증
              </button>
            </div>
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (!PASSWORD_REGEX.test(password)) {
                setPasswordError(PASSWORD_MESSAGE);
                return;
              }
              if (password !== confirmPassword) {
                setPasswordError("비밀번호가 일치하지 않습니다.");
                return;
              }
              setPasswordError(null);
              signupMutation.mutate({ email, password, nickname, mbtiId });
            }}
            className="space-y-4"
          >
            <FloatingLabelInput
              id="signup-nickname"
              label="닉네임 (2~50자)"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              onClear={() => setNickname("")}
              required
              minLength={2}
              maxLength={50}
              disabled={signupMutation.isPending}
              error={getApiErrorStatus(signupMutation.error) === 409}
            />
            <PasswordInput
              id="signup-password"
              label="비밀번호 (영문+숫자 10자 이상)"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setPasswordError(null); }}
              required
              autoComplete="new-password"
              disabled={signupMutation.isPending}
              error={!!passwordError}
            />
            <PasswordInput
              id="signup-confirm-password"
              label="비밀번호 확인"
              value={confirmPassword}
              onChange={(e) => { setConfirmPassword(e.target.value); setPasswordError(null); }}
              required
              autoComplete="new-password"
              disabled={signupMutation.isPending}
              error={!!passwordError}
            />

            <LabeledFilterBox label="MBTI">
              <MbtiSelect
                value={mbtiId}
                onChange={setMbtiId}
                mbtiList={mbtiList}
                disabled={signupMutation.isPending}
                className="border-0 rounded-none flex-1"
              />
            </LabeledFilterBox>

            {signupErrorMsg && <p className="text-xs text-red-500">{signupErrorMsg}</p>}
            <button
              type="submit"
              disabled={signupMutation.isPending}
              className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
            >
              {signupMutation.isPending ? "가입 중..." : "회원가입 완료"}
            </button>
          </form>
        </article>
      </section>
    );
  }

  return (
    <section className="max-w-md mx-auto text-center">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">가입 완료!</h1>
      <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <p className="text-sm text-gray-700">
          회원가입이 완료되었습니다. 로그인 후 서비스를 이용하세요.
        </p>
        <Link
          href={ROUTES.login}
          className="block w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 transition-colors"
        >
          로그인하기
        </Link>
      </article>
    </section>
  );
}
