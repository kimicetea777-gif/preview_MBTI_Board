"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { voteDebateStance, removeDebateStance } from "@/lib/api/post";
import { postKeys } from "@/hooks/usePosts";
import { toast } from "@/components/common/Toast";
import type { DebateStance } from "@/types/comment";

/**
 * 토론 진영 투표 mutation hook
 * - 낙관적 UI: 클릭 즉시 displayStance 업데이트, 오류 시 rollback
 * - 성공 후 게시글 상세 쿼리 invalidate → 서버 카운트 동기화
 */
export function useDebateStance(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (stance: DebateStance | null) =>
      stance === null
        ? removeDebateStance(postId)
        : voteDebateStance(postId, stance),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) });
    },
    onError: () => {
      toast.error("의견 선택에 실패했습니다. 다시 시도해 주세요.");
    },
  });
}
