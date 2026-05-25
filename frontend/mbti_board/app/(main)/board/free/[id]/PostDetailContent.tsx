"use client";

import { useState } from "react";
import { useRouter, notFound } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { usePostDetail, postKeys } from "@/hooks/usePosts";
import { updatePost, deletePost } from "@/lib/api/post";
import RichContentViewer from "@/components/post/RichContentViewer";
import RichContentEditor from "@/components/post/RichContentEditor";
import GuestPasswordModal, { MODAL_ACTION, type ModalAction } from "@/components/post/GuestPasswordModal";
import MbtiBadge from "@/components/common/MbtiBadge";
import CommentSection from "@/components/post/CommentSection";
import { formatDate } from "@/lib/dateUtils";
import { ROUTES } from "@/lib/routes";
import type { PostUpdateRequest } from "@/types/post";

function getApiErrorMessage(err: unknown, fallback = "오류가 발생했습니다."): string {
  if (isAxiosError(err)) return err.response?.data?.message ?? fallback;
  return fallback;
}

function isTiptapEmpty(json: string): boolean {
  if (!json) return true;
  try {
    const doc = JSON.parse(json);
    const extractText = (node: { type: string; text?: string; content?: unknown[] }): string => {
      if (node.type === "text") return node.text ?? "";
      return (node.content ?? []).map((n) => extractText(n as typeof node)).join("");
    };
    return !extractText(doc).trim();
  } catch {
    return true;
  }
}

interface Props {
  id: number;
}

function DetailSkeleton() {
  return (
    <section className="max-w-2xl mx-auto px-4 py-6 space-y-4 animate-pulse">
      <div className="h-6 bg-gray-200 rounded w-2/3" />
      <div className="h-4 bg-gray-200 rounded w-1/4" />
      <div className="space-y-2 mt-6">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-4 bg-gray-200 rounded" />
        ))}
      </div>
    </section>
  );
}

export default function PostDetailContent({ id }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [modalAction, setModalAction] = useState<ModalAction>(MODAL_ACTION.UPDATE);
  const [modalError, setModalError] = useState<string | undefined>();

  const { data: post, isLoading, isError, error } = usePostDetail(id);

  const updateMutation = useMutation({
    mutationFn: (req: PostUpdateRequest) => updatePost(id, req),
    onSuccess: () => {
      setIsEditing(false);
      setModalOpen(false);
      queryClient.invalidateQueries({ queryKey: postKeys.detail(id) });
    },
    onError: (err) => setModalError(getApiErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (guestPassword: string | undefined) => deletePost(id, guestPassword),
    onSuccess: () => router.push(ROUTES.board.free),
    onError: (err) => setModalError(getApiErrorMessage(err)),
  });

  if (isError) {
    if (isAxiosError(error) && error.response?.status === 404) notFound();
    return (
      <section className="max-w-2xl mx-auto px-4 py-6">
        <p className="text-sm text-red-500">오류가 발생했습니다. 다시 시도해 주세요.</p>
      </section>
    );
  }

  if (isLoading || !post) return <DetailSkeleton />;

  const handleEditStart = () => {
    setEditTitle(post.title);
    setEditContent(post.content ?? "");
    setIsEditing(true);
  };

  const handleEditCancel = () => setIsEditing(false);

  const handleSaveClick = () => {
    if (!editTitle.trim()) return;
    if (isTiptapEmpty(editContent)) return;
    if (post.isGuest) {
      setModalAction(MODAL_ACTION.UPDATE);
      setModalError(undefined);
      setModalOpen(true);
    } else {
      updateMutation.mutate({ title: editTitle.trim(), content: editContent });
    }
  };

  const handleDeleteClick = () => {
    if (post.isGuest) {
      setModalAction(MODAL_ACTION.DELETE);
      setModalError(undefined);
      setModalOpen(true);
    } else {
      if (!window.confirm("게시글을 삭제하시겠습니까?")) return;
      deleteMutation.mutate(undefined);
    }
  };

  const handleModalConfirm = (password: string) => {
    setModalError(undefined);
    if (modalAction === MODAL_ACTION.UPDATE) {
      updateMutation.mutate({ title: editTitle.trim(), content: editContent, guestPassword: password });
    } else {
      deleteMutation.mutate(password);
    }
  };

  return (
    <section className="max-w-2xl mx-auto px-4 py-6">
      <article className="space-y-4">
        <header className="space-y-2 border-b border-gray-100 pb-4">
          {isEditing ? (
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              maxLength={150}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-lg font-bold focus:border-gray-500 focus:outline-none"
            />
          ) : (
            <h1 className="text-xl font-bold text-gray-900">{post.title}</h1>
          )}

          <div className="flex flex-wrap items-center gap-2 text-xs text-gray-500">
            <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} />
            <span>{post.authorNickname}</span>
            <span>·</span>
            <time dateTime={post.createdAt}>
              {formatDate(post.createdAt)}
            </time>
            <span>조회 {post.viewCount}</span>
          </div>
        </header>

        <section>
          {isEditing ? (
            <RichContentEditor initialContent={editContent} onChange={setEditContent} />
          ) : (
            <RichContentViewer content={post.content} />
          )}
        </section>

        <footer className="pt-2 border-t border-gray-100">
          {/* 회원이 편집 모드에서 저장 실패했을 때 */}
          {isEditing && updateMutation.isError && !post.isGuest && (
            <p className="text-xs text-red-500 mb-2 text-right">
              {getApiErrorMessage(updateMutation.error)}
            </p>
          )}
          <div className="flex gap-2 justify-end">
          {isEditing ? (
            <>
              <button
                type="button"
                onClick={handleEditCancel}
                disabled={updateMutation.isPending}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleSaveClick}
                disabled={updateMutation.isPending || !editTitle.trim()}
                className="rounded-lg bg-gray-900 px-4 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
              >
                {updateMutation.isPending ? "저장 중..." : "저장"}
              </button>
            </>
          ) : (
            <>
              {post.isOwner && !post.isGuest && (
                <>
                  <button
                    type="button"
                    onClick={handleEditStart}
                    className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={handleDeleteClick}
                    disabled={deleteMutation.isPending}
                    className="rounded-lg border border-red-200 px-4 py-2 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50 transition-colors"
                  >
                    {deleteMutation.isPending ? "삭제 중..." : "삭제"}
                  </button>
                </>
              )}
              {post.isGuest && (
                <>
                  <button
                    type="button"
                    onClick={handleEditStart}
                    className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                  >
                    수정 (비회원)
                  </button>
                  <button
                    type="button"
                    onClick={handleDeleteClick}
                    className="rounded-lg border border-red-200 px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
                  >
                    삭제 (비회원)
                  </button>
                </>
              )}
            </>
          )}
          </div>
        </footer>
      </article>

      <CommentSection postId={id} />

      <GuestPasswordModal
        open={modalOpen}
        action={modalAction}
        onConfirm={handleModalConfirm}
        onClose={() => setModalOpen(false)}
        isPending={updateMutation.isPending || deleteMutation.isPending}
        error={modalError}
      />
    </section>
  );
}
