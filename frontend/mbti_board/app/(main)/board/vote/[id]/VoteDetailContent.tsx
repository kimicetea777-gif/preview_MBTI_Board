"use client";

import { useSuspensePostDetail } from "@/hooks/usePosts";
import { usePostDelete } from "@/hooks/usePostDelete";
import { useAuthStore } from "@/store/authStore";
import RichContentViewer from "@/components/post/RichContentViewer";
import PostHeader from "@/components/post/PostHeader";
import CommentSection from "@/components/post/CommentSection";
import VoteSection from "@/components/post/VoteSection";
import { isTiptapEmpty } from "@/lib/editor";
import { ROUTES } from "@/constants/routes";

interface Props {
  id: number;
}

export default function VoteDetailContent({ id }: Props) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn());

  const { data: post } = useSuspensePostDetail(id);

  const { menuItems } = usePostDelete({
    id,
    post,
    redirectPath: ROUTES.board.vote,
    editUnavailableMessage: "투표 게시글은 수정할 수 없습니다.",
  });

  return (
    <section className="max-w-225 mx-auto px-4 py-6">
      <article className="space-y-8">

        <PostHeader
          title={post.title}
          authorMbtiId={post.authorMbtiId}
          mbtiVerified={post.mbtiVerified}
          authorNickname={post.authorNickname}
          createdAt={post.createdAt}
          viewCount={post.viewCount}
          menuItems={menuItems}
        />

        {!isTiptapEmpty(post.content) && (
          <section className="text-on-surface-variant text-base md:text-lg leading-relaxed">
            <RichContentViewer content={post.content} />
          </section>
        )}

        {post.voteResult && (
          <VoteSection postId={id} voteResult={post.voteResult} isLoggedIn={isLoggedIn} />
        )}

      </article>

      <CommentSection postId={id} />

    </section>
  );
}
