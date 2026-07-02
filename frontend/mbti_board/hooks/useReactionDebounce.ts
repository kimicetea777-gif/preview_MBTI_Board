"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toggleReaction } from "@/lib/api/comment";
import { commentKeys } from "@/hooks/useComments";
import { REACTION } from "@/types/comment";
import type { Comment, CommentReactionState, ReactionType } from "@/types/comment";

const DEBOUNCE_MS = 500;

/**
 * 좋아요/싫어요 토글 시 로컬 카운트를 낙관적으로 재계산
 * 같은 타입을 다시 누르면 취소(null), 다른 타입을 누르면 이전 리액션 카운트를 내리고 새 타입 카운트를 올림
 */
function computeOptimisticPatch(
  current: CommentReactionState,
  target: ReactionType
): CommentReactionState {
  let { likeCount, dislikeCount } = current;
  if (current.myReactionType === REACTION.LIKE) likeCount--;
  if (current.myReactionType === REACTION.DISLIKE) dislikeCount--;
  const myReactionType = current.myReactionType === target ? null : target;
  if (myReactionType === REACTION.LIKE) likeCount++;
  if (myReactionType === REACTION.DISLIKE) dislikeCount++;
  return { myReactionType, likeCount, dislikeCount };
}

/** 댓글 목록 캐시(1단계 대댓글 포함)에서 id가 일치하는 댓글/대댓글 하나에만 patch를 병합 */
function applyPatch(list: Comment[], id: number, patch: Partial<Comment>): Comment[] {
  return list.map((c) => {
    if (c.id === id) return { ...c, ...patch };
    if (c.replies.length > 0) {
      return { ...c, replies: c.replies.map((r) => (r.id === id ? { ...r, ...patch } : r)) };
    }
    return c;
  });
}

/**
 * 좋아요/싫어요 클릭 디바운스 훅
 * 주의: 클릭할 때마다 즉시 요청을 보내지 않고 500ms 뒤 "최종 상태"만 서버에 1번 전송 —
 *       연타 시 요청 폭증 방지, 최종 상태가 최초 상태와 같으면(예: 좋아요→싫어요→좋아요) 요청 자체를 생략
 */
export function useReactionDebounce(comment: Comment, postId: number) {
  const queryClient = useQueryClient();

  const [localState, setLocalState] = useState<CommentReactionState>({
    myReactionType: comment.myReactionType,
    likeCount: comment.likeCount,
    dislikeCount: comment.dislikeCount,
  });

  // 클로저 내에서 항상 최신값 접근을 위한 ref
  const localStateRef = useRef<CommentReactionState>(localState);
  const initialReactionRef = useRef<ReactionType | null | undefined>(undefined);
  // undefined: 디바운스 비활성, null | ReactionType: 시퀀스 시작 시점의 서버 상태
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isPendingRef = useRef(false);

  /** 디바운스 시퀀스 종료 — 다음 클릭이 새 시퀀스로 인식되도록 pending 플래그·초기 상태 초기화 */
  const resetPending = () => {
    isPendingRef.current = false;
    initialReactionRef.current = undefined;
  };

  // 디바운스가 idle일 때 서버 데이터 동기화
  useEffect(() => {
    if (!isPendingRef.current) {
      const next: CommentReactionState = {
        myReactionType: comment.myReactionType,
        likeCount: comment.likeCount,
        dislikeCount: comment.dislikeCount,
      };
      localStateRef.current = next;
      setLocalState(next);
    }
  }, [comment.myReactionType, comment.likeCount, comment.dislikeCount]);

  const mutation = useMutation({
    mutationFn: (reactionType: ReactionType) =>
      toggleReaction(comment.id, { reactionType }),

    // 실제 요청 전송 시점(디바운스 종료 시)에 댓글 목록 캐시를 낙관적으로 갱신
    onMutate: async (reactionType) => {
      // 진행 중인 refetch가 낙관적 갱신을 덮어쓰지 않도록 취소
      await queryClient.cancelQueries({ queryKey: commentKeys.byPost(postId) });
      const snapshot = queryClient.getQueryData<Comment[]>(commentKeys.byPost(postId));

      // 캐시의 서버 확정 상태를 기준으로 패치 계산
      const cached =
        snapshot?.find((c) => c.id === comment.id) ??
        snapshot?.flatMap((c) => c.replies).find((r) => r.id === comment.id);

      if (cached) {
        const patch = computeOptimisticPatch(cached, reactionType);
        queryClient.setQueryData<Comment[]>(commentKeys.byPost(postId), (old) =>
          old ? applyPatch(old, comment.id, patch) : old
        );
      }

      return { snapshot };
    },

    // 요청 실패 시 onMutate에서 저장해둔 스냅샷으로 캐시 롤백
    // → 캐시 변경이 comment prop으로 흘러들어오면 idle 동기화 effect(line 68)가 localState도 자동으로 맞춰줌
    onError: (_err, _vars, context) => {
      if (context?.snapshot) {
        queryClient.setQueryData(commentKeys.byPost(postId), context.snapshot);
      }
      // 예약된 타이머(= 아직 안 보낸 후속 클릭)가 있으면 그 타이머가 스스로 pending을 정리하도록 둠
      if (!timerRef.current) resetPending();
    },

    // 성공 시엔 낙관적 패치가 이미 서버와 동일한 증감 로직이라 재검증 없이 그대로 신뢰.
    // pending 해제는 후속 클릭 타이머가 없을 때만(있으면 그 타이머가 처리)
    onSettled: () => {
      if (!timerRef.current) resetPending();
    },
  });

  // mutation.mutate는 TanStack Query가 안정성 보장 — 구조분해로 린터 경고 해소
  const { mutate } = mutation;

  // 내부가 ref와 안정적인 mutate만 사용하므로 useCallback으로 안정화
  const fireRequest = useCallback(() => {
    if (!isPendingRef.current) return;
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }

    const initial = initialReactionRef.current;
    const final = localStateRef.current.myReactionType;

    if (final === initial) {
      // 최종 상태가 초기 상태와 동일 → 요청 불필요
      resetPending();
      return;
    }

    // final이 null이면 기존 리액션 취소(같은 타입 전송), 아니면 추가/전환
    const reactionType = (final === null ? initial : final) as ReactionType;
    mutate(reactionType);
  }, [mutate]);

  // 언마운트 시 남은 디바운스 즉시 실행
  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      fireRequest();
    };
  }, [fireRequest]);

  /** 좋아요/싫어요 버튼 클릭 핸들러 — 화면은 즉시 갱신하고 실제 요청은 디바운스 타이머로 지연 전송 */
  const handleClick = (type: ReactionType) => {
    // 디바운스 시퀀스 시작 시점의 상태 캡처
    if (!isPendingRef.current) {
      initialReactionRef.current = localStateRef.current.myReactionType;
      isPendingRef.current = true;
    }

    const next = computeOptimisticPatch(localStateRef.current, type);
    localStateRef.current = next;
    setLocalState(next);

    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(fireRequest, DEBOUNCE_MS);
  };

  return { localState, handleClick, isMutating: mutation.isPending };
}
