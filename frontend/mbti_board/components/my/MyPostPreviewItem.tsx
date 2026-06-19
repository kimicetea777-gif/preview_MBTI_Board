import Link from "next/link";
import { MdBarChart, MdChatBubbleOutline } from "react-icons/md";
import PostTypeBadge from "@/components/common/PostTypeBadge";
import type { PostType } from "@/types/post";

interface Props {
  href: string;
  typeCode: PostType;
  title: string;
  timeLabel: string;
  stats?: { viewCount: number; commentCount: number };
}

export default function MyPostPreviewItem({ href, typeCode, title, timeLabel, stats }: Props) {
  return (
    <Link
      href={href}
      className="flex gap-4 p-4 bg-surface-container-low rounded-lg border border-outline-variant/10 transition-all hover:scale-[1.01]"
    >
      <div className="grow min-w-0">
        <div className="flex justify-between items-start gap-2">
          <PostTypeBadge type={typeCode} />
          <span className="text-xs text-on-surface-variant shrink-0">{timeLabel}</span>
        </div>
        <div className="flex justify-between gap-2 mt-1.5">
          <h4 className="text-sm font-bold text-on-surface line-clamp-1 ml-2">{title}</h4>
          {stats && (
            <div className="flex items-center gap-2 text-xs text-on-surface-variant">
              <span className="flex items-center gap-1">
                <MdBarChart size={13} />
                {stats.viewCount}
              </span>
              <span className="flex items-center gap-1">
                <MdChatBubbleOutline size={13} />
                {stats.commentCount}
              </span>
            </div>
          )}
        </div>
      </div>
    </Link>
  );
}
