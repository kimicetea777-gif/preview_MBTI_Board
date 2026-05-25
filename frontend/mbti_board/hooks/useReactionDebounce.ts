"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toggleReaction } from "@/lib/api/comment";
import { commentKeys } from "@/hooks/useComments";
import { REACTION } from "@/types/comment";
import type { Comment, CommentReactionState, ReactionType } from "@/types/comment";

const DEBOUNCE_MS = 500;

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

function applyPatch(list: Comment[], id: number, patch: Partial<Comment>): Comment[] {
  return list.map((c) => {
    if (c.id === id) return { ...c, ...patch };
    if (c.replies.length > 0) {
      return { ...c, replies: c.replies.map((r) => (r.id === id ? { ...r, ...patch } : r)) };
    }
    return c;
  });
}

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

    onMutate: async (reactionType) => {
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

    onError: (_err, _vars, context) => {
      if (context?.snapshot) {
        queryClient.setQueryData(commentKeys.byPost(postId), context.snapshot);
        const original =
          context.snapshot.find((c) => c.id === comment.id) ??
          context.snapshot.flatMap((c) => c.replies).find((r) => r.id === comment.id);
        if (original) {
          const rollback: CommentReactionState = {
            myReactionType: original.myReactionType,
            likeCount: original.likeCount,
            dislikeCount: original.dislikeCount,
          };
          localStateRef.current = rollback;
          setLocalState(rollback);
        }
      }
      resetPending();
    },

    onSettled: () => {
      resetPending();
      queryClient.invalidateQueries({ queryKey: commentKeys.byPost(postId) });
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
