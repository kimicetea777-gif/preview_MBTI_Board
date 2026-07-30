"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useAdminPost } from "@/hooks/useAdmin";
import { POST_TYPE_LABELS, POST_TYPE_META, POST_HIDDEN_BY_LABELS } from "@/types/post";
import { ROUTES } from "@/constants/routes";
import PostHeader from "@/components/post/PostHeader";
import RichContentViewer from "@/components/post/RichContentViewer";
import PostDetailSkeleton from "@/components/post/PostDetailSkeleton";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";

interface Props {
  id: number;
}

function AdminPostDetailInner({ id }: Props) {
  const { data: post } = useAdminPost(id);

  return (
    <section className="max-w-225 mx-auto px-4 py-6">
      <div className="flex items-center justify-between mb-4">
        <Link href={ROUTES.adminPosts} className="text-sm text-on-surface-variant hover:text-foreground transition-colors">
          ← 게시글 관리로
        </Link>
        <div className="flex items-center gap-2">
          <span className={`rounded px-2 py-0.5 text-xs font-medium ${POST_TYPE_META[post.typeCode]?.color ?? ""}`}>
            {POST_TYPE_LABELS[post.typeCode] ?? post.typeCode}
          </span>
          {post.isVisible ? (
            <span className="rounded px-2 py-0.5 text-xs font-medium text-green-600">공개</span>
          ) : (
            <span className="rounded px-2 py-0.5 text-xs font-medium text-red-500">
              숨김{post.hiddenBy && ` · ${POST_HIDDEN_BY_LABELS[post.hiddenBy]}`}
            </span>
          )}
        </div>
      </div>

      <article className="space-y-8">
        <PostHeader
          title={post.title}
          authorMbtiId={post.authorMbtiId}
          mbtiVerified={post.mbtiVerified}
          authorNickname={post.authorNickname}
          createdAt={post.createdAt}
          viewCount={post.viewCount}
          targetMbtiId={post.targetMbtiId}
        />
        <div className="border border-outline-variant/40 rounded-2xl overflow-hidden">
          <RichContentViewer content={post.content} />
        </div>
      </article>
    </section>
  );
}

export default function AdminPostDetailContent({ id }: Props) {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<PostDetailSkeleton />}>
        <AdminPostDetailInner id={id} />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
