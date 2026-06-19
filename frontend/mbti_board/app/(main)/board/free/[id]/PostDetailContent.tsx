"use client";

import { getApiErrorMessage } from "@/lib/apiError";
import { useSuspensePostDetail } from "@/hooks/usePosts";
import { usePostEditDelete } from "@/hooks/usePostEditDelete";
import RichContentViewer from "@/components/post/RichContentViewer";
import RichContentEditor from "@/components/post/RichContentEditor";
import GuestPasswordModal from "@/components/post/GuestPasswordModal";
import PostHeader from "@/components/post/PostHeader";
import CommentSection from "@/components/post/CommentSection";
import { isTiptapEmpty } from "@/lib/editor";
import { ROUTES } from "@/constants/routes";

interface Props {
  id: number;
}

export default function PostDetailContent({ id }: Props) {
  const { data: post } = useSuspensePostDetail(id);

  const {
    isEditing,
    editTitle,
    setEditTitle,
    editContent,
    setEditContent,
    handleEditCancel,
    handleSaveClick,
    updateMutation,
    menuItems,
    guestModalProps,
  } = usePostEditDelete({ id, post, redirectPath: ROUTES.board.free });

  return (
    <section className="max-w-225 mx-auto px-4 py-6">
      <article className="space-y-8">

        {/* ---- Post Header (Title)---- */}
        <PostHeader
          title={post.title}
          isEditing={isEditing}
          editTitle={editTitle}
          onEditTitleChange={setEditTitle}
          authorMbtiId={post.authorMbtiId}
          mbtiVerified={post.mbtiVerified}
          authorNickname={post.authorNickname}
          createdAt={post.createdAt}
          viewCount={post.viewCount}
          targetMbtiId={post.targetMbtiId}
          menuItems={menuItems}
        />

        {/* ---- Post Content ---- */}
        <section className="text-on-surface-variant text-base md:text-lg leading-relaxed">
          {isEditing ? (
            <RichContentEditor initialContent={editContent} onChange={setEditContent} />
          ) : (
            !isTiptapEmpty(post.content) && <RichContentViewer content={post.content} />
          )}
        </section>

        {/* ---- Footer: Save / Cancel (editing only) ---- */}
        {isEditing && (
          <footer className="pt-4 border-t border-outline-variant">
            {updateMutation.isError && !post.isGuest && (
              <p className="text-xs text-destructive mb-3 text-right">
                {getApiErrorMessage(updateMutation.error)}
              </p>
            )}
            <div className="flex gap-2 justify-end">
              <button
                type="button"
                onClick={handleEditCancel}
                disabled={updateMutation.isPending}
                className="rounded-lg border border-outline-variant px-5 py-2 text-sm text-on-surface-variant hover:bg-surface-container disabled:opacity-50 transition-colors"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleSaveClick}
                disabled={updateMutation.isPending}
                className="rounded-lg bg-primary text-primary-foreground px-5 py-2 text-sm font-semibold hover:opacity-90 active:scale-[0.98] disabled:opacity-50 transition-all"
              >
                {updateMutation.isPending ? "저장 중..." : "저장"}
              </button>
            </div>
          </footer>
        )}
      </article>

      <CommentSection postId={id} />

      <GuestPasswordModal {...guestModalProps} />
    </section>
  );
}
