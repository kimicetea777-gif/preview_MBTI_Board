import Link from "next/link";
import Image from "next/image";
import { MdChatBubbleOutline, MdBarChart , MdHowToVote, MdOutlineCheckCircle, MdPlayCircleOutline } from "react-icons/md";
import MbtiBadge from "@/components/common/MbtiBadge";
import { formatRelativeTime } from "@/lib/dateUtils";
import { calcDebatePct } from "@/lib/debateUtils";
import DebateProgressBar from "@/components/post/DebateProgressBar";
import { POST_TYPE } from "@/types/post";
import type { PostSummaryResponse } from "@/types/post";
import PostTypeBadge from "@/components/common/PostTypeBadge";

interface PostCardProps extends PostSummaryResponse {
  href: string;
  showType?: boolean;
  card?: boolean;
}

function DebateBar({ proLabel, conLabel, proCount, conCount, isClosed }: {
  proLabel: string;
  conLabel: string;
  proCount: number;
  conCount: number;
  isClosed: boolean;
}) {
  const { proPct, conPct } = calcDebatePct(proCount, conCount);

  return (
    <div className="mt-2 space-y-1">
      <DebateProgressBar proPct={proPct} conPct={conPct} height="h-2" />
      <div className="flex justify-between text-xs text-on-surface-variant">
        <span className="text-chart-1">{proLabel} <span className="font-bold">{proPct}%</span></span>
        <span className="flex items-center gap-1 text-primary">
          {isClosed && <span className="text-muted-foreground">[마감]</span>}
          {conLabel} <span className="font-bold">{conPct}%</span>
        </span>
      </div>
    </div>
  );
}

const STAT_ROW_CLS = "flex items-center text-on-surface-variant mt-auto pt-3 mr-2 border-t-2 border-outline-variant/30";

function CommentCount({ count }: { count: number }) {
  return (
    <div className="flex items-center gap-1">
      <MdChatBubbleOutline size={16} />
      <span className="text-xs">{count.toLocaleString()}</span>
    </div>
  );
}

function VoteOptionList({ options }: { options: { id: number; optionText: string }[] }) {
  return (
    <ul className="mt-2 flex flex-wrap gap-1">
      {options.map((opt) => (
        <li
          key={opt.id}
          className="rounded-md bg-secondary-container/50 px-2.5 py-1 text-xs text-on-secondary-container truncate max-w-48 "
        >
          {opt.optionText}
        </li>
      ))}
    </ul>
  );
}

export default function PostCard({ href, showType, card, ...post }: PostCardProps) {
  const thumbnail = post.imageUrls?.[0] ?? null;
  if(thumbnail != null || thumbnail != undefined) console.log("thumbnail: "+thumbnail)
  if (!card) {
    return (
      <article className="border-b border-border py-4 hover:bg-muted/40 transition-colors">
        <Link href={href} className="block">
          <div className="flex gap-3">
            <div className="flex-1 min-w-0 space-y-2">
              <div className="flex items-start justify-between gap-2">

                <div className="flex items-center gap-1.5 flex-1 min-w-0">
                  {showType && (
                    <PostTypeBadge type={post.typeCode} />
                  )}
                  <h2 className="text-sm font-medium text-foreground line-clamp-2">{post.title}</h2>
                </div>
                <div className="flex items-center gap-2 text-xs text-on-surface-variant">
                  <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} />
                  <span>{post.authorNickname}</span>
                  <span>·</span>
                  <span>{formatRelativeTime(post.createdAt)}</span>
                </div>
              </div>
              
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
                <div className="flex items-center gap-3 text-xs text-muted-foreground">
                  <span>댓글 {post.commentCount}</span>
                  <span>조회 {post.viewCount}</span>
                </div>
                {post.targetMbtiId && (
                  <span className="shrink-0 text-xs rounded-full px-2 py-0.5 bg-outline text-background font-medium">
                    {post.targetMbtiId}에게
                  </span>
                )}
              </div>
            </div>
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

  return (
    <Link href={href} className="block h-full">
      <article className="rounded-xl border border-outline-variant bg-card p-6 hover:border-secondary transition-all group cursor-pointer h-full flex flex-col">

        {/* Card Header */}
        <div className="flex items-start justify-between mb-6">
          <div className="flex items-center gap-3">
            <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} className="px-3 py-1 text-sm font-semibold " />
            <div className="flex flex-col">
              <p className="text-sm font-semibold text-foreground">{post.authorNickname}</p>
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
            <Image src={thumbnail} alt="" fill className="object-cover" />
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
