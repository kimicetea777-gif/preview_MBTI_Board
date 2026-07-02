"use client";

import { useState } from "react";
import { useSuspensePostDetail } from "@/hooks/usePosts";
import { usePostDelete } from "@/hooks/usePostDelete";
import { useAuthStore } from "@/store/authStore";
import RichContentViewer from "@/components/post/RichContentViewer";
import PostHeader from "@/components/post/PostHeader";
import CommentSection from "@/components/post/CommentSection";
import DebateSection from "@/components/post/DebateSection";
import { isTiptapEmpty } from "@/lib/editor";
import { ROUTES } from "@/constants/routes";
import { type DebateStance } from "@/types/comment";

interface Props {
  id: number;
}

export default function DebateDetailContent({ id }: Props) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn());
  const [selectedStance, setSelectedStance] = useState<DebateStance | null>(null);

  const { data: post } = useSuspensePostDetail(id);

  const { menuItems } = usePostDelete({
    id,
    post,
    redirectPath: ROUTES.board.debate,
    editUnavailableMessage: "토론 게시글은 수정할 수 없습니다.",
  });

  const debateResult = post.debateResult;

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

        {debateResult && (
          <DebateSection
            postId={id}
            debateResult={debateResult}
            isLoggedIn={isLoggedIn}
            onStanceChange={setSelectedStance}
          />
        )}

      </article>

      <CommentSection
        postId={id}
        debateStance={debateResult && !debateResult.isClosed ? selectedStance : undefined}
        proLabel={debateResult?.proLabel}
        conLabel={debateResult?.conLabel}
      />

    </section>
  );
}
