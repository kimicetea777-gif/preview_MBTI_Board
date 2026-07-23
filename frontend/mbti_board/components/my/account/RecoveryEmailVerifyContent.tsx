"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { confirmRecoveryEmailVerification } from "@/lib/api/user";
import { userKeys } from "@/hooks/useUser";
import { ROUTES } from "@/constants/routes";

interface Props {
  token: string | null;
}

export default function RecoveryEmailVerifyContent({ token }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [status, setStatus] = useState<"loading" | "success" | "error">(
    token ? "loading" : "error"
  );
  const [errorMsg, setErrorMsg] = useState<string | null>(
    token ? null : "유효하지 않은 인증 링크입니다."
  );

  useEffect(() => {
    if (!token) return;

    confirmRecoveryEmailVerification({ token })
      .then(() => {
        queryClient.invalidateQueries({ queryKey: userKeys.myProfile() });
        setStatus("success");
        setTimeout(() => router.push(ROUTES.myAccount), 1500);
      })
      .catch((err: unknown) => {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
        setErrorMsg(msg ?? "인증 링크가 만료되었거나 유효하지 않습니다.");
        setStatus("error");
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (status === "loading") {
    return (
      <section className="max-w-md mx-auto text-center py-16">
        <p className="text-sm text-gray-500">복구 이메일 인증 확인 중...</p>
      </section>
    );
  }

  if (status === "success") {
    return (
      <section className="max-w-md mx-auto text-center py-16">
        <p className="text-sm font-medium text-green-700">복구 이메일 인증이 완료되었습니다!</p>
        <p className="text-xs text-gray-400 mt-1">마이페이지로 이동합니다...</p>
      </section>
    );
  }

  return (
    <section className="max-w-md mx-auto text-center py-16">
      <p className="text-sm font-medium text-red-600">{errorMsg}</p>
      <button
        onClick={() => router.push(ROUTES.myAccount)}
        className="mt-4 text-xs text-gray-500 underline"
      >
        마이페이지로 돌아가기
      </button>
    </section>
  );
}
