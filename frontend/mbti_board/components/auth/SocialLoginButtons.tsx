"use client";

import SocialLoginButton from "@/components/auth/SocialLoginButton";
import { LOGIN_METHOD, type LoginMethod } from "@/lib/loginUtils";

interface Props {
  redirectTo: string;
  lastUsedMethod?: LoginMethod | null;
}

export default function SocialLoginButtons({ redirectTo, lastUsedMethod }: Props) {
  return (
    <nav className="space-y-2">
      <SocialLoginButton
        provider="google"
        redirectTo={redirectTo}
        isLastUsed={lastUsedMethod === LOGIN_METHOD.GOOGLE}
      />
      {/* <SocialLoginButton
        provider="kakao"
        redirectTo={redirectTo}
        isLastUsed={lastUsedMethod === LOGIN_METHOD.KAKAO}
      /> */}
    </nav>
  );
}
