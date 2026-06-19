"use client";

import Link from "next/link";
import { MdHistory } from "react-icons/md";
import { getRecentlyViewed, type RecentlyViewedPost } from "@/lib/recentlyViewedPosts";
import { postHref } from "@/lib/boardUtils";
import { formatRelativeTime } from "@/lib/dateUtils";
import { useLocalStorageSnapshot } from "@/lib/localStorageUtils";
import PostTypeBadge from "@/components/common/PostTypeBadge";

const EMPTY: RecentlyViewedPost[] = [];

export default function MyRecentlyViewedPreview() {
  const posts = useLocalStorageSnapshot(getRecentlyViewed, EMPTY);

  return (
    <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/20 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <MdHistory className="w-4 h-4 text-muted-foreground" />
          <h2 className="font-heading text-base font-bold text-foreground">최근 조회한 게시글</h2>
        </div>
      </div>

      {posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-32 rounded-xl border border-dashed border-outline-variant/50 bg-surface-container-low">
          <MdHistory className="w-6 h-6 text-outline-variant mb-2" />
          <p className="text-sm text-on-surface-variant">최근 조회한 게시글이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-1.5 p-2 bg-surface-container-low rounded-lg border border-outline-variant/10">
          {posts.map((post) => (
            <Link
              key={post.id}
              href={postHref(post.typeCode, post.id)}
              className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-surface-container-low hover:scale-[1.01] transition-all"
            >
              <PostTypeBadge type={post.typeCode} />
              <span className="flex-1 text-sm text-on-surface truncate">{post.title}</span>
              <span className="shrink-0 text-xs text-on-surface-variant">{formatRelativeTime(post.viewedAt)}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
