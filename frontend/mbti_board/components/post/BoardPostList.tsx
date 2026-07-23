"use client";

import { useRef, useEffect } from "react";
import Link from "next/link";
import { useSuspensePostList } from "@/hooks/usePosts";
import PostCard from "@/components/post/PostCard";
import PostListItem from "@/components/post/PostListItem";
import PostCardSkeleton from "@/components/post/PostCardSkeleton";
import { VIEW_MODE } from "@/types/post";
import type { PostSortType, PostType, PostSummaryResponse, ViewMode } from "@/types/post";
import { MdArticle, MdEdit } from "react-icons/md";
import { postHref } from "@/lib/boardUtils";
import { BOARD_META } from "@/constants/navigation";
import AdSenseCard from "@/components/common/AdSenseCard";
import { AD_VARIANT, AD_EVERY, AD_ENABLED } from "@/constants/adSlots";

const FEED_ITEM_KIND = { POST: "post", AD: "ad" } as const;

type FeedItem =
  | { kind: typeof FEED_ITEM_KIND.POST; data: PostSummaryResponse }
  | { kind: typeof FEED_ITEM_KIND.AD; index: number };

function buildFeedItems(posts: PostSummaryResponse[]): FeedItem[] {
  const items: FeedItem[] = [];
  let adCount = 0;
  for (let i = 0; i < posts.length; i++) {
    if (AD_ENABLED && items.length === 1 + adCount * AD_EVERY) {
      items.push({ kind: FEED_ITEM_KIND.AD, index: adCount++ });
    }
    items.push({ kind: FEED_ITEM_KIND.POST, data: posts[i] });
  }
  return items;
}

interface Props {
  type: PostType | undefined;
  sort: PostSortType;
  authorMbti: string | undefined;
  viewMode: ViewMode;
}

export default function BoardPostList({ type, sort, authorMbti, viewMode }: Props) {
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

  const { writeHref } = BOARD_META[type ?? "ALL"];

  return (
    <section className="px-8 pb-24">
      {viewMode === VIEW_MODE.CARD ? (
        // 카드 보기 모드
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {feedItems.map((item) =>
            item.kind === FEED_ITEM_KIND.POST ? (
              <PostCard key={item.data.id} href={postHref(item.data.typeCode, item.data.id)} showType={!type} {...item.data} />
            ) : (
              <AdSenseCard key={`ad-${item.index}`} variant={AD_VARIANT.FLUID} />
            )
          )}
        </div>
      ) : (
        // 목록 보기 모드
        <div className="flex flex-col">
          {feedItems.map((item) =>
            item.kind === FEED_ITEM_KIND.POST ? (
              <PostListItem key={item.data.id} href={postHref(item.data.typeCode, item.data.id)} showType={!type} {...item.data} />
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
