"use client";

import { useState, useRef, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useComments, commentKeys } from "@/hooks/useComments";
import { useAuthStore } from "@/store/authStore";
import MbtiBadge from "@/components/common/MbtiBadge";
import { formatRelativeTime } from "@/lib/dateUtils";
import CommentForm from "@/components/post/CommentForm";
import ErrorMessage from "@/components/common/ErrorMessage";
import CommentReactionButtons from "@/components/post/CommentReactionButtons";
import GuestPasswordModal, { MODAL_ACTION, type ModalAction } from "@/components/post/GuestPasswordModal";
import { updateComment, deleteComment } from "@/lib/api/comment";
import { STANCE } from "@/types/comment";
import type { Comment, DebateStance } from "@/types/comment";
import { MdReply, MdMoreVert, MdEdit, MdDelete, MdCheckCircle, MdCancel } from "react-icons/md";

interface CommentItemProps {
  comment: Comment;
  postId: number;
  isReply?: boolean;
  debateStance?: DebateStance | null;
  proLabel?: string;
  conLabel?: string;
}

function CommentItem({ comment, postId, isReply = false, debateStance, proLabel, conLabel }: CommentItemProps) {
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [editError, setEditError] = useState<string | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [modalAction, setModalAction] = useState<ModalAction>(MODAL_ACTION.UPDATE);
  const [modalError, setModalError] = useState<string | undefined>();

  const menuRef = useRef<HTMLDivElement>(null);
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn());
  const queryClient = useQueryClient();

  const showMenuButton = comment.isMine || (!isLoggedIn && comment.isGuest);
  const isMenuOpen = showMenu && showMenuButton;

  useEffect(() => {
    if (!isMenuOpen) return;
    const handleOutsideClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowMenu(false);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [isMenuOpen]);


  const updateMutation = useMutation({
    mutationFn: (guestPassword?: string) =>
      updateComment(postId, comment.id, { content: editContent.trim(), guestPassword }),
    onSuccess: () => {
      setShowEditForm(false);
      setEditError(undefined);
      setModalOpen(false);
      setModalError(undefined);
      queryClient.invalidateQueries({ queryKey: commentKeys.byPost(postId) });
    },
    onError: (err) => {
      const msg = isAxiosError(err)
        ? (err.response?.data?.message ?? "댓글 수정에 실패했습니다.")
        : "댓글 수정에 실패했습니다.";
      if (isLoggedIn) {
        setEditError(msg);
      } else {
        setModalError(msg);
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (guestPassword?: string) =>
      deleteComment(postId, comment.id, guestPassword),
    onSuccess: () => {
      setModalOpen(false);
      setModalError(undefined);
      queryClient.invalidateQueries({ queryKey: commentKeys.byPost(postId) });
    },
    onError: (err) => {
      const msg = isAxiosError(err)
        ? (err.response?.data?.message ?? "댓글 삭제에 실패했습니다.")
        : "댓글 삭제에 실패했습니다.";
      setModalError(msg);
    },
  });

  const handleEditClick = () => {
    setEditContent(comment.content);
    setShowEditForm(true);
    setShowMenu(false);
  };

  const handleDeleteClick = () => {
    if (deleteMutation.isPending) return;
    setShowMenu(false);
    if (isLoggedIn) {
      deleteMutation.mutate(undefined);
    } else {
      setModalAction(MODAL_ACTION.DELETE);
      setModalError(undefined);
      setModalOpen(true);
    }
  };

  const handleEditSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editContent.trim()) return;
    setEditError(undefined);
    if (isLoggedIn) {
      updateMutation.mutate(undefined);
    } else {
      setModalAction(MODAL_ACTION.UPDATE);
      setModalError(undefined);
      setModalOpen(true);
    }
  };

  const commentBody = (
    <div className={isReply ? "p-3" : ""}>
      <header className="flex justify-between items-start mb-4 gap-2">
        <div className="flex items-center gap-3 min-w-0">
          <MbtiBadge mbti={comment.authorMbtiId} verified={comment.mbtiVerified} className="shrink-0" />
            <p className="text-sm font-semibold text-primary truncate">{comment.authorNickname}</p>
            <time className="text-xs text-on-surface-variant shrink-0" dateTime={comment.createdAt}>
              {formatRelativeTime(comment.createdAt)}
            </time>
        </div>
        {comment.debateStance !== null && (
          <span className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold shrink-0 ${
            comment.debateStance === STANCE.PRO ? "bg-secondary/10 text-secondary" : "bg-primary/10 text-primary"
          }`}>
            {comment.debateStance === STANCE.PRO ? <MdCheckCircle size={14} /> : <MdCancel size={14} />}
            {comment.debateStance === STANCE.PRO ? (proLabel ?? "찬성") : (conLabel ?? "반대")}
          </span>
        )}
        {comment.voteOptionText != null && (
          <span className="inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold shrink-0 bg-primary/10 text-primary">
            <MdCheckCircle size={14} />
            {comment.voteOptionText}
          </span>
        )}
      </header>

      {showEditForm ? (
        <form onSubmit={handleEditSubmit} className="space-y-2 mb-3">
          <textarea
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
            rows={3}
            maxLength={1000}
            className="w-full rounded-xl border border-outline-variant bg-surface-container-lowest px-4 py-3 text-sm resize-none focus:border-ring focus:outline-none placeholder:text-on-surface-variant/50 transition-colors"
          />
          <ErrorMessage message={editError} />
          <div className="flex gap-2 justify-end">
            <button
              type="button"
              onClick={() => { setShowEditForm(false); setEditError(undefined); }}
              className="rounded-full border border-outline-variant px-4 py-1.5 text-xs text-on-surface-variant hover:bg-secondary/10 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={updateMutation.isPending || !editContent.trim()}
              className="rounded-full bg-primary text-primary-foreground px-4 py-1.5 text-xs hover:opacity-90 disabled:opacity-50 transition-all active:scale-95"
            >
              {updateMutation.isPending ? "수정 중..." : "수정 완료"}
            </button>
          </div>
        </form>
      ) : (
        <p className="text-sm text-on-surface whitespace-pre-wrap leading-relaxed mb-3">
          {comment.content}
        </p>
      )}

      <footer className="flex items-center gap-3">
        <CommentReactionButtons comment={comment} postId={postId} />
        {!isReply && (
          <button
            type="button"
            onClick={() => setShowReplyForm((v) => !v)}
            className="flex items-center gap-1.5 text-on-surface-variant hover:text-secondary transition-colors"
          >
            <MdReply size={18} />
            <span className="text-xs">{showReplyForm ? "취소" : "답글"}</span>
          </button>
        )}
        {showMenuButton && <div className="ml-auto relative" ref={menuRef}>
          <button
            type="button"
            onClick={() => setShowMenu((v) => !v)}
            disabled={updateMutation.isPending || deleteMutation.isPending}
            className="flex items-center text-on-surface-variant hover:text-secondary transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <MdMoreVert size={18} />
          </button>
          {isMenuOpen && (
            <div className="absolute right-0 bottom-full mb-1 bg-surface-container-lowest border border-outline-variant rounded-lg shadow-md py-1 z-10 min-w-20">
              <button
                type="button"
                className="flex items-center gap-2 px-3 py-1.5 text-xs text-on-surface hover:bg-secondary/10 w-full"
                onClick={handleEditClick}
              >
                <MdEdit size={14} />
                수정
              </button>
              <button
                type="button"
                className="flex items-center gap-2 px-3 py-1.5 text-xs text-destructive hover:bg-destructive/10 w-full"
                onClick={handleDeleteClick}
              >
                <MdDelete size={14} />
                삭제
              </button>
            </div>
          )}
        </div>}
      </footer>
    </div>
  );

  const modal = (
    <GuestPasswordModal
      open={modalOpen}
      action={modalAction}
      onConfirm={(password) => {
        if (modalAction === MODAL_ACTION.DELETE) {
          deleteMutation.mutate(password);
        } else {
          updateMutation.mutate(password);
        }
      }}
      onClose={() => { setModalOpen(false); setModalError(undefined); }}
      isPending={updateMutation.isPending || deleteMutation.isPending}
      error={modalError}
    />
  );

  if (isReply) {
    return <><article>{commentBody}</article>{modal}</>;
  }

  return (
    <li className="list-none">
      <div className="bg-surface-container-lowest rounded-2xl border border-surface-container overflow-hidden" style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.08)" }}>
        <article className="p-6">
          {commentBody}
        </article>

        {showReplyForm && (
          <div className="px-5 pb-5">
            <CommentForm
              postId={postId}
              parentId={comment.id}
              debateStance={debateStance}
              onSuccess={() => setShowReplyForm(false)}
            />
          </div>
        )}

        {comment.replies.length > 0 && (
          <ul className="px-5 pb-5 space-y-4">
            {comment.replies.map((reply) => (
              <li
                key={reply.id}
                className="ml-6 bg-surface-container-low rounded-2xl border border-outline-variant overflow-hidden"
                style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.08)" }}
              >
                <CommentItem
                  comment={reply}
                  postId={postId}
                  isReply
                  debateStance={debateStance}
                  proLabel={proLabel}
                  conLabel={conLabel}
                />
              </li>
            ))}
          </ul>
        )}
      </div>

      {modal}
    </li>
  );
}

function CommentSkeleton() {
  return (
    <div className="mt-8 pt-6 space-y-3 animate-pulse">
      <div className="h-5 bg-muted rounded w-20" />
      {[1, 2, 3].map((i) => (
        <div key={i} className="bg-surface-container-lowest rounded-2xl border border-outline-variant p-5 space-y-3">
          <div className="flex items-center gap-2">
            <div className="h-5 bg-muted rounded-full w-14" />
            <div className="h-4 bg-muted rounded w-20" />
          </div>
          <div className="h-4 bg-muted rounded w-3/4" />
          <div className="h-3 bg-muted rounded w-1/2" />
        </div>
      ))}
    </div>
  );
}

interface Props {
  postId: number;
  debateStance?: DebateStance | null;
  proLabel?: string;
  conLabel?: string;
}

export default function CommentSection({ postId, debateStance, proLabel, conLabel }: Props) {
  const { data: comments, isLoading, isError } = useComments(postId);

  if (isLoading) return <CommentSkeleton />;

  if (isError) return (
    <p className="mt-8 pt-6 text-sm text-destructive">
      댓글을 불러오는 중 오류가 발생했습니다.
    </p>
  );

  const totalCount =
    (comments?.length ?? 0) +
    (comments?.reduce((sum, c) => sum + c.replies.length, 0) ?? 0);

  return (
    <section className="mt-12 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-xl font-bold text-primary flex items-center gap-2">
          {/* <MdChatBubbleOutline size={22} /> */}
          댓글
          <span className="text-on-surface-variant font-normal text-base">
            ({totalCount.toLocaleString()})
          </span>
        </h3>
      </div>

      <CommentForm postId={postId} debateStance={debateStance} />

      {comments && comments.length > 0 ? (
        <ul className="space-y-6">
          {comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} postId={postId} debateStance={debateStance} proLabel={proLabel} conLabel={conLabel} />
          ))}
        </ul>
      ) : (
        <p className="py-8 text-center text-sm text-on-surface-variant">
          아직 댓글이 없습니다. 첫 댓글을 남겨보세요.
        </p>
      )}
    </section>
  );
}
