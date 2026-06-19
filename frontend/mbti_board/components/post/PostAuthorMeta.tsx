import MbtiBadge from "@/components/common/MbtiBadge";
import { formatDate } from "@/lib/dateUtils";
import type { MbtiVerified } from "@/types/user";
import { MdBarChart } from "react-icons/md";

interface Props {
  authorMbtiId: string;
  mbtiVerified: MbtiVerified;
  authorNickname: string;
  createdAt: string;
  viewCount: number;
}

export default function PostAuthorMeta({ authorMbtiId, mbtiVerified, authorNickname, createdAt, viewCount }: Props) {
  return (
    <div className="flex items-center justify-between border-b border-outline-variant pb-6">
      <div className="flex items-baseline gap-3">
        <MbtiBadge mbti={authorMbtiId} verified={mbtiVerified} />
        <p className="text-sm font-semibold text-primary">{authorNickname}</p>
        <time dateTime={createdAt} className="text-sm text-on-surface-variant">
          {formatDate(createdAt)}
        </time>
      </div>
      <span className="text-outline text-xs flex items-center gap-1.5 pr-10">
        <MdBarChart className="text-sm" />
        {viewCount.toLocaleString()}
      </span>
    </div>
  );
}
