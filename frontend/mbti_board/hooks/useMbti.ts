import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchMbtiById, fetchMbtiList, fetchMbtiTestQuestions, submitMbtiTest } from "@/lib/api/mbti";
import type { MbtiTestScores } from "@/types/mbti";

export const mbtiKeys = {
  all:       () => ["mbti"]                              as const,
  list:      () => [...mbtiKeys.all(), "list"]           as const,
  byId:      (id: string) => [...mbtiKeys.all(), "id", id] as const,
  questions: () => [...mbtiKeys.all(), "questions"]      as const,
};

export function useMbtiList() {
  return useQuery({
    queryKey: mbtiKeys.list(),
    queryFn: fetchMbtiList,
    staleTime: Infinity,
  });
}

export function useMbtiById(id: string) {
  return useQuery({
    queryKey: mbtiKeys.byId(id),
    queryFn: () => fetchMbtiById(id),
    staleTime: Infinity,
    enabled: !!id,
  });
}

/** 검사 시작 시 32문항을 한 번만 불러오고, 페이지 이탈 전까지 재요청하지 않음 */
export function useMbtiTestQuestions() {
  return useQuery({
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
