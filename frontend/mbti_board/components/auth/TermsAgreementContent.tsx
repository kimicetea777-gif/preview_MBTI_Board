"use client";

import { useState, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { confirmOAuth2Signup } from "@/lib/api/auth";
import { useAuthStore } from "@/store/authStore";
import { ROUTES } from "@/constants/routes";
import { toSafeRedirect } from "@/lib/safeRedirect";
import { setLastLoginMethod, PENDING_SOCIAL_PROVIDER_KEY, type LoginMethod } from "@/lib/loginUtils";
import { OAUTH2_REDIRECT_KEY } from "@/components/auth/OAuth2CallbackContent";
import { getApiErrorMessage } from "@/lib/apiError";
import TermsAgreementCheckboxes from "@/components/auth/TermsAgreementCheckboxes";
import { toast } from "@/components/common/Toast";

// subscribe는 모듈 레벨 상수 — 렌더마다 새 참조 생성 방지 (해시는 바뀌어도 갱신 안 함, 최초 1회 값만 필요)
const noopSubscribe = () => () => {};

/**
 * URL fragment(#signupToken=...)는 서버로 전송되지 않아 SSR 시점엔 알 수 없음 —
 * useSyncExternalStore로 서버에서는 undefined(미확정), 클라이언트 hydration 이후에만 실제 값을 읽음
 */
function useSignupTokenFromHash(): string | null | undefined {
  return useSyncExternalStore(
    noopSubscribe,
    () => new URLSearchParams(window.location.hash.slice(1)).get("signupToken"),
    () => undefined,
  );
}

/**
 * 소셜 로그인 신규 가입 약관 동의 페이지
 * 주의: 라우트/이름을 "소셜 가입" 대신 "약관 동의"로 범용화 — 향후 약관 버전이 올라가
 *       기존 유저 재동의가 필요해질 때도 같은 화면을 재사용하기 위함(이번 단계에서는 signupToken 있는
 *       신규 소셜 가입 완료 흐름만 구현, 재동의 분기는 범위 밖)
 */
export default function TermsAgreementContent() {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  const signupToken = useSignupTokenFromHash();
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);
  const [age14Agreed, setAge14Agreed] = useState(false);

  const mutation = useMutation({
    mutationFn: confirmOAuth2Signup,
    onSuccess: (data) => {
      // 소셜 가입도 체크박스가 없고 백엔드도 항상 유지 로그인으로 RT를 발급함
      setAccessToken(data.accessToken, true);

      const provider = sessionStorage.getItem(PENDING_SOCIAL_PROVIDER_KEY) as LoginMethod | null;
      if (provider) {
        setLastLoginMethod(provider);
        sessionStorage.removeItem(PENDING_SOCIAL_PROVIDER_KEY);
      }

      const raw = sessionStorage.getItem(OAUTH2_REDIRECT_KEY) ?? ROUTES.home;
      sessionStorage.removeItem(OAUTH2_REDIRECT_KEY);
      router.replace(toSafeRedirect(raw));
    },
  });

  const errorMsg = mutation.isError
    ? getApiErrorMessage(mutation.error, "가입 처리 중 오류가 발생했습니다.")
    : null;

  // undefined: 서버/hydration 이전(아직 미확정) — 폼을 잘못 보여주지 않도록 대기
  if (signupToken === undefined) {
    return (
      <section className="text-center py-16">
        <p className="text-sm text-on-surface-variant">확인 중...</p>
      </section>
    );
  }

  // null: hydration 후 확인 결과 실제로 signupToken이 없음
  if (signupToken === null) {
    return (
      <section className="max-w-md mx-auto text-center py-16 space-y-4">
        <p className="text-sm font-medium text-red-600">잘못된 접근입니다. 소셜 로그인을 다시 시도해 주세요.</p>
        <Link
          href={ROUTES.login}
          className="inline-block rounded-lg bg-gray-900 px-5 py-2 text-sm text-white hover:bg-gray-700 transition-colors"
        >
          로그인 페이지로
        </Link>
      </section>
    );
  }

  return (
    <section className="max-w-md mx-auto py-16 space-y-6">
      <div className="text-center space-y-2">
        <h1 className="text-xl font-bold text-foreground">약관 동의</h1>
        <p className="text-sm text-on-surface-variant">서비스 이용을 위해 아래 약관에 동의해 주세요.</p>
      </div>

      <TermsAgreementCheckboxes
        termsAgreed={termsAgreed}
        onTermsAgreedChange={setTermsAgreed}
        privacyAgreed={privacyAgreed}
        onPrivacyAgreedChange={setPrivacyAgreed}
        age14Agreed={age14Agreed}
        onAge14AgreedChange={setAge14Agreed}
        disabled={mutation.isPending}
      />

      {errorMsg && (
        <div className="text-center space-y-2">
          <p className="text-sm font-medium text-red-600">{errorMsg}</p>
          <Link href={ROUTES.login} className="text-xs text-on-surface-variant underline hover:text-primary transition-colors">
            로그인 페이지로 돌아가기
          </Link>
        </div>
      )}

      <button
        type="button"
        disabled={mutation.isPending}
        onClick={() => {
          if (!termsAgreed || !privacyAgreed || !age14Agreed) {
            toast.warning("약관 동의를 읽고 체크해주세요.");
            return;
          }
          mutation.mutate({ signupToken, termsAgreed, privacyAgreed, age14Agreed });
        }}
        className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all"
      >
        {mutation.isPending ? "처리 중..." : "동의하고 시작하기"}
      </button>
    </section>
  );
}
