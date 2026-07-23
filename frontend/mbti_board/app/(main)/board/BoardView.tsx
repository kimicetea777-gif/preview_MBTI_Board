"use client";

import { Suspense, useState } from "react";
import BoardListHeader from "@/components/post/BoardListHeader";
import BoardPostList from "@/components/post/BoardPostList";
import PostCardSkeleton from "@/components/post/PostCardSkeleton";
import { POST_SORT, VIEW_MODE } from "@/types/post";
import type { PostSortType, PostType, ViewMode } from "@/types/post";

function BoardListFallback() {
  return (
    <div className="p-8">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {Array.from({ length: 6 }).map((_, i) => <PostCardSkeleton key={i} />)}
      </div>
    </div>
  );
}

export default function BoardView({ type }: { type: PostType | undefined }) {
  const [sort, setSort] = useState<PostSortType>(POST_SORT.LATEST);
  const [authorMbti, setAuthorMbti] = useState<string | undefined>();
  const [viewMode, setViewMode] = useState<ViewMode>(VIEW_MODE.CARD);

  return (
    <>
      <BoardListHeader
        type={type}
        sort={sort}
        onSortChange={setSort}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        authorMbti={authorMbti}
        onAuthorMbtiChange={setAuthorMbti}
      />
      <Suspense fallback={<BoardListFallback />}>
        <BoardPostList type={type} sort={sort} authorMbti={authorMbti} viewMode={viewMode} />
      </Suspense>
    </>
  );
}
