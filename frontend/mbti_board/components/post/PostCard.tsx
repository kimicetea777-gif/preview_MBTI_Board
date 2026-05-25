import Link from "next/link";
import MbtiBadge from "@/components/common/MbtiBadge";
import { ROUTES } from "@/lib/routes";
import { formatRelativeTime } from "@/lib/dateUtils";
import type { PostSummaryResponse } from "@/types/post";

export default function PostCard(post: PostSummaryResponse) {
  return (
    <article className="border-b border-gray-100 py-4 hover:bg-gray-50 transition-colors">
      <Link href={ROUTES.board.freePost(post.id)} className="block space-y-2">
        <div className="flex items-start justify-between gap-2">
          <h2 className="text-sm font-medium text-gray-900 line-clamp-2 flex-1">
            {post.title}
          </h2>
          {post.targetMbtiId && (
            <span className="shrink-0 text-xs rounded-full px-2 py-0.5 bg-gray-400 text-white font-medium">
              {post.targetMbtiId}에게
            </span>
          )}
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-500">
          <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} />
          <span>{post.authorNickname}</span>
          <span>·</span>
          <span>{formatRelativeTime(post.createdAt)}</span>
        </div>

        <div className="flex items-center gap-3 text-xs text-gray-400">
          <span>댓글 {post.commentCount}</span>
          <span>조회 {post.viewCount}</span>
        </div>
      </Link>
    </article>
  );
}
