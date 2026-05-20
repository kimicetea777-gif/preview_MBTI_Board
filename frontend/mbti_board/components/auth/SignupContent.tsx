"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchMbtiList } from "@/lib/api/mbti";
import { sendEmailVerification, signup } from "@/lib/api/auth";
import { ROUTES } from "@/lib/routes";

import { STEP, type Step } from "@/app/(auth)/signup/steps";


const EV_EMAIL_KEY = "ev_email";

interface ApiError {
  response?: { data?: { message?: string } };
}

const getErrorMsg = (error: unknown, fallback: string) =>
  (error as ApiError)?.response?.data?.message ?? fallback;

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

  const sendErrorMsg = sendMutation.isError
    ? getErrorMsg(sendMutation.error, "이메일 발송에 실패했습니다. 다시 시도해 주세요.")
    : null;

  const signupErrorMsg =
    passwordError ??
    (signupMutation.isError
      ? getErrorMsg(signupMutation.error, "회원가입에 실패했습니다. 이메일 인증을 완료했는지 확인해 주세요.")
      : null);

  if (step === STEP.EMAIL) {
    return (
      <section className="max-w-md mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">회원가입</h1>
        <p className="text-xs text-gray-400 mb-6">Step 1 — 이메일 인증</p>
        <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
          <form
            onSubmit={(e) => { e.preventDefault(); sendMutation.mutate({ email }); }}
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
                placeholder="example@email.com"
                autoComplete="email"
                disabled={sendMutation.isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>
            {sendErrorMsg && <p className="text-xs text-red-500">{sendErrorMsg}</p>}
            <button
              type="submit"
              disabled={sendMutation.isPending}
              className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
            >
              {sendMutation.isPending ? "발송 중..." : "인증 메일 발송"}
            </button>
          </form>
          <p className="text-center text-xs text-gray-500">
            이미 계정이 있으신가요?{" "}
            <Link href={ROUTES.login} className="text-gray-900 underline">
              로그인
            </Link>
          </p>
        </article>
      </section>
    );
  }

  if (step === STEP.EMAIL_SENT) {
    return (
      <section className="max-w-md mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">회원가입</h1>
        <p className="text-xs text-gray-400 mb-6">Step 1 — 이메일 인증</p>
        <article className="bg-white rounded-xl border border-gray-200 p-6 text-center space-y-3">
          <p className="text-2xl">📬</p>
          <p className="text-sm font-medium text-gray-900">인증 메일을 발송했습니다</p>
          <p className="text-xs text-gray-500">
            <strong>{email}</strong>로 발송된 링크를 클릭하면 다음 단계로 이동합니다.
            <br />
            링크는 10분간 유효합니다.
          </p>
          <button
            onClick={() => { setStep(STEP.EMAIL); sendMutation.reset(); }}
            className="text-xs text-gray-400 underline"
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
        <p className="text-xs text-gray-400 mb-6">Step 2 — 회원 정보 입력</p>
        <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
          <div className="flex items-center justify-between rounded-lg bg-green-50 border border-green-200 px-3 py-2 gap-2">
            <span className="text-xs text-gray-600">{email}</span>
            <span className="text-xs font-medium text-green-700">인증 완료</span>
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (password !== confirmPassword) {
                setPasswordError("비밀번호가 일치하지 않습니다.");
                return;
              }
              setPasswordError(null);
              signupMutation.mutate({ email, password, nickname, mbtiId });
            }}
            className="space-y-4"
          >
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                닉네임
              </label>
              <input
                type="text"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                required
                minLength={2}
                maxLength={50}
                placeholder="닉네임 (2~50자)"
                disabled={signupMutation.isPending}
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
                onChange={(e) => { setPassword(e.target.value); setPasswordError(null); }}
                required
                minLength={8}
                placeholder="8자 이상"
                autoComplete="new-password"
                disabled={signupMutation.isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                비밀번호 확인
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => { setConfirmPassword(e.target.value); setPasswordError(null); }}
                required
                placeholder="비밀번호를 다시 입력하세요"
                autoComplete="new-password"
                disabled={signupMutation.isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                MBTI
              </label>
              <select
                value={mbtiId}
                onChange={(e) => setMbtiId(e.target.value)}
                disabled={signupMutation.isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              >
                {mbtiList.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.id === "UNKNOWN" ? "미설정" : m.id}
                  </option>
                ))}
              </select>
            </div>
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
