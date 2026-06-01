import Link from "next/link";
import Image from "next/image";
import { MdPerson, MdVisibility, MdChatBubbleOutline } from "react-icons/md";
import MbtiBadge from "@/components/common/MbtiBadge";
import { formatRelativeTime } from "@/lib/dateUtils";
import { POST_TYPE, POST_TYPE_LABELS } from "@/types/post";
import type { PostSummaryResponse } from "@/types/post";

const TYPE_BADGE_COLORS: Record<string, string> = {
  FREE:   "bg-blue-100 text-blue-700",
  VOTE:   "bg-purple-100 text-purple-700",
  DEBATE: "bg-orange-100 text-orange-700",
};

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
  const total = proCount + conCount;
  const proPct = total === 0 ? 50 : Math.round((proCount / total) * 100);
  const conPct = 100 - proPct;

  return (
    <div className="mt-2 space-y-1">
      <div className="flex h-2 overflow-hidden rounded-full bg-gray-100">
        <div className={`${isClosed ? "bg-blue-300" : "bg-blue-400"} transition-all`} style={{ width: `${proPct}%` }} />
        <div className={`${isClosed ? "bg-orange-300" : "bg-orange-400"} transition-all`} style={{ width: `${conPct}%` }} />
      </div>
      <div className="flex justify-between text-xs text-gray-500">
        <span className="text-blue-600">{proLabel} {proPct}%</span>
        <span className="flex items-center gap-1 text-orange-500">
          {isClosed && <span className="text-gray-400">[마감]</span>}
          {conLabel} {conPct}%
        </span>
      </div>
    </div>
  );
}

function VoteOptionList({ options }: { options: { id: number; optionText: string }[] }) {
  return (
    <ul className="mt-2 space-y-1">
      {options.map((opt) => (
        <li
          key={opt.id}
          className="rounded-md bg-purple-50 px-2.5 py-1 text-xs text-purple-700 line-clamp-1"
        >
          {opt.optionText}
        </li>
      ))}
    </ul>
  );
}

export default function PostCard({ href, showType, card, ...post }: PostCardProps) {
  const thumbnail = post.imageUrls?.[0] ?? null;

  if (!card) {
    return (
      <article className="border-b border-border py-4 hover:bg-muted/40 transition-colors">
        <Link href={href} className="block">
          <div className="flex gap-3">
            <div className="flex-1 min-w-0 space-y-2">
              <div className="flex items-start justify-between gap-2">

                <div className="flex items-center gap-1.5 flex-1 min-w-0">
                  {showType && (
                    <span className={`shrink-0 text-xs rounded-full px-2 py-0.5 font-medium ${TYPE_BADGE_COLORS[post.typeCode] ?? "bg-gray-100 text-gray-600"}`}>
                      {POST_TYPE_LABELS[post.typeCode] ?? post.typeCode}
                    </span>
                  )}
                  <h2 className="text-sm font-medium text-gray-900 line-clamp-2">{post.title}</h2>
                </div>

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
              
              {post.contentPreview && (
                <p className="text-xs text-gray-500 line-clamp-2">{post.contentPreview}</p>
              )}
              {post.typeCode === POST_TYPE.DEBATE && post.debateSummary && (
                <DebateBar {...post.debateSummary} />
              )}
              {post.typeCode === POST_TYPE.VOTE && post.voteOptions && post.voteOptions.length > 0 && (
                <VoteOptionList options={post.voteOptions} />
              )}
              
              <div className="flex items-center gap-3 text-xs text-gray-400">
                <span>댓글 {post.commentCount}</span>
                <span>조회 {post.viewCount}</span>
              </div>
            </div>
            {thumbnail && (
              <div className="shrink-0 w-20 h-20 rounded-md overflow-hidden bg-gray-100">
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
      <article className="rounded-xl border border-outline-variant bg-card p-8 hover:border-secondary transition-all group cursor-pointer h-full flex flex-col">

        {/* Card Header */}
        <div className="flex items-start justify-between mb-6">
          <div className="flex items-center gap-3">
            <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} className="px-3 py-2 text-sm font-semibold" />
            <div>
              <div className="flex items-center gap-2">
                <p className="text-sm font-semibold text-foreground">{post.authorNickname}</p>
              </div>
              <p className="text-xs text-on-surface-variant mt-0.5">{formatRelativeTime(post.createdAt)}</p>
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
          <div className="mb-4 w-full rounded-md overflow-hidden bg-gray-100 aspect-video relative">
            <Image src={thumbnail} alt="" fill className="object-cover" />
          </div>
        )}

        {/* 통계 */}
        <div className="flex items-center justify-end gap-4 text-on-surface-variant mt-auto pt-2 mr-2">
          <div className="flex items-center gap-1.5">
            <MdVisibility size={16} />
            <span className="text-xs">{post.viewCount.toLocaleString()}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <MdChatBubbleOutline size={16} />
            <span className="text-xs">{post.commentCount}</span>
          </div>
        </div>

      </article>
    </Link>
  );
}
