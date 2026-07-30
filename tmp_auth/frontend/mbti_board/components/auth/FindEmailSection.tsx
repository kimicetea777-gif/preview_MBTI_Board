"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { requestFindEmailOtp, confirmFindEmail } from "@/lib/api/auth";
import { EMAIL_REGEX, EMAIL_MESSAGE } from "@/lib/validationConstants";
import type { FindEmailResponse } from "@/types/auth";
import type { ApiError } from "@/types/api";
import { MdEmail, MdPersonSearch } from "react-icons/md";
import ErrorMessage from "@/components/common/ErrorMessage";

const FIND_EMAIL_STEP = { EMAIL: "email", OTP: "otp" } as const;
type FindEmailStep = typeof FIND_EMAIL_STEP[keyof typeof FIND_EMAIL_STEP];

const getRequestErrorMessage = (err: ApiError) => {
  const status = err.response?.status;
  if (status === 404) return "해당 정보로 등록된 계정이 없습니다.";
  if (status === 400) return EMAIL_MESSAGE;
  if (status === 429) return "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";
  return "오류가 발생했습니다. 다시 시도해 주세요.";
};

const getOtpErrorMessage = (err: ApiError) =>
  err.response?.data?.message ?? "인증 코드 확인에 실패했습니다.";

export default function FindEmailSection() {
  const [step, setStep] = useState<FindEmailStep>(FIND_EMAIL_STEP.EMAIL);
  const [recoveryEmail, setRecoveryEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [otpError, setOtpError] = useState<string | null>(null);
  const [emailResult, setEmailResult] = useState<FindEmailResponse | null>(null);
  const [requestError, setRequestError] = useState<string | null>(null);

  const requestOtpMutation = useMutation({
    mutationFn: requestFindEmailOtp,
    onSuccess: () => {
      setRequestError(null);
      setOtp("");
      setOtpError(null);
      setStep(FIND_EMAIL_STEP.OTP);
    },
    onError: (err: ApiError) => setRequestError(getRequestErrorMessage(err)),
  });

  const confirmMutation = useMutation({
    mutationFn: () => confirmFindEmail({ recoveryEmail, otp }),
    onSuccess: (data) => {
      setEmailResult(data);
      setOtpError(null);
    },
    onError: (err: ApiError) => setOtpError(getOtpErrorMessage(err)),
  });

  return (
    <section className="space-y-6">
      {step === FIND_EMAIL_STEP.EMAIL ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (!EMAIL_REGEX.test(recoveryEmail)) {
              setRequestError(EMAIL_MESSAGE);
              return;
            }
            setRequestError(null);
            requestOtpMutation.mutate({ recoveryEmail });
          }}
          className="space-y-6"
        >
          {/* 복구 이메일 입력창 */}
          <div className="space-y-2">
            <label htmlFor="find-email-recovery-email" className="block text-sm font-semibold text-primary">
              복구 이메일
            </label>
            <div className="relative">
              <MdEmail className="absolute left-4 top-1/2 -translate-y-1/2 text-outline text-xl pointer-events-none" />
              <input
                id="find-email-recovery-email"
                type="email"
                value={recoveryEmail}
                onChange={(e) => setRecoveryEmail(e.target.value)}
                required
                placeholder="recovery@example.com"
                disabled={requestOtpMutation.isPending}
                className="w-full h-12 pl-11 pr-4 rounded-lg border border-outline-variant bg-background text-on-surface placeholder:text-outline/60 focus:outline-none focus:ring-2 focus:ring-secondary/40 focus:border-secondary disabled:opacity-50 transition-all text-sm"
              />
            </div>
          </div>

          {/* 인증 코드 발송 버튼 */}
          <button
            type="submit"
            disabled={requestOtpMutation.isPending}
            className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
          >
            {requestOtpMutation.isPending ? (
              <div className="flex items-center gap-2">
                <span className="w-4 h-4 border-2 border-t-transparent border-white rounded-md animate-spin" />
                발송 중...
              </div>
            ) : (
              <>인증 코드 발송 <MdPersonSearch className="text-lg" /></>
            )}
          </button>

          <ErrorMessage message={requestError} />
        </form>
      ) : (
        <div className="space-y-6">
          <div className="space-y-2">
            <label htmlFor="find-email-otp" className="block text-sm font-semibold text-primary">
              인증 코드
            </label>
            <p className="text-xs text-muted-foreground">{recoveryEmail}로 전송된 6자리 코드를 입력해 주세요.</p>
            <input
              id="find-email-otp"
              type="text"
              value={otp}
              onChange={(e) => {
                setOtp(e.target.value.replace(/\D/g, "").slice(0, 6));
                setOtpError(null);
              }}
              placeholder="6자리 인증 코드"
              maxLength={6}
              inputMode="numeric"
              disabled={confirmMutation.isPending || !!emailResult}
              className="w-full h-12 px-4 rounded-lg border border-outline-variant bg-background text-center text-lg tracking-widest focus:outline-none focus:ring-2 focus:ring-secondary/40 focus:border-secondary disabled:opacity-50 transition-all"
            />
            <button
              type="button"
              onClick={() => requestOtpMutation.mutate({ recoveryEmail })}
              disabled={requestOtpMutation.isPending || !!emailResult}
              className="text-xs text-muted-foreground hover:text-foreground underline disabled:no-underline disabled:opacity-50"
            >
              {requestOtpMutation.isPending ? "재전송 중..." : "인증 코드 재전송"}
            </button>
          </div>

          {!emailResult && (
            <button
              type="button"
              onClick={() => confirmMutation.mutate()}
              disabled={confirmMutation.isPending || otp.length < 6}
              className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              {confirmMutation.isPending ? "확인 중..." : "확인"}
            </button>
          )}

          <ErrorMessage message={otpError} />
        </div>
      )}

      {/* 가입된 이메일 확인 */}
      {emailResult && (
        <aside
          className="rounded-xl border border-outline-variant bg-muted p-4 space-y-1"
          aria-live="polite"
        >
          <p className="text-xs text-muted-foreground">가입된 이메일</p>
          <p className="text-sm font-semibold text-foreground">{emailResult.maskedEmail}</p>
          {emailResult.isSocialOnly && (
            <p className="text-xs text-muted-foreground mt-1">소셜 로그인으로 가입된 계정입니다.</p>
          )}
        </aside>
      )}
    </section>
  );
}
