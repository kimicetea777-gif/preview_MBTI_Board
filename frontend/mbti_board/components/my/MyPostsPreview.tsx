"use client";

import { Suspense } from "react";
import Link from "next/link";
import { FiFileText } from "react-icons/fi";
import { useMyPostsPreviewSuspense } from "@/hooks/useUser";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import { formatRelativeTime } from "@/lib/dateUtils";
import { ROUTES } from "@/constants/routes";
import { postHref } from "@/lib/boardUtils";
import MyPostPreviewItem from "./MyPostPreviewItem";

function MyPostsPreviewInner() {
  const { data: posts } = useMyPostsPreviewSuspense();

  return (
    <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/20 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <FiFileText className="w-4 h-4 text-muted-foreground" />
          <h2 className="font-heading text-base font-bold text-foreground">내가 작성한 게시글</h2>
        </div>
        <Link
          href={ROUTES.myPosts}
          className="text-primary text-xs flex items-center gap-0.5 hover:underline transition-colors"
        >
          전체 보기
        </Link>
      </div>

      {posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-32 rounded-xl border border-dashed border-outline-variant/50 bg-surface-container-low">
          <FiFileText className="w-6 h-6 text-outline-variant mb-2" />
          <p className="text-sm text-on-surface-variant">아직 작성한 게시글이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {posts.map((post) => (
            <MyPostPreviewItem
              key={post.id}
              href={postHref(post.typeCode, post.id)}
              typeCode={post.typeCode}
              title={post.title}
              timeLabel={formatRelativeTime(post.createdAt)}
              stats={{ viewCount: post.viewCount, commentCount: post.commentCount }}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default function MyPostsPreview() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={
        <div className="bg-surface-container rounded-xl p-8 border border-outline-variant/20 animate-pulse">
          <div className="flex justify-between items-end mb-6">
            <div className="space-y-2">
              <div className="h-6 w-40 rounded bg-surface-container-high" />
              <div className="h-4 w-56 rounded bg-surface-container-high" />
            </div>
            <div className="h-4 w-16 rounded bg-surface-container-high" />
          </div>
          <div className="space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="flex gap-4 p-4 bg-surface-container-lowest rounded-lg border border-outline-variant/10">
                <div className="w-12 h-12 rounded-lg bg-surface-container-high shrink-0" />
                <div className="grow space-y-2">
                  <div className="flex justify-between gap-2">
                    <div className="h-4 flex-1 rounded bg-surface-container-high" />
                    <div className="h-3 w-10 rounded bg-surface-container-high shrink-0" />
                  </div>
                  <div className="flex gap-2">
                    <div className="h-4 w-10 rounded-full bg-surface-container-high" />
                    <div className="h-4 w-8 rounded bg-surface-container-high" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      }>
        <MyPostsPreviewInner />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
