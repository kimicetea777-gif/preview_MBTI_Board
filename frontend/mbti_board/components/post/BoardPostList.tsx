"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { useSuspensePostList } from "@/hooks/usePosts";
import PostCard from "@/components/post/PostCard";
import PostCardSkeleton from "@/components/post/PostCardSkeleton";
import { POST_SORT, SORT_OPTIONS, VIEW_MODE } from "@/types/post";
import type { PostSortType, PostType, PostSummaryResponse, ViewMode } from "@/types/post";
import { MdArticle, MdEdit } from "react-icons/md";
import ViewModeToggle from "@/components/common/ViewModeToggle";
import { postHref } from "@/lib/boardUtils";
import { BOARD_META } from "@/constants/navigation";
import { MBTI_16 } from "@/types/mbti";
import AdSenseCard from "@/components/common/AdSenseCard";
import { AD_VARIANT } from "@/constants/adSlots";

const AD_EVERY = 6;

type FeedItem =
  | { kind: "post"; data: PostSummaryResponse }
  | { kind: "ad"; index: number };

function buildFeedItems(posts: PostSummaryResponse[]): FeedItem[] {
  const items: FeedItem[] = [];
  let adCount = 0;
  for (let i = 0; i < posts.length; i++) {
    if (items.length === 1 + adCount * AD_EVERY) {
      items.push({ kind: "ad", index: adCount++ });
    }
    items.push({ kind: "post", data: posts[i] });
  }
  return items;
}

export default function BoardPostList({ type }: { type: PostType | undefined }) {
  const [sort, setSort] = useState<PostSortType>(POST_SORT.LATEST);
  const [authorMbti, setAuthorMbti] = useState<string | undefined>();
  const [viewMode, setViewMode] = useState<ViewMode>(VIEW_MODE.CARD);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useSuspensePostList({ type, sort, authorMbti, targetMbti: undefined });

  const posts = data.pages.flatMap((page) => page.content);
  const feedItems = buildFeedItems(posts);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting && hasNextPage) fetchNextPage(); },
      { threshold: 0.1 }
    );
    if (sentinelRef.current) observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasNextPage, fetchNextPage]);

  const { title, description, writeHref } = BOARD_META[type ?? "ALL"];

  return (
    <section className="p-8 pb-24">

      {/*----------------------------------------------게시판 설명, 인사말----------------------------------------------- */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-bold text-foreground tracking-tight mb-1">{title}</h1>
          <p className="text-base text-on-surface-variant">{description}</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {/*----------------------------------------------필터: 게시글 정렬 (최신순 인기순..)----------------------------------------------- */}
          <div className="flex items-center gap-1 bg-surface-container px-1 py-1 rounded-full">
            {SORT_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => setSort(opt.value)}
                className={`px-4 py-1.5 rounded-full text-xs font-semibold transition-all ${
                  sort === opt.value
                    ? "bg-card shadow-sm text-foreground"
                    : "text-on-surface-variant hover:text-foreground"
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
          {/*----------------------------------------------목록 보기 모드 (카드 보기, 리스트 보기)----------------------------------------------- */}
          <ViewModeToggle viewMode={viewMode} onChange={setViewMode} />
        </div>
      </div>

      {/*----------------------------------------------필터: MBTI----------------------------------------------- */}
      <div className="flex flex-wrap gap-2 mb-8">
        <button
          type="button"
          onClick={() => setAuthorMbti(undefined)}
          className={`px-4 py-1.5 rounded-full text-xs font-semibold transition-colors ${
            !authorMbti
              ? "bg-primary-container text-surface-variant"
              : "border border-outline-variant text-on-surface-variant hover:border-primary"
          }`}
        >
          전체 유형
        </button>
        {MBTI_16.map((mbti) => (
          <button
            key={mbti}
            type="button"
            onClick={() => setAuthorMbti(authorMbti === mbti ? undefined : mbti)}
            className={`px-4 py-1.5 rounded-full text-xs font-semibold transition-colors ${
              authorMbti === mbti
                ? "bg-secondary-container text-on-secondary-container"
                : "border border-outline-variant text-on-surface-variant hover:border-primary"
            }`}
          >
            {mbti}
          </button>
        ))}
      </div>

      {viewMode === VIEW_MODE.CARD ? (
        // 카드 보기 모드
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {feedItems.map((item) =>
            item.kind === "post" ? (
              <PostCard key={item.data.id} href={postHref(item.data.typeCode, item.data.id)} showType={!type} card {...item.data} />
            ) : (
              <AdSenseCard key={`ad-${item.index}`} variant={AD_VARIANT.FLUID} />
            )
          )}
        </div>
      ) : (
        // 목록 보기 모드
        <div className="flex flex-col">
          {feedItems.map((item) =>
            item.kind === "post" ? (
              <PostCard key={item.data.id} href={postHref(item.data.typeCode, item.data.id)} showType={!type} {...item.data} />
            ) : (
              <AdSenseCard key={`ad-${item.index}`} variant={AD_VARIANT.RECTANGLE} />
            )
          )}
        </div>
      )}

      {isFetchingNextPage && (
        viewMode === VIEW_MODE.CARD ? (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
            {Array.from({ length: 2 }).map((_, i) => <PostCardSkeleton key={i} />)}
          </div>
        ) : (
          <div className="border-b border-border py-4 animate-pulse space-y-2 mt-2">
            <div className="h-3 bg-muted rounded w-3/4" />
            <div className="h-3 bg-muted rounded w-1/2" />
          </div>
        )
      )}

      <div ref={sentinelRef} />

      {posts.length === 0 && (
        <div className="py-20 text-center">
          <MdArticle className="text-on-surface-variant opacity-30 block mb-3" size={48} />
          <p className="text-sm text-on-surface-variant">아직 게시글이 없습니다.</p>
        </div>
      )}
      {!hasNextPage && posts.length > 0 && (
        <p className="py-8 text-center text-xs text-on-surface-variant opacity-50">모든 게시글을 불러왔습니다.</p>
      )}

      <div className="fixed bottom-8 right-8 flex flex-col items-end gap-3 z-40">
        <Link
          href={writeHref}
          className="w-13 h-13 rounded-2xl bg-primary/60 text-primary-foreground shadow-lg flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
        >
          <MdEdit size={22} />
        </Link>
      </div>

    </section>
  );
}
