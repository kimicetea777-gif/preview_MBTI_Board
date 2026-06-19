"use client";

import { useState, useRef } from "react";
import { useAuthStore } from "@/store/authStore";
import { useReactionDebounce } from "@/hooks/useReactionDebounce";
import { REACTION } from "@/types/comment";
import type { Comment, ReactionType } from "@/types/comment";
import { MdThumbUp, MdThumbUpAlt, MdThumbDown, MdThumbDownAlt } from "react-icons/md";

interface Props {
  comment: Comment;
  postId: number;
}

export default function CommentReactionButtons({ comment, postId }: Props) {
  const isLoggedIn = !!useAuthStore((s) => s.accessToken);
  const { localState, handleClick, isMutating } = useReactionDebounce(comment, postId);
  const [loginAlert, setLoginAlert] = useState(false);
  const alertTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const onReactionClick = (type: ReactionType) => {
    if (!isLoggedIn) {
      if (alertTimerRef.current) clearTimeout(alertTimerRef.current);
      setLoginAlert(true);
      alertTimerRef.current = setTimeout(() => setLoginAlert(false), 2000);
      return;
    }
    handleClick(type);
  };

  const likeActive = localState.myReactionType === REACTION.LIKE;
  const dislikeActive = localState.myReactionType === REACTION.DISLIKE;

  return (
    <div className="flex items-center gap-4">
      <button
        type="button"
        onClick={() => onReactionClick(REACTION.LIKE)}
        disabled={isMutating}
        aria-pressed={likeActive}
        className={`flex items-center gap-1.5 transition-colors ${
          !isLoggedIn
            ? "cursor-default text-on-surface-variant opacity-60"
            : likeActive
            ? "text-primary"
            : "text-on-surface-variant hover:text-secondary"
        }`}
      >
        {likeActive ? <MdThumbUp size={18} /> : <MdThumbUpAlt size={18} />}
        <span className="text-xs">{localState.likeCount}</span>
      </button>

      <button
        type="button"
        onClick={() => onReactionClick(REACTION.DISLIKE)}
        disabled={isMutating}
        aria-pressed={dislikeActive}
        className={`flex items-center gap-1.5 transition-colors ${
          !isLoggedIn
            ? "cursor-default text-on-surface-variant opacity-60"
            : dislikeActive
            ? "text-destructive"
            : "text-on-surface-variant hover:text-secondary"
        }`}
      >
        {dislikeActive ? <MdThumbDown size={18} /> : <MdThumbDownAlt size={18} />}
        <span className="text-xs">{localState.dislikeCount}</span>
      </button>

      {loginAlert && (
        <span className="text-xs text-on-surface-variant animate-callout">
          로그인 후 이용 가능합니다.
        </span>
      )}
    </div>
  );
}
