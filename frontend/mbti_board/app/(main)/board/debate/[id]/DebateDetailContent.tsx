"use client";

import { useState } from "react";
import { useRouter, notFound } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { getApiErrorMessage } from "@/lib/apiError";
import { usePostDetail, postKeys } from "@/hooks/usePosts";
import { updatePost, deletePost } from "@/lib/api/post";
import { useAuthStore } from "@/store/authStore";
import RichContentViewer from "@/components/post/RichContentViewer";
import RichContentEditor from "@/components/post/RichContentEditor";
import GuestPasswordModal, { MODAL_ACTION, type ModalAction } from "@/components/post/GuestPasswordModal";
import MbtiBadge from "@/components/common/MbtiBadge";
import CommentSection from "@/components/post/CommentSection";
import { formatDate, getDeadlineLabel } from "@/lib/dateUtils";
import { isTiptapEmpty } from "@/lib/editor";
import { ROUTES } from "@/lib/routes";
import { STANCE, type DebateStance } from "@/types/comment";
import type { PostUpdateRequest } from "@/types/post";


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
      <div className="h-32 bg-gray-100 rounded-lg mt-4" />
    </section>
  );
}

export default function DebateDetailContent({ id }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isLoggedIn = useAuthStore((s) => s.accessToken) !== null;

  const [selectedStance, setSelectedStance] = useState<DebateStance | null>(null);
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
    onSuccess: () => router.push(ROUTES.board.debate),
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

  const debateResult = post.debateResult;
  const totalDebate = debateResult ? debateResult.proCount + debateResult.conCount : 0;
  const proPct = totalDebate > 0 && debateResult
    ? Math.round((debateResult.proCount / totalDebate) * 100)
    : 50;
  const conPct = 100 - proPct;

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
            !isTiptapEmpty(post.content) && <RichContentViewer content={post.content} />
          )}
        </section>

        {debateResult && (
          <section className="border border-gray-200 rounded-lg p-4 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-gray-700">토론 현황</h2>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                debateResult.isClosed
                  ? "bg-gray-100 text-gray-500"
                  : "bg-gray-900 text-white"
              }`}>
                {getDeadlineLabel(debateResult.endsAt, debateResult.isClosed, "토론 종료")}
              </span>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between text-sm font-medium">
                <span className="text-blue-600">{debateResult.proLabel}</span>
                <span className="text-red-600">{debateResult.conLabel}</span>
              </div>

              <div className="relative h-6 rounded-full overflow-hidden bg-red-200">
                <div
                  className="absolute left-0 top-0 h-full bg-blue-400 transition-all duration-500"
                  style={{ width: `${proPct}%` }}
                />
                <div className="absolute inset-0 flex items-center justify-between px-2 text-xs font-semibold text-white">
                  <span>{proPct}%</span>
                  <span>{conPct}%</span>
                </div>
              </div>

              <div className="flex justify-between text-xs text-gray-500">
                <span>{debateResult.proCount}명</span>
                <span className="text-gray-400">총 {totalDebate}명 참여</span>
                <span>{debateResult.conCount}명</span>
              </div>
            </div>

            {!debateResult.isClosed && (
              <div className="pt-2 border-t border-gray-100 space-y-2">
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setSelectedStance(selectedStance === STANCE.PRO ? null : STANCE.PRO)}
                    disabled={!isLoggedIn}
                    className={`rounded-lg border px-4 py-2 text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${
                      selectedStance === STANCE.PRO
                        ? "border-blue-500 bg-blue-500 text-white"
                        : "border-blue-200 text-blue-600 hover:bg-blue-50"
                    }`}
                  >
                    {debateResult.proLabel}
                  </button>
                  <button
                    type="button"
                    onClick={() => setSelectedStance(selectedStance === STANCE.CON ? null : STANCE.CON)}
                    disabled={!isLoggedIn}
                    className={`rounded-lg border px-4 py-2 text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${
                      selectedStance === STANCE.CON
                        ? "border-red-500 bg-red-500 text-white"
                        : "border-red-200 text-red-600 hover:bg-red-50"
                    }`}
                  >
                    {debateResult.conLabel}
                  </button>
                </div>
                {!isLoggedIn && (
                  <p className="text-xs text-gray-400 text-center">로그인 후 의견을 선택할 수 있습니다.</p>
                )}
              </div>
            )}
          </section>
        )}

        <footer className="pt-2 border-t border-gray-100">
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

      <CommentSection
        postId={id}
        debateStance={debateResult && !debateResult.isClosed ? selectedStance : undefined}
      />

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
