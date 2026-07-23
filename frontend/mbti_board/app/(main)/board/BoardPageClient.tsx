"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import type { PostType } from "@/types/post";
import { BOARD_META } from "@/constants/navigation";
import BoardView from "./BoardView";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import PostCardSkeleton from "@/components/post/PostCardSkeleton";

function BoardListFallback() {
  return (
    <div className="p-8">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {Array.from({ length: 6 }).map((_, i) => <PostCardSkeleton key={i} />)}
      </div>
    </div>
  );
}

function BoardPageContent() {
  const searchParams = useSearchParams();
  const rawType = searchParams.get("type");
  const type = rawType && rawType in BOARD_META ? (rawType as PostType) : undefined;
  return <BoardView key={type ?? "all"} type={type} />;
}

export default function BoardPageClient() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<BoardListFallback />}>
        <BoardPageContent />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
