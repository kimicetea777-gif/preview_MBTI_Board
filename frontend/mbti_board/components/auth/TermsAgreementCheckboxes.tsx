"use client";

import Link from "next/link";
import { ROUTES } from "@/constants/routes";

interface Props {
  termsAgreed: boolean;
  onTermsAgreedChange: (checked: boolean) => void;
  privacyAgreed: boolean;
  onPrivacyAgreedChange: (checked: boolean) => void;
  disabled?: boolean;
  className?: string;
}

/** 이용약관/개인정보처리방침 동의 체크박스 쌍 — 이메일 회원가입·소셜 약관 동의·비회원 글쓰기에서 공유 */
export default function TermsAgreementCheckboxes({
  termsAgreed,
  onTermsAgreedChange,
  privacyAgreed,
  onPrivacyAgreedChange,
  disabled,
  className,
}: Props) {
  return (
    <div className={`space-y-2 ${className ?? ""}`}>
      <label className="flex items-start gap-2.5 cursor-pointer">
        <input
          type="checkbox"
          checked={termsAgreed}
          onChange={(e) => onTermsAgreedChange(e.target.checked)}
          disabled={disabled}
          className="mt-0.5 h-4 w-4 shrink-0 accent-primary"
        />
        <span className="text-xs text-on-surface-variant leading-relaxed">
          <Link href={ROUTES.terms} target="_blank" className="font-medium text-primary underline underline-offset-2 hover:opacity-80">이용약관</Link>에 동의합니다. <span className="text-red-500">*</span>
        </span>
      </label>
      <label className="flex items-start gap-2.5 cursor-pointer">
        <input
          type="checkbox"
          checked={privacyAgreed}
          onChange={(e) => onPrivacyAgreedChange(e.target.checked)}
          disabled={disabled}
          className="mt-0.5 h-4 w-4 shrink-0 accent-primary"
        />
        <span className="text-xs text-on-surface-variant leading-relaxed">
          <Link href={ROUTES.privacy} target="_blank" className="font-medium text-primary underline underline-offset-2 hover:opacity-80">개인정보처리방침</Link>에 동의합니다. <span className="text-red-500">*</span>
        </span>
      </label>
    </div>
  );
}
