"use client";

import { useState, useRef, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { usePostList } from "@/hooks/usePosts";
import PostCard from "@/components/post/PostCard";
import { POST_SORT, SORT_OPTIONS } from "@/types/post";
import type { PostSortType, PostType, PostSummaryResponse } from "@/types/post";
import { MdGridView, MdList, MdPark, MdBolt, MdArticle, MdEdit, MdAdd, MdClose } from "react-icons/md";
import { BOARD_META, postHref } from "@/lib/boardUtils";
import { MBTI_GROUPS } from "@/types/mbti";

function PostCardSkeleton() {
  return (
    <div className="rounded-xl border border-outline-variant bg-card animate-pulse p-6">
      <div className="flex items-center gap-3 mb-4">
        <div className="rounded-full bg-muted w-10 h-10" />
        <div className="space-y-1.5 flex-1">
          <div className="h-3 bg-muted rounded w-24" />
          <div className="h-3 bg-muted rounded w-16" />
        </div>
        <div className="h-5 bg-muted rounded-full w-12" />
      </div>
      <div className="space-y-2">
        <div className="h-4 bg-muted rounded w-3/4" />
        <div className="h-3 bg-muted rounded w-full" />
        <div className="h-3 bg-muted rounded w-2/3" />
      </div>
    </div>
  );
}

/*--------------------------------------------------------------광고---------------------------------------------------------------- */
function AdPanelDailyInsight() {
  return (
    <div className="bg-primary-container text-on-primary-container p-6 rounded-xl relative overflow-hidden h-fit">
      <p className="text-xs font-semibold mb-3 opacity-70 uppercase tracking-wider">Daily Seed of Thought</p>
      <p className="text-sm italic leading-relaxed mb-3 opacity-60">광고 영역입니다</p>
      <div className="absolute -right-3 -bottom-3 opacity-10">
        <MdPark size={100} />
      </div>
    </div>
  );
}

function AdPanelGrowingDiscussions() {
  return (
    <div className="bg-surface-container-high p-6 rounded-xl">
      <h4 className="text-sm font-semibold text-foreground mb-4 flex items-center gap-2">
        <MdBolt size={18} />
        Growing Discussions
      </h4>
      <div className="flex flex-col gap-3">
        <p className="text-xs text-on-surface-variant text-center py-4 opacity-60">광고 영역입니다</p>
      </div>
    </div>
  );
}


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

function BoardPostList({ type }: { type: PostType | undefined }) {
  const [sort, setSort] = useState<PostSortType>(POST_SORT.LATEST);
  const [authorMbti, setAuthorMbti] = useState<string | undefined>();
  const [viewMode, setViewMode] = useState<"card" | "list">("card");
  const [fabOpen, setFabOpen] = useState(false);

  const { data, isLoading, isError, fetchNextPage, hasNextPage, isFetchingNextPage } =
    usePostList({ type, sort, authorMbti, targetMbti: undefined });

  const posts = data?.pages.flatMap((page) => page.content) ?? [];
  const feedItems = buildFeedItems(posts);
  const sentinelRef = useRef<HTMLDivElement>(null);

  /*----------------------------------------------Observer - 무한 스크롤 용----------------------------------------------- */
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
          <div className="flex items-center bg-surface-container px-1 py-1 rounded-full">
            <button
              type="button"
              onClick={() => setViewMode("card")}
              className={`px-2 py-1.5 rounded-full transition-all ${viewMode === "card" ? "bg-card shadow-sm text-foreground" : "text-on-surface-variant hover:text-foreground"}`}
              title="카드 보기"
            >
              <MdGridView size={18} />
            </button>
            <button
              type="button"
              onClick={() => setViewMode("list")}
              className={`px-2 py-1.5 rounded-full transition-all ${viewMode === "list" ? "bg-card shadow-sm text-foreground" : "text-on-surface-variant hover:text-foreground"}`}
              title="리스트 보기"
            >
              <MdList size={18} />
            </button>
          </div>
        </div>
      </div>

      {/*----------------------------------------------필터: MBTI----------------------------------------------- */}
      <div className="flex flex-wrap gap-2 mb-8">
        <button
          type="button"
          onClick={() => setAuthorMbti(undefined)}
          className={`px-4 py-1.5 rounded-full text-xs font-semibold transition-colors ${
            !authorMbti
              ? "bg-primary-container text-on-primary-container"
              : "border border-outline-variant text-on-surface-variant hover:border-primary"
          }`}
        >
          전체 유형
        </button>
        {MBTI_GROUPS.flatMap((g) => [...g.ids]).map((mbti) => (
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

      {isLoading ? (
        viewMode === "card" ? (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {Array.from({ length: 6 }).map((_, i) => <PostCardSkeleton key={i} />)}
          </div>
        ) : (
          <div className="flex flex-col">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="border-b border-border py-4 animate-pulse space-y-2">
                <div className="h-3 bg-muted rounded w-3/4" />
                <div className="h-3 bg-muted rounded w-1/2" />
              </div>
            ))}
          </div>
        )
      ) : viewMode === "card" ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {feedItems.map((item) =>
            item.kind === "post" ? (
              <PostCard key={item.data.id} href={postHref(item.data.typeCode, item.data.id)} showType={!type} card {...item.data} />
            ) : item.index % 2 === 0 ? (
              <AdPanelDailyInsight key={`ad-${item.index}`} />
            ) : (
              <AdPanelGrowingDiscussions key={`ad-${item.index}`} />
            )
          )}
        </div>
      ) : (
        <div className="flex flex-col">
          {posts.map((post) => (
            <PostCard key={post.id} href={postHref(post.typeCode, post.id)} showType={!type} {...post} />
          ))}
        </div>
      )}

      {isFetchingNextPage && (
        viewMode === "card" ? (
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

      {isError && (
        <p className="py-8 text-center text-sm text-destructive">
          게시글을 불러오는 중 오류가 발생했습니다.
        </p>
      )}
      {!isLoading && !isError && posts.length === 0 && (
        <div className="py-20 text-center">
          <MdArticle className="text-on-surface-variant opacity-30 block mb-3" size={48} />
          <p className="text-sm text-on-surface-variant">아직 게시글이 없습니다.</p>
        </div>
      )}
      {!hasNextPage && posts.length > 0 && (
        <p className="py-8 text-center text-xs text-on-surface-variant opacity-50">모든 게시글을 불러왔습니다.</p>
      )}

      {/* FAB */}
      <div className="fixed bottom-8 right-8 flex flex-col items-end gap-3 z-40">
        {fabOpen && (
          <Link
            href={writeHref}
            onClick={() => setFabOpen(false)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-full bg-card border border-outline-variant shadow-md text-sm font-medium text-foreground hover:bg-accent transition-colors"
          >
            <MdEdit size={18} />
            글쓰기
          </Link>
        )}
        <button
          type="button"
          onClick={() => setFabOpen((v) => !v)}
          className="w-14 h-14 rounded-2xl bg-secondary-container text-on-secondary-container shadow-lg flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
        >
          {fabOpen ? <MdClose size={28} /> : <MdAdd size={28} />}
        </button>
      </div>

    </section>
  );
}

function BoardPageContent() {
  const searchParams = useSearchParams();
  const rawType = searchParams.get("type");
  const type = rawType && rawType in BOARD_META ? (rawType as PostType) : undefined;
  return <BoardPostList key={type ?? "all"} type={type} />;
}

export default function BoardPage() {
  return (
    <Suspense>
      <BoardPageContent />
    </Suspense>
  );
}
