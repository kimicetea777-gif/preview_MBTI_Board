import Link from "next/link";
import Image from "next/image";
import { MdChatBubbleOutline, MdBarChart, MdHowToVote, MdOutlineCheckCircle, MdPlayCircleOutline } from "react-icons/md";
import MbtiBadge from "@/components/common/MbtiBadge";
import { formatRelativeTime } from "@/lib/dateUtils";
import { POST_TYPE } from "@/types/post";
import { DebateBar, VoteOptionList } from "@/components/post/PostCardShared";
import type { PostCardBaseProps } from "@/components/post/PostCardShared";

const STAT_ROW_CLS = "flex items-center text-on-surface-variant mt-auto pt-3 mr-2 border-t-2 border-outline-variant/30";

function CommentCount({ count }: { count: number }) {
  return (
    <div className="flex items-center gap-1">
      <MdChatBubbleOutline size={16} />
      <span className="text-xs">{count.toLocaleString()}</span>
    </div>
  );
}

export default function PostCard({ href, ...post }: PostCardBaseProps) {
  const thumbnail = post.imageUrls?.[0] ?? null;

  return (
    <Link href={href} className="block h-full">
      <article className="rounded-xl border border-outline-variant bg-card p-6 hover:border-secondary transition-all group cursor-pointer h-full flex flex-col">

        {/* Card Header */}
        <div className="flex items-start justify-between mb-6">
          <div className="flex items-center gap-3 min-w-0">
            <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} className="px-3 py-1 text-sm font-semibold " />
            <div className="flex flex-col min-w-0">
              <p className="text-sm font-semibold text-foreground truncate">{post.authorNickname}</p>
              <p className="text-xs text-on-surface-variant">{formatRelativeTime(post.createdAt)}</p>
            </div>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            {post.targetMbtiId && (
              <div className="flex flex-col items-center border border-secondary rounded px-3 py-1 bg-secondary-container/20 min-w-14">
                <span className="text-[9px] font-bold tracking-widest text-on-surface-variant uppercase">wanted</span>
                <span className="text-sm font-bold text-on-secondary-container leading-tight">{post.targetMbtiId}</span>
              </div>
            )}
          </div>
        </div>

        {/* Content Title */}
        <h2 className="text-lg font-bold text-foreground mb-3 group-hover:text-surface-tint transition-colors leading-snug line-clamp-2">
          {post.title}
        </h2>

        {/* Content Preview (300자) */}
        {post.contentPreview && (
          <p className="text-sm text-on-surface-variant mb-4 line-clamp-3 leading-relaxed">
            {post.contentPreview}
          </p>
        )}

        {/* 토론 바 / 투표 선택지 */}
        {post.typeCode === POST_TYPE.DEBATE && post.debateSummary && (
          <div className="mb-4"><DebateBar {...post.debateSummary} /></div>
        )}
        {post.typeCode === POST_TYPE.VOTE && post.voteOptions && post.voteOptions.length > 0 && (
          <div className="mb-4"><VoteOptionList options={post.voteOptions} /></div>
        )}

        {/* 썸네일 */}
        {thumbnail && (
          <div className="mb-4 w-full rounded-md overflow-hidden bg-muted aspect-video relative">
            <Image src={thumbnail} alt="" fill sizes="(min-width: 1024px) 50vw, 100vw" className="object-cover" />
          </div>
        )}

        {/* 통계 */}
        {post.typeCode === POST_TYPE.VOTE ? (
          <div className={`${STAT_ROW_CLS} justify-between`}>
            <div className="flex items-center gap-1">
              <MdHowToVote size={16} />
              <span className="text-xs">{(post.voteOptions?.reduce((sum, o) => sum + o.voteCount, 0) ?? 0).toLocaleString()}명 참여</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1">
                <MdBarChart size={16} />
                <span className="text-xs">{post.viewCount.toLocaleString()}</span>
              </div>
              <CommentCount count={post.commentCount} />
            </div>
          </div>
        ) : post.typeCode === POST_TYPE.DEBATE ? (
          <div className={`${STAT_ROW_CLS} justify-between`}>
            <div className="flex items-center gap-1">
              {post.debateSummary?.isClosed ? (
                <MdOutlineCheckCircle size={16} className="text-muted-foreground" />
              ) : (
                <MdPlayCircleOutline size={16} className="text-chart-1" />
              )}
              <span className={`text-xs ${post.debateSummary?.isClosed ? "" : "text-chart-1"}`}>
                {post.debateSummary?.isClosed ? "종료" : "진행 중"}
              </span>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1">
                <MdBarChart size={16} />
                <span className="text-xs">{post.viewCount.toLocaleString()}</span>
              </div>
              <CommentCount count={post.commentCount} />
            </div>
          </div>
        ) : (
          <div className={`${STAT_ROW_CLS} justify-end gap-3`}>
            <div className="flex items-center gap-1">
              <MdBarChart size={16} />
              <span className="text-xs">{post.viewCount.toLocaleString()}</span>
            </div>
            <CommentCount count={post.commentCount} />
          </div>
        )}

      </article>
    </Link>
  );
}
