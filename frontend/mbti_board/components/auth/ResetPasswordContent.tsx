"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/components/common/Toast";
import { confirmPasswordReset } from "@/lib/api/auth";
import { ROUTES } from "@/lib/routes";
import type { ApiError } from "@/types/api";
import { PASSWORD_REGEX, PASSWORD_MESSAGE } from "@/lib/validationConstants";

interface Props {
  token: string | null;
}

export default function ResetPasswordContent({ token }: Props) {
  const router = useRouter();
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  
  // 검증 실패(400) vs 서버 오류를 구분하기 위함:
  // - tokenInvalid=true 이면 전체 폼 숨김 (검증 링크가 만료)
  // - retryError 이면 메시지만, 재시도 가능 (검증은 아직 유효할 수 있음)
  const [tokenInvalid, setTokenInvalid] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      router.replace(ROUTES.accountFind);
    }
  }, [token, router]);

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

  if (!token) {
    return null;
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setValidationError(null);
    setRetryError(null);

    if (!PASSWORD_REGEX.test(newPassword)) {
      setValidationError(PASSWORD_MESSAGE);
      return;
    }
    if (newPassword !== confirmPassword) {
      setValidationError("비밀번호가 일치하지 않습니다.");
      return;
    }

    confirmMutation.mutate({ token, newPassword });
  };

  return (
    <section className="max-w-md mx-auto w-full">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">비밀번호 재설정</h1>

      <article className="bg-white rounded-xl border border-gray-200 p-6">
        {tokenInvalid ? (
          <aside className="space-y-4 text-center py-2">
            <p className="text-sm text-red-500">재설정 링크가 만료되었거나 유효하지 않습니다.</p>
            <Link
              href={ROUTES.accountFind + "?tab=password"}
              className="inline-block text-sm text-gray-700 underline hover:text-gray-900 transition-colors"
            >
              다시 요청하기
            </Link>
          </aside>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label
                htmlFor="new-password"
                className="block text-xs text-gray-400 uppercase tracking-wide mb-1"
              >
                새 비밀번호
              </label>
              <input
                id="new-password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                autoComplete="new-password"
                placeholder="영문+숫자 10자 이상 입력"
                disabled={confirmMutation.isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>

            <div>
              <label
                htmlFor="confirm-password"
                className="block text-xs text-gray-400 uppercase tracking-wide mb-1"
              >
                비밀번호 확인
              </label>
              <input
                id="confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                autoComplete="new-password"
                placeholder="비밀번호를 다시 입력"
                disabled={confirmMutation.isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>

            {validationError && (
              <p className="text-xs text-red-500">{validationError}</p>
            )}
            {retryError && (
              <p className="text-xs text-red-500">{retryError}</p>
            )}

            <button
              type="submit"
              disabled={confirmMutation.isPending}
              className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
            >
              {confirmMutation.isPending ? "변경 중..." : "비밀번호 변경"}
            </button>
          </form>
        )}
      </article>

      <p className="mt-4 text-center text-xs text-gray-400">
        <Link href={ROUTES.login} className="hover:text-gray-600 transition-colors">
          로그인으로 돌아가기
        </Link>
      </p>
    </section>
  );
}
