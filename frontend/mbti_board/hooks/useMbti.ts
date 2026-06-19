import { useMutation, useSuspenseQuery } from "@tanstack/react-query";
import { fetchMbtiTestQuestions, submitMbtiTest, submitFunResponses, fetchFunStats } from "@/lib/api/mbti";
import type { MbtiTestScores, TotalQuestion, FunResponseSubmit } from "@/types/mbti";

export const mbtiKeys = {
  all:       () => ["mbti"]                         as const,
  questions: () => [...mbtiKeys.all(), "questions"] as const,
  funStats:  () => [...mbtiKeys.all(), "fun-stats"] as const,
};

/** 검사 시작 시 35문항을 한 번만 불러오고, 페이지 이탈 전까지 재요청하지 않음 */
export function useMbtiTestQuestions() {
  return useSuspenseQuery<TotalQuestion[]>({
    queryKey: mbtiKeys.questions(),
    queryFn: fetchMbtiTestQuestions,
    staleTime: Infinity,
    gcTime: 0,
  });
}

/**
 * 점수 제출 mutation.
 * 비로그인 사용자는 점수를 sessionStorage(MBTI_PENDING_SCORES_KEY)에 저장 후
 * 로그인 완료 시 이 mutation을 호출한다.
 */
export function useSubmitMbtiTest() {
  return useMutation({
    mutationFn: (scores: MbtiTestScores) => submitMbtiTest(scores),
  });
}

/** FN 응답 제출 mutation — 결과 화면 진입 즉시 호출, 인증 불필요 */
export function useSubmitFunResponses() {
  return useMutation({
    mutationFn: (data: FunResponseSubmit) => submitFunResponses(data),
  });
}

/** FN 질문 전체 통계 조회 */
export function useFunStats() {
  return useSuspenseQuery({
    queryKey: mbtiKeys.funStats(),
    queryFn: fetchFunStats,
    staleTime: 1000 * 60 * 5,
  });
}
