"use client";

import { useState } from "react";
import { MdAccountCircle } from "react-icons/md";
import { HiArrowRight } from "react-icons/hi";
import AuthCardHeader from "@/components/auth/AuthCardHeader";
import FloatingLabelInput from "@/components/common/FloatingLabelInput";
import PasswordInput from "@/components/common/PasswordInput";
import MbtiSelect from "@/components/common/MbtiSelect";
import LabeledFilterBox from "@/components/common/LabeledFilterBox";
import { MBTI_ID } from "@/types/mbti";
import { PASSWORD_REGEX, PASSWORD_MESSAGE, PHONE_REGEX, PHONE_MESSAGE } from "@/lib/validationConstants";
import { usePhoneInput } from "@/hooks/usePhoneInput";
import ErrorMessage from "@/components/common/ErrorMessage";
import TermsAgreementCheckboxes from "@/components/auth/TermsAgreementCheckboxes";
import { toast } from "@/components/common/Toast";

interface Props {
  email: string;
  onReauth: () => void;
  hasAuthError: boolean;
  onSubmit: (data: { email: string; password: string; nickname: string; mbtiId: string; phone?: string; termsAgreed: boolean; privacyAgreed: boolean }) => void;
  isPending: boolean;
  apiErrorMsg: string | null;
  isDuplicateNickname: boolean;
  isDuplicatePhone: boolean;
}

export default function FormStep({ email, onReauth, hasAuthError, onSubmit, isPending, apiErrorMsg, isDuplicateNickname, isDuplicatePhone }: Props) {
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [mbtiId, setMbtiId] = useState<string>(MBTI_ID.NONE);
  const { phone, setPhone, handlePhoneChange } = usePhoneInput();
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);

  const errorMsg = passwordError ?? apiErrorMsg;

  return (
    <section className="max-w-120 mx-auto isolate relative">
      <article
        className="bg-surface-container-lowest rounded-[1.5rem] border border-surface-variant overflow-hidden relative"
        style={{ boxShadow: "0 20px 40px -15px rgba(49, 23, 10, 0.08)" }}
      >
        <div className="absolute top-0 right-0 w-32 h-32 bg-secondary opacity-[0.03] rounded-full -mr-16 -mt-16 pointer-events-none" />
        <AuthCardHeader
          icon={<MdAccountCircle className="text-primary text-3xl" />}
          step={{ current: 3, total: 3 }}
          title="나만의 프로필 완성하기"
          description="프로필을 완성하고 다양한 MBTI 유형의 사람들을 만나보세요."
        />
        <div className="p-8 md:p-10 relative z-10">
          <div className={`flex items-center justify-between rounded-lg px-3 py-2.5 gap-2 border mb-6 ${hasAuthError ? "bg-red-50 border-red-200" : "bg-surface-container-low border-outline-variant"}`}>
            <span className="text-xs text-on-surface-variant">{email}</span>
            <div className="flex items-center gap-2 shrink-0">
              <span className="text-xs font-medium text-secondary">인증 완료</span>
              <span className="text-outline-variant">·</span>
              <button
                type="button"
                onClick={onReauth}
                className="text-xs text-on-surface-variant underline hover:text-primary transition-colors"
              >
                재인증
              </button>
            </div>
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (!PASSWORD_REGEX.test(password)) {
                setPasswordError(PASSWORD_MESSAGE);
                return;
              }
              if (password !== confirmPassword) {
                setPasswordError("비밀번호가 일치하지 않습니다.");
                return;
              }
              if (phone && !PHONE_REGEX.test(phone)) {
                setPhoneError(PHONE_MESSAGE);
                return;
              }
              setPasswordError(null);
              setPhoneError(null);
              if (!termsAgreed || !privacyAgreed) {
                toast.warning("약관 동의를 읽고 체크해주세요.");
                return;
              }
              onSubmit({ email, password, nickname, mbtiId, phone: phone ? phone.replace(/-/g, "") : undefined, termsAgreed, privacyAgreed });
            }}
            className="space-y-4"
          >
            <FloatingLabelInput
              id="signup-nickname"
              label="닉네임 (2~12자, 한글·영문·숫자·_)"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              onClear={() => setNickname("")}
              required
              minLength={2}
              maxLength={12}
              disabled={isPending}
              error={isDuplicateNickname}
            />
            <PasswordInput
              id="signup-password"
              label="비밀번호 (영문+숫자 10자 이상)"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setPasswordError(null); }}
              required
              autoComplete="new-password"
              disabled={isPending}
              error={!!passwordError}
            />
            <PasswordInput
              id="signup-confirm-password"
              label="비밀번호 확인"
              value={confirmPassword}
              onChange={(e) => { setConfirmPassword(e.target.value); setPasswordError(null); }}
              required
              autoComplete="new-password"
              disabled={isPending}
              error={!!passwordError}
            />
            <LabeledFilterBox label="MBTI">
              <MbtiSelect
                value={mbtiId}
                onChange={(v) => setMbtiId(v ?? MBTI_ID.NONE)}
                disabled={isPending}
                className="border-0 rounded-none flex-1"
              />
            </LabeledFilterBox>
            <div className="space-y-1.5">
              <FloatingLabelInput
                id="signup-phone"
                label="전화번호 (선택)"
                type="text"
                value={phone}
                onChange={(e) => { handlePhoneChange(e); setPhoneError(null); }}
                onClear={() => { setPhone(""); setPhoneError(null); }}
                disabled={isPending}
                hint="010-1234-5678"
                error={!!phoneError || isDuplicatePhone}
              />
              <p className="text-xs text-on-surface-variant px-1">
                이메일을 잊어버렸을 경우 <strong className="text-on-surface">이메일 찾기</strong>에 사용됩니다. 입력하지 않아도 됩니다.
              </p>
              <ErrorMessage message={phoneError} />
            </div>
            <ErrorMessage message={errorMsg} />
            <TermsAgreementCheckboxes
              className="pt-1"
              termsAgreed={termsAgreed}
              onTermsAgreedChange={setTermsAgreed}
              privacyAgreed={privacyAgreed}
              onPrivacyAgreedChange={setPrivacyAgreed}
              disabled={isPending}
            />
            <div className="pt-2">
              <button
                type="submit"
                disabled={isPending}
                className="w-full h-12 bg-primary text-primary-foreground text-sm font-semibold rounded-md hover:opacity-80 active:scale-[0.98] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
              >
                {isPending ? (
                  <>
                    <span className="w-4 h-4 border-2 border-on-primary/30 border-t-on-primary rounded-full animate-spin" />
                    가입 중...
                  </>
                ) : (
                  <>
                    회원가입 완료
                    <HiArrowRight className="text-lg group-hover:translate-x-1 transition-transform" />
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </article>
    </section>
  );
}
