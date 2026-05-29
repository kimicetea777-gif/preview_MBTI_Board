"use client";

import { useState, useRef, useEffect } from "react";
import { usePostList } from "@/hooks/usePosts";
import PostCard from "@/components/post/PostCard";
import PostListFilter from "@/components/post/PostListFilter";
import { ROUTES } from "@/lib/routes";
import { POST_SORT, POST_TYPE } from "@/types/post";
import type { PostSortType, PostSummaryResponse } from "@/types/post";

function PostCardSkeleton() {
  return (
    <div className="border-b border-gray-100 py-4 space-y-2 animate-pulse">
      <div className="h-4 bg-gray-200 rounded w-3/4" />
      <div className="h-3 bg-gray-200 rounded w-1/3" />
      <div className="h-3 bg-gray-200 rounded w-1/4" />
    </div>
  );
}

function getPostHref(post: PostSummaryResponse): string {
  if (post.typeCode === POST_TYPE.VOTE) return ROUTES.board.votePost(post.id);
  if (post.typeCode === POST_TYPE.DEBATE) return ROUTES.board.debatePost(post.id);
  return ROUTES.board.freePost(post.id);
}

export default function HomePage() {
  const [sort, setSort] = useState<PostSortType>(POST_SORT.LATEST);
  const [authorMbti, setAuthorMbti] = useState<string | undefined>();
  const [targetMbti, setTargetMbti] = useState<string | undefined>();

  const { data, isLoading, isError, fetchNextPage, hasNextPage, isFetchingNextPage } =
    usePostList({ sort, authorMbti, targetMbti });

  const posts = data?.pages.flatMap((page) => page.content) ?? [];

  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting && hasNextPage) fetchNextPage(); },
      { threshold: 0.1 }
    );
    if (sentinelRef.current) observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasNextPage, fetchNextPage]);

  return (
    <section className="max-w-2xl mx-auto px-4 py-6">
      <header className="mb-2">
        <h1 className="text-xl font-bold text-gray-900">전체 게시판</h1>
      </header>

      <PostListFilter
        sort={sort}
        authorMbti={authorMbti}
        targetMbti={targetMbti}
        onSortChange={setSort}
        onAuthorMbtiChange={setAuthorMbti}
        onTargetMbtiChange={setTargetMbti}
      />

      <article>
        {isLoading
          ? Array.from({ length: 5 }).map((_, i) => <PostCardSkeleton key={i} />)
          : posts.map((post) => (
              <PostCard
                key={post.id}
                href={getPostHref(post)}
                showType
                {...post}
              />
            ))
        }
      </article>

      <div ref={sentinelRef} />

      {isFetchingNextPage && (
        <div>
          {Array.from({ length: 3 }).map((_, i) => <PostCardSkeleton key={i} />)}
        </div>
      )}

      {isError && (
        <p className="py-8 text-center text-sm text-red-500">
          게시글을 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.
        </p>
      )}
      {!hasNextPage && posts.length > 0 && (
        <p className="py-6 text-center text-xs text-gray-400">모든 게시글을 불러왔습니다.</p>
      )}
      {!isLoading && !isError && posts.length === 0 && (
        <p className="py-16 text-center text-sm text-gray-400">아직 게시글이 없습니다.</p>
      )}
    </section>
  );
}
