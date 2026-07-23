"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { requestPasswordReset } from "@/lib/api/auth";
import ErrorMessage from "@/components/common/ErrorMessage";
import InfoBanner from "@/components/common/InfoBanner";
import { MdEmail, MdSend } from "react-icons/md";

type PasswordStep = 1 | 2;

interface Props {
  onPasswordSent: () => void;
}

export default function FindPasswordSection({ onPasswordSent }: Props) {
  const [email, setEmail] = useState("");
  const [passwordStep, setPasswordStep] = useState<PasswordStep>(1);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const requestResetMutation = useMutation({
    mutationFn: requestPasswordReset,
    onSuccess: () => {
      setPasswordStep(2);
      setPasswordError(null);
      onPasswordSent();
    },
    onError: () => {
      setPasswordError("오류가 발생했습니다. 다시 시도해 주세요.");
    },
  });

  return (
    <section>
      {passwordStep === 1 ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setPasswordError(null);
            requestResetMutation.mutate({ email });
          }}
          className="space-y-6"
        >
          <div className="space-y-2">
            <label htmlFor="find-password-email" className="block text-sm font-semibold text-primary">
              이메일
            </label>
            <div className="relative">
              <MdEmail className="absolute left-4 top-1/2 -translate-y-1/2 text-outline text-xl pointer-events-none" />
              <input
                id="find-password-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                placeholder="가입 시 사용한 이메일"
                disabled={requestResetMutation.isPending}
                className="w-full h-12 pl-11 pr-4 rounded-lg border border-outline-variant bg-background text-on-surface placeholder:text-outline/60 focus:outline-none focus:ring-2 focus:ring-secondary/40 focus:border-secondary disabled:opacity-50 transition-all text-sm"
              />
            </div>
          </div>
          <ErrorMessage message={passwordError} />
          <button
            type="submit"
            disabled={requestResetMutation.isPending}
            className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-90 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
          >
            {requestResetMutation.isPending ? (
              "발송 중..."
            ) : (
              <>재설정 링크 발송 <MdSend className="text-lg" /></>
            )}
          </button>
        </form>
      ) : (
        <aside className="text-center py-2 space-y-4">
          <InfoBanner
            variant="info"
            text="소셜 로그인 사용자는 비밀번호 재설정 이메일이 전송되지 않습니다. 소셜 로그인을 이용해 주세요."
          />
          <button
            type="button"
            onClick={() => {
              setPasswordStep(1);
              requestResetMutation.reset();
            }}
            className="text-xs text-muted-foreground underline underline-offset-4 hover:text-on-surface-variant transition-colors"
          >
            · 다시 받기 ·
          </button>
        </aside>
      )}
    </section>
  );
}
