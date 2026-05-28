"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { mbtiKeys, useMbtiList, useMbtiTestQuestions, useSubmitMbtiTest } from "@/hooks/useMbti";
import { useAuthStore } from "@/store/authStore";
import { ROUTES } from "@/lib/routes";
import { MBTI_PENDING_SCORES_KEY } from "@/types/mbti";
import type { MbtiTestScores } from "@/types/mbti";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

// sessionStorage에서 꺼낸 값의 shape·범위 검증 (TypeScript as 단언은 런타임 보장 없음)
function isValidScores(v: unknown): v is MbtiTestScores {
  if (!v || typeof v !== "object") return false;
  const s = v as Record<string, unknown>;
  return (
    typeof s.eiScore === "number" && s.eiScore >= -16 && s.eiScore <= 16 &&
    typeof s.snScore === "number" && s.snScore >= -16 && s.snScore <= 16 &&
    typeof s.tfScore === "number" && s.tfScore >= -16 && s.tfScore <= 16 &&
    typeof s.jpScore === "number" && s.jpScore >= -16 && s.jpScore <= 16
  );
}

// 선택지별 점수·색상 (mbti_quest_design.md §1 참고)
const ANSWER_OPTIONS = [
  {
    label: "매우 그렇다", score:  2,
    defaultCls:  "border-red-200    bg-red-50    text-red-700    hover:border-red-400    hover:bg-red-100",
    selectedCls: "border-red-500    bg-red-500   text-white",
  },
  {
    label: "그렇다",      score:  1,
    defaultCls:  "border-orange-200 bg-orange-50 text-orange-700 hover:border-orange-400 hover:bg-orange-100",
    selectedCls: "border-orange-400 bg-orange-400 text-white",
  },
  {
    label: "보통이다",    score:  0,
    defaultCls:  "border-gray-200   bg-white     text-gray-600   hover:border-gray-400  hover:bg-gray-50",
    selectedCls: "border-gray-500   bg-gray-500  text-white",
  },
  {
    label: "아니다",      score: -1,
    defaultCls:  "border-sky-200    bg-sky-50    text-sky-700    hover:border-sky-400   hover:bg-sky-100",
    selectedCls: "border-sky-500    bg-sky-500   text-white",
  },
  {
    label: "매우 아니다", score: -2,
    defaultCls:  "border-blue-200   bg-blue-50   text-blue-700   hover:border-blue-400  hover:bg-blue-100",
    selectedCls: "border-blue-500   bg-blue-500  text-white",
  },
] as const;

function computeScores(
  questions: Array<{ id: number; category: string; isReverse: boolean }>,
  answers: Record<number, number>
): MbtiTestScores {
  let eiScore = 0, snScore = 0, tfScore = 0, jpScore = 0;
  for (const q of questions) {
    const sel = answers[q.id];
    if (sel === undefined) continue;
    const delta = q.isReverse ? -sel : sel;
    if      (q.category === "EI") eiScore += delta;
    else if (q.category === "SN") snScore += delta;
    else if (q.category === "TF") tfScore += delta;
    else if (q.category === "JP") jpScore += delta;
  }
  return { eiScore, snScore, tfScore, jpScore };
}

function deriveMbtiId({ eiScore, snScore, tfScore, jpScore }: MbtiTestScores): string {
  return [
    eiScore >= 0 ? "E" : "I",
    snScore >= 0 ? "S" : "N",
    tfScore >= 0 ? "T" : "F",
    jpScore >= 0 ? "J" : "P",
  ].join("");
}

type Phase = "testing" | "result";

export default function MbtiTestContent() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const {
    mutate: submitScores,
    isPending: isSubmitting,
    isError: isSubmitError,
    reset: resetSubmit,
  } = useSubmitMbtiTest();

  const {
    data: questions,
    isPending: questionsLoading,
    isFetching: questionsFetching,
    isError: questionsError,
  } = useMbtiTestQuestions();
  const { data: mbtiList = [] } = useMbtiList();

  // sessionStorage pending 점수를 읽어 초기 phase·scores 결정 (SSR에서는 기본값)
  const [phase, setPhase] = useState<Phase>(() => {
    if (typeof window === "undefined") return "testing";
    const token = useAuthStore.getState().accessToken;
    if (!token) return "testing";
    return sessionStorage.getItem(MBTI_PENDING_SCORES_KEY) ? "result" : "testing";
  });
  const [scores, setScores] = useState<MbtiTestScores | null>(() => {
    if (typeof window === "undefined") return null;
    const token = useAuthStore.getState().accessToken;
    if (!token) return null;
    const raw = sessionStorage.getItem(MBTI_PENDING_SCORES_KEY);
    if (!raw) return null;
    try {
      const parsed: unknown = JSON.parse(raw);
      return isValidScores(parsed) ? parsed : null;
    } catch { return null; }
  });
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [saved, setSaved] = useState(false);

  const totalQuestions = questions?.length ?? 32;

  // 로그인 후 복귀 시: state는 lazy initializer로 이미 설정됨
  // 이 effect에서는 sessionStorage 정리 + 서버 제출(외부 시스템 동기화)만 수행
  useEffect(() => {
    const raw = sessionStorage.getItem(MBTI_PENDING_SCORES_KEY);
    if (!raw) return; // 일반 검사 완료 흐름 — 자동 제출 불필요

    let parsed: unknown;
    try {
      parsed = JSON.parse(raw);
    } catch {
      sessionStorage.removeItem(MBTI_PENDING_SCORES_KEY);
      return;
    }

    // 파싱 성공 시점에 단일 제거 — 이후 어느 분기든 정리 완료
    sessionStorage.removeItem(MBTI_PENDING_SCORES_KEY);
    if (!isValidScores(parsed)) return;

    submitScores(parsed, {
      onSuccess: () => {
        setSaved(true);
        queryClient.invalidateQueries({ queryKey: ["myProfile"] });
      },
    });
  // submitScores, queryClient는 안정적인 참조 — 마운트 1회만 실행
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const mbtiId = useMemo(() => (scores ? deriveMbtiId(scores) : null), [scores]);

  const mbtiMeta = useMemo(
    () => (mbtiId ? (mbtiList.find((m) => m.id === mbtiId) ?? null) : null),
    [mbtiId, mbtiList]
  );

  function handleAnswer(score: number) {
    if (!questions) return;
    const question = questions[currentIndex];
    const newAnswers = { ...answers, [question.id]: score };
    setAnswers(newAnswers);

    if (currentIndex < totalQuestions - 1) {
      setCurrentIndex((i) => i + 1);
    } else {
      const computed = computeScores(questions, newAnswers);
      setScores(computed);
      setPhase("result");
    }
  }

  function handleSaveConfirm() {
    if (!scores) return;
    const token = useAuthStore.getState().accessToken;
    if (!token) {
      // 비로그인: 점수 임시 저장 후 로그인 페이지로 이동, 복귀 시 자동 제출
      sessionStorage.setItem(MBTI_PENDING_SCORES_KEY, JSON.stringify(scores));
      router.push(`${ROUTES.login}?redirect=${encodeURIComponent(ROUTES.mbti.test)}`);
      return;
    }
    submitScores(scores, {
      onSuccess: () => {
        setSaved(true);
        setSaveDialogOpen(false);
        queryClient.invalidateQueries({ queryKey: ["myProfile"] });
      },
    });
  }

  function handleShare() {
    const text = mbtiId
      ? `나의 MBTI 유형은 ${mbtiId}입니다! — ${mbtiMeta?.summary ?? ""}`
      : "";
    if (navigator.share) {
      navigator.share({ title: "MBTI 검사 결과", text });
    } else {
      navigator.clipboard.writeText(text);
    }
  }

  function handleNext() {
    if (!questions) return;
    if (currentIndex < totalQuestions - 1) {
      setCurrentIndex((i) => i + 1);
    } else {
      const computed = computeScores(questions, answers);
      setScores(computed);
      setPhase("result");
    }
  }

  function handleRetry() {
    setPhase("testing");
    setCurrentIndex(0);
    setAnswers({});
    setScores(null);
    setSaved(false);
    setSaveDialogOpen(false);
    resetSubmit();
    // 재검사 시 새 문항 랜덤 추출을 보장하기 위해 캐시 무효화
    queryClient.invalidateQueries({ queryKey: mbtiKeys.questions() });
  }

  // 문항 로딩/에러는 testing phase에서만 체크 (result phase는 questions 불필요)
  if (phase === "testing" && (questionsLoading || questionsFetching)) {
    return (
      <div className="flex items-center justify-center py-20 text-sm text-gray-500">
        문항을 불러오는 중...
      </div>
    );
  }

  if (phase === "testing" && (questionsError || !questions)) {
    return (
      <div className="flex items-center justify-center py-20 text-sm text-red-500">
        문항을 불러올 수 없습니다. 페이지를 새로 고침해 주세요.
      </div>
    );
  }

  // phase가 "result"인데 scores가 없으면 sessionStorage가 깨진 상태
  if (phase === "result" && !mbtiId) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-sm text-red-500">결과를 불러올 수 없습니다.</p>
        <button
          onClick={handleRetry}
          className="rounded-xl bg-gray-900 px-6 py-2.5 text-sm text-white hover:bg-gray-700 transition-colors"
        >
          검사 다시 시작하기
        </button>
      </div>
    );
  }

  return (
    <article className="max-w-xl mx-auto">
      {phase === "testing" && questions && (
        <section>
          <header className="mb-8">
            <div className="flex justify-between text-xs text-gray-400 mb-2">
              <span>{currentIndex + 1} / {totalQuestions}</span>
              <span>{Math.round(((currentIndex + 1) / totalQuestions) * 100)}%</span>
            </div>
            <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-gray-800 rounded-full transition-all duration-300"
                style={{ width: `${((currentIndex + 1) / totalQuestions) * 100}%` }}
              />
            </div>
          </header>

          <div className="bg-white rounded-2xl border border-gray-200 p-8 mb-6 shadow-sm">
            {/* <p className="text-xs text-gray-400 uppercase tracking-wide mb-3">
              {questions[currentIndex].category}
            </p> */}
            <p className="text-lg font-medium text-gray-900 leading-relaxed">
              {questions[currentIndex].title}
            </p>
          </div>

          <div className="flex flex-col gap-2">
            {ANSWER_OPTIONS.map((opt) => {
              const isSelected = answers[questions[currentIndex].id] === opt.score;
              return (
                <button
                  key={opt.label}
                  onClick={() => handleAnswer(opt.score)}
                  className={`w-full rounded-xl border px-5 py-3.5 text-sm text-left active:scale-[0.99] transition-colors ${isSelected ? opt.selectedCls : opt.defaultCls}`}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>

          {(() => {
            const currentAnswered = answers[questions[currentIndex].id] !== undefined;
            const isLast = currentIndex === totalQuestions - 1;
            if (!currentAnswered && currentIndex === 0) return null;
            return (
              <div className="mt-6 flex justify-between">
                {currentIndex > 0 ? (
                  <button
                    onClick={() => setCurrentIndex((i) => i - 1)}
                    className="text-xs text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    ← 이전 문항
                  </button>
                ) : <span />}
                {currentAnswered && (
                  <button
                    onClick={handleNext}
                    className="text-xs text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    {isLast ? "결과 보기 →" : "다음 문항 →"}
                  </button>
                )}
              </div>
            );
          })()}
        </section>
      )}

      {phase === "result" && mbtiId && (
        <section className="text-center">
          <div
            className="rounded-2xl p-10 mb-6 text-white shadow-sm"
            style={{ backgroundColor: mbtiMeta?.colorCode ?? "#6b7280" }}
          >
            <p className="text-sm opacity-80 mb-2">나의 MBTI 유형</p>
            <p className="text-6xl font-bold tracking-widest mb-4">{mbtiId}</p>
            {mbtiMeta?.summary && (
              <p className="text-sm opacity-90 leading-relaxed">{mbtiMeta.summary}</p>
            )}
          </div>

          {saved && (
            <p className="mb-4 text-sm text-green-600">마이페이지에 기록되었습니다.</p>
          )}
          {isSubmitting && (
            <p className="mb-4 text-sm text-gray-400">저장 중...</p>
          )}
          {isSubmitError && !saved && (
            <p className="mb-4 text-sm text-red-500">저장에 실패했습니다. 다시 시도해 주세요.</p>
          )}

          <div className="flex flex-col gap-3">
            {!saved && (
              <button
                onClick={() => setSaveDialogOpen(true)}
                disabled={isSubmitting}
                className="w-full rounded-xl bg-gray-900 py-3 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
              >
                MBTI 기록하기
              </button>
            )}
            <button
              onClick={handleShare}
              className="w-full rounded-xl border border-gray-200 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
            >
              결과 공유하기
            </button>
            <button
              onClick={handleRetry}
              className="w-full rounded-xl border border-gray-200 py-3 text-sm text-gray-500 hover:bg-gray-50 transition-colors"
            >
              다시 검사하기
            </button>
          </div>

          <Dialog open={saveDialogOpen} onOpenChange={setSaveDialogOpen}>
            <DialogContent showCloseButton={false}>
              <DialogHeader>
                <DialogTitle>MBTI를 기록하시겠어요?</DialogTitle>
                <DialogDescription>
                  {mbtiId} 유형이 마이페이지에 저장되고 검사 인증 배지가 활성화됩니다.
                </DialogDescription>
              </DialogHeader>
              <DialogFooter>
                <button
                  onClick={() => setSaveDialogOpen(false)}
                  disabled={isSubmitting}
                  className="flex-1 rounded-lg border border-gray-200 py-2 text-sm hover:bg-gray-50 disabled:opacity-50 transition-colors"
                >
                  아니요
                </button>
                <button
                  onClick={handleSaveConfirm}
                  disabled={isSubmitting}
                  className="flex-1 rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                >
                  {isSubmitting ? "저장 중..." : "네, 기록할게요"}
                </button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </section>
      )}
    </article>
  );
}
