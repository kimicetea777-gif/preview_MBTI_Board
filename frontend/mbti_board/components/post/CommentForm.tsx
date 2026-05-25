"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useAuthStore } from "@/store/authStore";
import { createComment } from "@/lib/api/comment";
import { commentKeys } from "@/hooks/useComments";
import GuestPasswordModal, { MODAL_ACTION } from "@/components/post/GuestPasswordModal";

interface Props {
  postId: number;
}

export default function CommentForm({ postId }: Props) {
  const isLoggedIn = !!useAuthStore((s) => s.accessToken);
  const queryClient = useQueryClient();

  const [content, setContent] = useState("");
  const [formError, setFormError] = useState<string | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [modalError, setModalError] = useState<string | undefined>();

  const mutation = useMutation({
    mutationFn: (guestPassword?: string) =>
      createComment(postId, { content: content.trim(), guestPassword }),
    onSuccess: () => {
      setContent("");
      setFormError(undefined);
      setModalOpen(false);
      setModalError(undefined);
      queryClient.invalidateQueries({ queryKey: commentKeys.byPost(postId) });
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
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={mutation.isPending || !content.trim()}
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
