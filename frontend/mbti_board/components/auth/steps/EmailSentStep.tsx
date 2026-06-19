"use client";

import AuthCardHeader from "@/components/auth/AuthCardHeader";
import SenderEmailNotice from "@/components/auth/SenderEmailNotice";
import { HiArrowLeft, HiRefresh, HiInformationCircle, HiMail } from "react-icons/hi";

interface Props {
  email: string;
  countdown: number;
  isPending: boolean;
  onResend: () => void;
  onBack: () => void;
}

export default function EmailSentStep({ email, countdown, isPending, onResend, onBack }: Props) {
  return (
    <section className="max-w-120 mx-auto isolate relative">
      <article
        className="bg-surface-container-lowest rounded-[1.5rem] border border-surface-variant overflow-hidden relative"
        style={{ boxShadow: "0 20px 40px -15px rgba(49, 23, 10, 0.08)" }}
      >
        <AuthCardHeader
          icon={<HiMail className="text-secondary text-3xl" />}
          step={{ current: 2, total: 3 }}
          title="이메일을 확인해주세요"
          description={<><strong className="text-on-surface">{email}</strong>로 인증 링크를 발송했어요.<br />링크를 클릭하면 다음 단계로 이동합니다.</>}
        />
        <div className="p-8 md:p-10 text-center">
          <div className="bg-surface-container-low p-4 rounded-xl border border-outline-variant/40 mb-6 text-left space-y-2">
            <p className="text-sm text-on-surface-variant flex items-start gap-2">
              <HiInformationCircle className="text-secondary text-lg mt-0.5 shrink-0" />
              <span className="whitespace-pre-line">
                {"메일이 안 보이면 스팸함을 확인하거나,\n"}
                {countdown > 0
                  ? <><strong className="text-primary">{countdown}초</strong> 후 재발송할 수 있어요.</>
                  : <>재발송 버튼을 눌러주세요.</>
                }
              </span>
            </p>
            <SenderEmailNotice />
          </div>
          <div className="flex flex-col gap-3">
            <button
              type="button"
              disabled={countdown > 0 || isPending}
              onClick={onResend}
              className="w-full h-12 border border-primary text-primary text-sm font-semibold rounded-md hover:bg-primary/5 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
            >
              <HiRefresh className={`text-lg ${isPending ? "animate-spin" : ""}`} />
              {isPending ? "발송 중..." : countdown > 0 ? `재발송 (${countdown}초)` : "인증 메일 재발송"}
            </button>
            <button
              type="button"
              onClick={onBack}
              className="inline-flex items-center justify-center gap-2 text-sm text-on-surface-variant hover:text-primary transition-colors py-2"
            >
              <HiArrowLeft className="text-lg" />
              이메일 다시 입력하기
            </button>
          </div>
        </div>
      </article>
    </section>
  );
}
