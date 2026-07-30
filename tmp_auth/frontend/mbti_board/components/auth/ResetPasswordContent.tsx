"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/components/common/Toast";
import { confirmPasswordReset } from "@/lib/api/auth";
import { ROUTES } from "@/constants/routes";
import { FIND_ACCOUNT_TAB } from "@/types/auth";
import type { ApiError } from "@/types/api";
import { validatePassword } from "@/lib/validationConstants";
import { MdLockReset, MdLock, MdErrorOutline } from "react-icons/md";
import AuthCardHeader from "@/components/auth/AuthCardHeader";
import BackToLoginLink from "@/components/auth/BackToLoginLink";
import PasswordInput from "@/components/common/PasswordInput";
import SenderEmailNotice from "@/components/auth/SenderEmailNotice";

interface Props {
  token: string;
}

export default function ResetPasswordContent({ token }: Props) {
  const router = useRouter();
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  /*
   검증 실패(400) vs 서버 오류를 구분하기 위함:
   - tokenInvalid=true 이면 전체 폼 숨김 (검증 링크가 만료)
   - retryError 이면 메시지만, 재시도 가능 (검증은 아직 유효할 수 있음)
  */
  const [tokenInvalid, setTokenInvalid] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);

  const confirmMutation = useMutation({
    mutationFn: confirmPasswordReset,
    onSuccess: () => {
      toast.success("비밀번호가 변경되었습니다.");
      router.replace(ROUTES.login);
    },
    onError: (err: ApiError) => {
      if (err.response?.status === 400) {
        setTokenInvalid(true);
      } else {
        setRetryError("오류가 발생했습니다. 다시 시도해 주세요.");
      }
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setValidationError(null);
    setRetryError(null);

    const error = validatePassword(newPassword, confirmPassword);
    if (error) {
      setValidationError(error);
      return;
    }

    confirmMutation.mutate({ token, newPassword });
  };

  return (
    <div className="relative w-full max-w-120 isolate">
      {/* -------------- Card -------------- */}
      <article
        className="w-full bg-card border border-outline-variant rounded-[1.5rem] overflow-hidden"
        style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.12)" }}
      >
        <AuthCardHeader
          icon={tokenInvalid
            ? <MdErrorOutline className="text-primary text-3xl" />
            : <MdLockReset className="text-primary text-3xl" />
          }
          title={tokenInvalid ? "링크가 만료되었습니다." : "비밀번호 재설정"}
          description={tokenInvalid
            ? "재설정 링크가 만료되었거나 유효하지 않습니다."
            : "새로운 비밀번호를 입력해주세요."
          }
        />
        <div className="p-8 md:p-12">
        {/* -------------- (TOKEN 만료) -------------- */}
        {tokenInvalid ? (
          <aside className="text-center py-2">
            <Link
              href={`${ROUTES.accountFind}?tab=${FIND_ACCOUNT_TAB.PASSWORD}`}
              className="text-sm font-semibold text-primary hover:text-secondary transition-colors"
            >
              다시 요청하기
            </Link>
          </aside>
        ) : (
          //{/* -------------- (TOKEN 유효) NEW PASSWORD FORM -------------- */}
          <form onSubmit={handleSubmit} className="space-y-6">
            <PasswordInput
              id="new-password"
              label="새 비밀번호"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              icon={<MdLock />}
              required
              autoComplete="new-password"
              hint="영문+숫자 10자 이상 입력"
              disabled={confirmMutation.isPending}
              error={!!validationError}
            />

            <PasswordInput
              id="confirm-password"
              label="비밀번호 확인"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              icon={<MdLock />}
              required
              autoComplete="new-password"
              disabled={confirmMutation.isPending}
              error={!!validationError}
            />
            {/* -------------- SENDER NOTICE -------------- */}
            <SenderEmailNotice />
            {/* -------------- ERROR MESSAGE -------------- */}
            {(validationError || retryError) && (
              <p className="text-sm text-destructive" role="alert">
                {validationError ?? retryError}
              </p>
            )}
            {/* -------------- Submit Button -------------- */}
            <button
              type="submit"
              disabled={confirmMutation.isPending}
              className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              {confirmMutation.isPending ? (
                <div className="flex items-center gap-2">
                  <span className="w-4 h-4 border-2 border-t-transparent border-white rounded-full animate-spin" />
                  변경 중...
                </div>
              ) : (
                <>비밀번호 변경 <MdLockReset className="text-lg" /></>
              )}
            </button>
          </form>
        )}
        {/* -------------- Footer links -------------- */}
        <footer className="mt-8 pt-8 border-t border-outline-variant flex flex-col items-center">
          <BackToLoginLink />
        </footer>
      </div>
      </article>
    </div>
  );
}
