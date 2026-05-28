"use client";

import { useState } from "react";
import { useComments } from "@/hooks/useComments";
import MbtiBadge from "@/components/common/MbtiBadge";
import { formatDate } from "@/lib/dateUtils";
import CommentForm from "@/components/post/CommentForm";
import CommentReactionButtons from "@/components/post/CommentReactionButtons";
import { STANCE } from "@/types/comment";
import type { Comment, DebateStance } from "@/types/comment";

interface CommentItemProps {
  comment: Comment;
  postId: number;
  isReply?: boolean;
  debateStance?: DebateStance | null;
}

function CommentItem({ comment, postId, isReply = false, debateStance }: CommentItemProps) {
  const [showReplyForm, setShowReplyForm] = useState(false);

  return (
    <li>
      <article
        className={
          isReply
            ? "ml-8 pl-4 border-l-2 border-gray-100 py-3"
            : "py-4"
        }
      >
        <header className="flex flex-wrap items-center gap-1.5 mb-2">
          <MbtiBadge mbti={comment.authorMbtiId} verified={comment.mbtiVerified} />
          <span className="text-sm font-medium text-gray-800">{comment.authorNickname}</span>
          {comment.debateStance !== null && (
            <span
              className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium text-white ${
                comment.debateStance === STANCE.PRO ? "bg-blue-500" : "bg-red-500"
              }`}
            >
              {comment.debateStance === STANCE.PRO ? "찬성" : "반대"}
            </span>
          )}
          <time
            className="ml-auto text-xs text-gray-400"
            dateTime={comment.createdAt}
          >
            {formatDate(comment.createdAt)}
          </time>
        </header>

        <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
          {comment.content}
        </p>

        <footer className="flex items-start justify-between">
          <CommentReactionButtons comment={comment} postId={postId} />
          {!isReply && (
            <button
              type="button"
              onClick={() => setShowReplyForm((v) => !v)}
              className="mt-2 text-xs text-gray-400 hover:text-gray-700 transition-colors shrink-0"
            >
              {showReplyForm ? "취소" : "답글"}
            </button>
          )}
        </footer>
      </article>

      {/* 서버가 1단계 깊이만 보장하므로 재귀 없이 단일 레벨 렌더링 */}
      {comment.replies.length > 0 && (
        <ul>
          {comment.replies.map((reply) => (
            <CommentItem key={reply.id} comment={reply} postId={postId} isReply debateStance={debateStance} />
          ))}
        </ul>
      )}

      {showReplyForm && (
        <div className="ml-8 pl-4 border-l-2 border-gray-100 py-2">
          <CommentForm
            postId={postId}
            parentId={comment.id}
            debateStance={debateStance}
            onSuccess={() => setShowReplyForm(false)}
          />
        </div>
      )}
    </li>
  );
}

function CommentSkeleton() {
  return (
    <div className="mt-8 border-t border-gray-100 pt-6 space-y-4 animate-pulse">
      <div className="h-4 bg-gray-200 rounded w-16" />
      {[1, 2, 3].map((i) => (
        <div key={i} className="py-4 border-b border-gray-100 space-y-2">
          <div className="flex items-center gap-2">
            <div className="h-5 bg-gray-200 rounded-full w-14" />
            <div className="h-4 bg-gray-200 rounded w-16" />
          </div>
          <div className="h-4 bg-gray-200 rounded w-3/4" />
          <div className="h-3 bg-gray-200 rounded w-1/2" />
        </div>
      ))}
    </div>
  );
}

interface Props {
  postId: number;
  debateStance?: DebateStance | null;
}

export default function CommentSection({ postId, debateStance }: Props) {
  const { data: comments, isLoading, isError } = useComments(postId);

  if (isLoading) return <CommentSkeleton />;
  
  if (isError) return (
    <p className="mt-8 pt-6 border-t border-gray-100 text-sm text-red-500">
      댓글을 불러오는 중 오류가 발생했습니다.
    </p>
  );

  const totalCount =
    (comments?.length ?? 0) +
    (comments?.reduce((sum, c) => sum + c.replies.length, 0) ?? 0);

  return (
    <section className="mt-8 border-t border-gray-100 pt-6">
      <h2 className="text-sm font-semibold text-gray-700 mb-4">
        댓글{" "}
        <span className="font-normal text-gray-500">{totalCount}</span>
      </h2>

      <CommentForm postId={postId} debateStance={debateStance} />

      {comments && comments.length > 0 ? (
        <ul className="divide-y divide-gray-100">
          {comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} postId={postId} debateStance={debateStance} />
          ))}
        </ul>
      ) : (
        <p className="py-10 text-center text-sm text-gray-400">
          아직 댓글이 없습니다. 첫 댓글을 남겨보세요.
        </p>
      )}
    </section>
  );
}
