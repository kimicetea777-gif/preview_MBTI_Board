"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { findEmail } from "@/lib/api/auth";
import { EMAIL_REGEX, EMAIL_MESSAGE } from "@/lib/validationConstants";
import type { FindEmailResponse } from "@/types/auth";
import type { ApiError } from "@/types/api";
import { MdEmail, MdPersonSearch } from "react-icons/md";
import ErrorMessage from "@/components/common/ErrorMessage";

const getEmailErrorMessage = (err: ApiError) => {
  const status = err.response?.status;
  if (status === 404) return "해당 정보로 등록된 계정이 없습니다.";
  if (status === 400) return EMAIL_MESSAGE;
  if (status === 429) return "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";
  return "오류가 발생했습니다. 다시 시도해 주세요.";
};

export default function FindEmailSection() {
  const [recoveryEmail, setRecoveryEmail] = useState("");
  const [emailResult, setEmailResult] = useState<FindEmailResponse | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);

  const findEmailMutation = useMutation({
    mutationFn: findEmail,
    onSuccess: (data) => {
      setEmailResult(data);
      setEmailError(null);
    },
    onError: (err: ApiError) => {
      setEmailResult(null);
      setEmailError(getEmailErrorMessage(err));
    },
  });

  return (
    <section className="space-y-6">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          setEmailResult(null);
          if (!EMAIL_REGEX.test(recoveryEmail)) {
            setEmailError(EMAIL_MESSAGE);
            return;
          }
          setEmailError(null);
          findEmailMutation.mutate({ recoveryEmail });
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
              disabled={findEmailMutation.isPending}
              className="w-full h-12 pl-11 pr-4 rounded-lg border border-outline-variant bg-background text-on-surface placeholder:text-outline/60 focus:outline-none focus:ring-2 focus:ring-secondary/40 focus:border-secondary disabled:opacity-50 transition-all text-sm"
            />
          </div>
        </div>

        {/* 이메일 찾기 버튼 */}
        <button
          type="submit"
          disabled={findEmailMutation.isPending}
          className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
        >
          {findEmailMutation.isPending ? (
            <div className="flex items-center gap-2">
              <span className="w-4 h-4 border-2 border-t-transparent border-white rounded-md animate-spin" />
              조회 중...
            </div>
          ) : (
            <>이메일 찾기 <MdPersonSearch className="text-lg" /></>
          )}
        </button>
      </form>

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

      {/* 에러 메세지 */}
      <ErrorMessage message={emailError} />
    </section>
  );
}
