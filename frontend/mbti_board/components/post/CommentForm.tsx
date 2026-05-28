"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useAuthStore } from "@/store/authStore";
import { createComment } from "@/lib/api/comment";
import { commentKeys } from "@/hooks/useComments";
import GuestPasswordModal, { MODAL_ACTION } from "@/components/post/GuestPasswordModal";
import type { DebateStance } from "@/types/comment";

interface Props {
  postId: number;
  debateStance?: DebateStance | null;
  parentId?: number;
  onSuccess?: () => void;
}

export default function CommentForm({ postId, debateStance, parentId, onSuccess }: Props) {
  const isLoggedIn = !!useAuthStore((s) => s.accessToken);
  const queryClient = useQueryClient();

  const [content, setContent] = useState("");
  const [formError, setFormError] = useState<string | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [modalError, setModalError] = useState<string | undefined>();

  // 토론 게시글(debateStance prop이 존재)에서:
  //   - 회원: 의견 미선택 시 등록 불가
  //   - 비회원: 의견 선택 자체가 불가능하므로 댓글 등록 불가 (백엔드도 stance 필수로 거부)
  const isDebatePost = debateStance !== undefined;
  const requiresStance = isDebatePost && (
    (isLoggedIn && debateStance === null) || !isLoggedIn
  );

  const mutation = useMutation({
    mutationFn: (guestPassword?: string) =>
      createComment(postId, {
        content: content.trim(),
        parentId,
        debateStance: isLoggedIn ? debateStance : undefined,
        guestPassword,
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
    if (!content.trim()) return;
    setFormError(undefined);
    if (isLoggedIn) {
      mutation.mutate(undefined);
    } else {
      setModalError(undefined);
      setModalOpen(true);
    }
  };

  return (
    <>
      <form onSubmit={handleSubmit} className="mt-6 space-y-2">
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="댓글을 입력하세요"
          rows={3}
          maxLength={1000}
          className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm resize-none focus:border-gray-400 focus:outline-none"
        />
        {formError && <p className="text-xs text-red-500">{formError}</p>}
        <div className="flex items-center justify-between">
          {requiresStance ? (
            <p className="text-xs text-gray-400">
              {!isLoggedIn
                ? "로그인 후 의견을 선택하고 댓글을 작성할 수 있습니다."
                : "의견을 먼저 선택해주세요."}
            </p>
          ) : (
            <span />
          )}
          <button
            type="submit"
            disabled={mutation.isPending || !content.trim() || requiresStance}
            className="rounded-lg bg-gray-900 px-4 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
          >
            {mutation.isPending ? "등록 중..." : "댓글 등록"}
          </button>
        </div>
      </form>

      <GuestPasswordModal
        open={modalOpen}
        action={MODAL_ACTION.CREATE}
        onConfirm={(password) => mutation.mutate(password)}
        onClose={() => setModalOpen(false)}
        isPending={mutation.isPending}
        error={modalError}
      />
    </>
  );
}
