import { calcDebatePct } from "@/lib/debateUtils";
import DebateProgressBar from "@/components/post/DebateProgressBar";
import type { PostSummaryResponse } from "@/types/post";

export interface PostCardBaseProps extends PostSummaryResponse {
  href: string;
  showType?: boolean;
}

export function DebateBar({ proLabel, conLabel, proCount, conCount, isClosed }: {
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
      <div className="flex justify-between gap-2 text-xs text-on-surface-variant">
        <span className="text-chart-1 truncate min-w-0">{proLabel} <span className="font-bold">{proPct}%</span></span>
        <span className="flex items-center gap-1 text-primary shrink-0 min-w-0">
          {isClosed && <span className="text-muted-foreground">[마감]</span>}
          <span className="truncate">{conLabel}</span> <span className="font-bold shrink-0">{conPct}%</span>
        </span>
      </div>
    </div>
  );
}

export function VoteOptionList({ options }: { options: { id: number; optionText: string }[] }) {
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
