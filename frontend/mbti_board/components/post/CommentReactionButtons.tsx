"use client";

import { useState, useRef } from "react";
import { useAuthStore } from "@/store/authStore";
import { useReactionDebounce } from "@/hooks/useReactionDebounce";
import { REACTION } from "@/types/comment";
import type { Comment, ReactionType } from "@/types/comment";

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

  const buttonBase = "flex items-center gap-1 rounded px-1.5 py-0.5 transition-colors";

  return (
    <div className="mt-2 space-y-1">
      <div className="flex items-center gap-3 text-xs text-gray-400">
        <button
          type="button"
          onClick={() => onReactionClick(REACTION.LIKE)}
          disabled={isMutating}
          aria-pressed={localState.myReactionType === REACTION.LIKE}
          className={`${buttonBase} ${
            !isLoggedIn
              ? "cursor-default opacity-50"
              : localState.myReactionType === REACTION.LIKE
              ? "text-blue-500 bg-blue-50"
              : "hover:text-blue-400"
          }`}
        >
          👍 <span>{localState.likeCount}</span>
        </button>
        <button
          type="button"
          onClick={() => onReactionClick(REACTION.DISLIKE)}
          disabled={isMutating}
          aria-pressed={localState.myReactionType === REACTION.DISLIKE}
          className={`${buttonBase} ${
            !isLoggedIn
              ? "cursor-default opacity-50"
              : localState.myReactionType === REACTION.DISLIKE
              ? "text-red-500 bg-red-50"
              : "hover:text-red-400"
          }`}
        >
          👎 <span>{localState.dislikeCount}</span>
        </button>
      </div>
      {loginAlert && (
        <p className="text-xs text-gray-400">로그인 후 이용 가능합니다.</p>
      )}
    </div>
  );
}
