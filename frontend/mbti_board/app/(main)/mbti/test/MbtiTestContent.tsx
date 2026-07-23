"use client";

import { useEffect, useMemo, useReducer, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { mbtiKeys, useMbtiTestQuestions, useSubmitMbtiTest, useSubmitFunResponses } from "@/hooks/useMbti";
import { userKeys } from "@/hooks/useUser";
import { useAuthStore } from "@/store/authStore";
import { ROUTES } from "@/constants/routes";
import { MBTI_PENDING_SCORES_KEY } from "@/types/mbti";
import type { MbtiTestScores } from "@/types/mbti";
import { isValidScores, deriveMbtiId } from "@/lib/mbtiUtils";
import { sessionStorage } from "@/lib/storageUtils";
import { MbtiIntroPhase } from "@/components/mbti/MbtiIntroPhase";
import { MbtiTestingPhase } from "@/components/mbti/MbtiTestingPhase";
import { MbtiResultPhase } from "@/components/mbti/MbtiResultPhase";
import { toast } from "@/components/common/Toast";

const PHASE = {
  INTRO:   "intro",
  TESTING: "testing",
  RESULT:  "result",
} as const;
type Phase = typeof PHASE[keyof typeof PHASE];

const PHASE_ACTION = {
  RESTORE: "restore",
  START:   "start",
  ANSWER:  "answer",
  RETRY:   "retry",
} as const;

type PhaseState = { phase: Phase; scores: MbtiTestScores | null };
type PhaseAction =
  | { type: typeof PHASE_ACTION.RESTORE; scores: MbtiTestScores }
  | { type: typeof PHASE_ACTION.START }
  | { type: typeof PHASE_ACTION.ANSWER; scores: MbtiTestScores }
  | { type: typeof PHASE_ACTION.RETRY };

function phaseReducer(_: PhaseState, action: PhaseAction): PhaseState {
  switch (action.type) {
    case PHASE_ACTION.RESTORE: return { phase: PHASE.RESULT, scores: action.scores };
    case PHASE_ACTION.START:   return { phase: PHASE.TESTING, scores: null };
    case PHASE_ACTION.ANSWER:  return { phase: PHASE.RESULT,  scores: action.scores };
    case PHASE_ACTION.RETRY:   return { phase: PHASE.INTRO,   scores: null };
  }
}
/* -------------------------------------------------------------------------- */
export default function MbtiTestContent() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const {
    mutate: submitScores,
    isPending: isSubmitting,
    isSuccess: saved,
    reset: resetSubmit,
  } = useSubmitMbtiTest();

  const { mutate: submitFunResponses } = useSubmitFunResponses();

  const { data: questions } = useMbtiTestQuestions();

  const [{ phase, scores }, dispatch] = useReducer(phaseReducer, { phase: PHASE.INTRO, scores: null });

  const [funAnswers, setFunAnswers] = useState<Record<number, number>>({});  // FUN: id → optionId
  const [saveDialogOpen, setSaveDialogOpen] = useState(false);

  const totalQuestions = questions.length;
  
  // 로그인 후 복귀 시: sessionStorage에서 scores 복원 + 서버 제출
  useEffect(() => {
    if (!useAuthStore.getState().isLoggedIn()) return;

    const parsed = sessionStorage.getJson<MbtiTestScores>(MBTI_PENDING_SCORES_KEY);
    if (!parsed) return;

    sessionStorage.remove(MBTI_PENDING_SCORES_KEY);
    if (!isValidScores(parsed)) return;

    dispatch({ type: PHASE_ACTION.RESTORE, scores: parsed });
    submitScores(parsed, {
      onSuccess: () => {
        toast.success("MBTI가 기록되었습니다.");
        queryClient.invalidateQueries({ queryKey: userKeys.myProfile() });
        queryClient.invalidateQueries({ queryKey: userKeys.myActivityStats() });
      },
      onError: () => {
        toast.error("저장에 실패했습니다.");
      },
    });
  // submitScores, queryClient는 안정적인 참조 — 마운트 1회만 실행
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 결과 화면 진입 즉시 FN 응답 전송 (인증 불필요, 비회원 포함)
  // 복귀 흐름(funAnswers 비어있음)에서는 자동으로 skip
  const mbtiId = useMemo(() => (scores ? deriveMbtiId(scores) : null), [scores]);

  useEffect(() => {
    if (phase !== PHASE.RESULT || !mbtiId) return;
    const optionIds = Object.values(funAnswers);
    if (optionIds.length === 0) return;
    submitFunResponses({ mbtiId, optionIds });
  // mbtiId·phase가 바뀔 때만 실행 — submitFunResponses는 안정적 참조
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, mbtiId]);

  function handleTestComplete(scores: MbtiTestScores, funAnswers: Record<number, number>) {
    setFunAnswers(funAnswers);
    dispatch({ type: PHASE_ACTION.ANSWER, scores });
  }

  function handleSaveConfirm() {
    if (!scores) return;
    if (!useAuthStore.getState().isLoggedIn()) {
      sessionStorage.set(MBTI_PENDING_SCORES_KEY, JSON.stringify(scores));
      router.push(`${ROUTES.login}?redirect=${encodeURIComponent(ROUTES.mbti.test)}`);
      return;
    }
    submitScores(scores, {
      onSuccess: () => {
        toast.success("MBTI가 기록되었습니다.");
        setSaveDialogOpen(false);
        queryClient.invalidateQueries({ queryKey: userKeys.myProfile() });
        queryClient.invalidateQueries({ queryKey: userKeys.myActivityStats() });
      },
      onError: () => {
        toast.error("저장에 실패했습니다.");
      },
    });
  }

  function handleStart() {
    dispatch({ type: PHASE_ACTION.START });
  }

  function handleRetry() {
    dispatch({ type: PHASE_ACTION.RETRY });
    setFunAnswers({});
    setSaveDialogOpen(false);
    resetSubmit();
    queryClient.invalidateQueries({ queryKey: mbtiKeys.questions() });
  }

  if (phase === PHASE.RESULT && !mbtiId) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-sm text-destructive">결과를 불러올 수 없습니다.</p>
        <button
          onClick={handleRetry}
          className="rounded-xl bg-primary px-6 py-2.5 text-sm text-primary-foreground hover:bg-primary/80 transition-colors"
        >
          검사 다시 시작하기
        </button>
      </div>
    );
  }

  return (
    <article className={`mx-auto mt-1 p-5 ${phase === PHASE.RESULT ? "max-w-4xl" : "max-w-xl"}`}>

      {phase === PHASE.INTRO && (
        <MbtiIntroPhase totalQuestions={totalQuestions} onStart={handleStart} />
      )}

      {phase === PHASE.TESTING && (
        <MbtiTestingPhase questions={questions} onComplete={handleTestComplete} />
      )}

      {phase === PHASE.RESULT && mbtiId && (
        <MbtiResultPhase
          mbtiId={mbtiId}
          saved={saved}
          isSubmitting={isSubmitting}
          saveDialogOpen={saveDialogOpen}
          onSaveOpen={() => setSaveDialogOpen(true)}
          onSaveClose={() => setSaveDialogOpen(false)}
          onSaveConfirm={handleSaveConfirm}
        />
      )}
    </article>
  );
}
