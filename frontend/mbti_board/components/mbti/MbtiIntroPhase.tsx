import Image from "next/image";
import { LuSnail, LuClipboardPenLine, LuClock } from "react-icons/lu";

interface Props {
  totalQuestions: number;
  onStart: () => void;
}

export function MbtiIntroPhase({ totalQuestions, onStart }: Props) {
  return (
    <section className="flex flex-col items-center text-center">
      <div className="w-full rounded-2xl border border-border bg-card p-10 mb-6 shadow-sm">
        <h2 className="text-2xl font-bold text-card-foreground mb-6">MBTI 성격유형 검사</h2>
        <div className="relative flex items-center justify-center mb-6">
          <div className="absolute inset-x-0 top-1/2 -translate-y-1/2 border-t border-border" />
          <span className="relative bg-card px-3 text-muted-foreground">
            <LuSnail className="text-2xl" />
          </span>
        </div>
        <p className="text-sm text-muted-foreground leading-relaxed mb-6">
          총 {totalQuestions}문항으로 나의 MBTI 성격 유형을 알아보세요.<br />
          각 질문에 솔직하게 답할수록 정확한 결과를 얻을 수 있습니다.
        </p>
        <div className="flex flex-col items-center gap-4">
          <div className="inline-flex items-center gap-4 rounded-full border border-border bg-muted px-5 py-2 text-sm text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <LuClipboardPenLine className="text-base" />
              총 {totalQuestions}문항
            </span>
            <span>·</span>
            <span className="flex items-center gap-1.5">
              <LuClock className="text-base" />
              5분 소요
            </span>
          </div>

          <div className="relative w-full flex justify-center m-6">
            <button
              onClick={onStart}
              className="rounded-xl bg-primary-container absolute -bottom-10 px-6 py-3.5 text-sm font-medium text-primary-foreground hover:bg-primary/80 transition-colors"
            >
              MBTI 검사하기
            </button>
            <Image
              src="/images/mbti/elements/animals_backhome.png"
              alt=""
              aria-hidden="true"
              width={160}
              height={54}
              style={{ height: "auto" }}
              className="select-none absolute -bottom-12 -right-4"
              draggable={false}
            />
          </div>
        </div>
      </div>
    </section>
  );
}
