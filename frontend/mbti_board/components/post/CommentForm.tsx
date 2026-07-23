"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useAuthStore } from "@/store/authStore";
import { createComment } from "@/lib/api/comment";
import { commentKeys } from "@/hooks/useComments";
import GuestPasswordModal, { MODAL_ACTION, type GuestAgreement } from "@/components/post/GuestPasswordModal";
import ErrorMessage from "@/components/common/ErrorMessage";
import { toast } from "@/components/common/Toast";
import type { DebateStance } from "@/types/comment";

interface Props {
  postId: number;
  debateStance?: DebateStance | null;
  parentId?: number;
  onSuccess?: () => void;
}

/*
  토론 게시글(debateStance prop이 존재)에서:
    - 회원: 의견 미선택 시 등록 불가
    - 비회원: 의견 선택 자체가 불가능하므로 댓글 등록 불가 (백엔드도 stance 필수로 거부)
*/
export default function CommentForm({ postId, debateStance, parentId, onSuccess }: Props) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn());
  const queryClient = useQueryClient();

  const [content, setContent] = useState("");
  const [formError, setFormError] = useState<string | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [modalError, setModalError] = useState<string | undefined>();

  // Debate Post State
  const isDebatePost = (debateStance !== undefined);
  const requiresStance = isDebatePost && (
    (isLoggedIn && debateStance === null) || !isLoggedIn
  );

  const mutation = useMutation({
    mutationFn: (params?: { guestPassword?: string; agreement?: GuestAgreement }) =>
      createComment(postId, {
        content: content.trim(),
        parentId,
        debateStance: isLoggedIn ? debateStance : undefined,
        guestPassword: params?.guestPassword,
        termsAgreed: params?.agreement?.termsAgreed,
        privacyAgreed: params?.agreement?.privacyAgreed,
        age14Agreed: params?.agreement?.age14Agreed,
      }),
    onSuccess: () => {
      setContent("");
      setFormError(undefined);
      setModalOpen(false);
      setModalError(undefined);
      queryClient.invalidateQueries({ queryKey: commentKeys.byPost(postId) });
      onSuccess?.();
    },
    onError: (err) => {
      const msg = isAxiosError(err)
        ? (err.response?.data?.message ?? "댓글 작성에 실패했습니다.")
        : "댓글 작성에 실패했습니다.";
      if (isLoggedIn) {
        setFormError(msg);
      } else {
        setModalError(msg);
      }
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) {
      toast.warning(
          "댓글 내용을 작성해주세요."
      );
      return;
    }

    if (requiresStance) {
      toast.warning(
        !isLoggedIn
          ? "로그인 후 의견을 선택하고 댓글을 작성할 수 있습니다."
          : "의견을 먼저 선택해주세요."
      );
      return;
    }

    setFormError(undefined);
    if (isLoggedIn) {
      mutation.mutate(undefined);
    } else {
      setModalError(undefined);
      setModalOpen(true);
    }
  };

  const handleGuestConfirm = (guestPassword: string, agreement?: GuestAgreement) => {
    mutation.mutate({ guestPassword, agreement });
  };

  const isButtonDisabled = mutation.isPending || !content.trim();

  return (
    <>
      <form onSubmit={handleSubmit}>
        <div className="bg-surface-container-low rounded-xl p-4 border border-outline-variant focus-within:border-secondary transition-colors">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="댓글을 입력하세요"
            rows={4}
            maxLength={1000}
            className="w-full bg-transparent border-none focus:ring-0 text-sm text-on-surface placeholder:text-outline-variant resize-none focus:outline-none"
          />
          <div className="flex justify-end mt-2 pt-2 border-t border-outline-variant">
            <button
              type="submit"
              disabled={mutation.isPending}
              className={`bg-primary text-primary-foreground px-8 py-2 rounded-lg text-sm font-semibold transition-all ${
                isButtonDisabled || requiresStance
                  ? "opacity-50 cursor-not-allowed"
                  : "hover:opacity-90 active:scale-[0.98]"
              }`}
            >
              {mutation.isPending ? "등록 중..." : "댓글 등록"}
            </button>
          </div>
        </div>
        <ErrorMessage message={formError} />
      </form>

      <GuestPasswordModal
        open={modalOpen}
        action={MODAL_ACTION.CREATE}
        onConfirm={handleGuestConfirm}
        onClose={() => setModalOpen(false)}
        isPending={mutation.isPending}
        error={modalError}
      />
    </>
  );
}
