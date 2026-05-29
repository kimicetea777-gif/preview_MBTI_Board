import Link from "next/link";
import MbtiBadge from "@/components/common/MbtiBadge";
import { formatRelativeTime } from "@/lib/dateUtils";
import { POST_TYPE_LABELS } from "@/types/post";
import type { PostSummaryResponse } from "@/types/post";

const TYPE_BADGE_COLORS: Record<string, string> = {
  FREE:   "bg-blue-100 text-blue-700",
  VOTE:   "bg-purple-100 text-purple-700",
  DEBATE: "bg-orange-100 text-orange-700",
};

interface PostCardProps extends PostSummaryResponse {
  href: string;
  showType?: boolean;
}

export default function PostCard({ href, showType, ...post }: PostCardProps) {
  return (
    <article className="border-b border-gray-100 py-4 hover:bg-gray-50 transition-colors">
      <Link href={href} className="block space-y-2">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-1.5 flex-1 min-w-0">
            {showType && (
              <span className={`shrink-0 text-xs rounded-full px-2 py-0.5 font-medium ${TYPE_BADGE_COLORS[post.typeCode] ?? "bg-gray-100 text-gray-600"}`}>
                {POST_TYPE_LABELS[post.typeCode] ?? post.typeCode}
              </span>
            )}
            <h2 className="text-sm font-medium text-gray-900 line-clamp-2">
              {post.title}
            </h2>
          </div>
          {post.targetMbtiId && (
            <span className="shrink-0 text-xs rounded-full px-2 py-0.5 bg-gray-400 text-white font-medium">
              {post.targetMbtiId}에게
            </span>
          )}
        </div>

        {post.contentPreview && (
          <p className="text-xs text-gray-500 line-clamp-2">{post.contentPreview}</p>
        )}

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
