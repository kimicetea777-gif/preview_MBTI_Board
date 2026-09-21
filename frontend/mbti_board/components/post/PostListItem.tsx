import Link from "next/link";
import Image from "next/image";
import MbtiBadge from "@/components/common/MbtiBadge";
import { formatRelativeTime } from "@/lib/dateUtils";
import { POST_TYPE } from "@/types/post";
import { DebateBar, VoteOptionList } from "@/components/post/PostCardShared";
import type { PostCardBaseProps } from "@/components/post/PostCardShared";
import PostTypeBadge from "@/components/common/PostTypeBadge";

export default function PostListItem({ href, showType, ...post }: PostCardBaseProps) {
  const thumbnail = post.imageUrls?.[0] ?? null;

  return (
    <article className="border-b border-border py-4 hover:bg-muted/40 transition-colors">
      <Link href={href} className="block">
        <div className="flex gap-3">
          <div className="flex-1 min-w-0 space-y-2">
            <div className="flex flex-col md:flex-row md:items-start justify-between gap-2">
              {/* Post */}
              <div className="flex items-center gap-1.5 flex-1 min-w-0">
                {showType && (
                  <PostTypeBadge type={post.typeCode} />
                )}
                <h2 className="text-sm font-medium text-foreground line-clamp-2">{post.title}</h2>
              </div>

              {/* Author */}
              <div className="flex items-center gap-2 text-xs text-on-surface-variant min-w-0">
                <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} />
                <span className="truncate max-w-32">{post.authorNickname}</span>
                <span className="hidden shrink-0">·</span>
                <span className="hidden shrink-0">{formatRelativeTime(post.createdAt)}</span>
              </div>
            </div>

            {/* contentPreview */}
            {post.contentPreview && (
              <p className="text-xs text-on-surface-variant line-clamp-2">{post.contentPreview}</p>
            )}
            {post.typeCode === POST_TYPE.DEBATE && post.debateSummary && (
              <DebateBar {...post.debateSummary} />
            )}
            {post.typeCode === POST_TYPE.VOTE && post.voteOptions && post.voteOptions.length > 0 && (
              <VoteOptionList options={post.voteOptions} />
            )}

            
            <div className="flex justify-between gap-3">

              {/* etc info */}
              <div className="flex items-center gap-3 text-xs text-muted-foreground">
                <span>댓글 {post.commentCount}</span>
                <span>조회 {post.viewCount}</span>
              </div>

              {/* target Mbti */}
              {post.targetMbtiId && (
                <span className="shrink-0 text-xs rounded-full px-2 py-0.5 bg-outline text-background font-medium">
                  {post.targetMbtiId}에게
                </span>
              )}
            </div>
          </div>

          {/* thumbnail */}
          {thumbnail && (
            <div className="shrink-0 w-20 h-20 rounded-md overflow-hidden bg-muted">
              <Image src={thumbnail} alt="" width={80} height={80} className="w-full h-full object-cover" />
            </div>
          )}
        </div>
      </Link>
    </article>
  );
}
