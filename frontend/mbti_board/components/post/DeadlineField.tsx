"use client";

import { LuCheck } from "react-icons/lu";
import { getMinDatetimeLocal } from "@/lib/dateUtils";

interface Props {
  endsAt: string;
  isNoDeadline: boolean;
  isPending: boolean;
  onEndsAtChange: (value: string) => void;
  onNoDeadlineToggle: () => void;
}

export default function DeadlineField({
  endsAt,
  isNoDeadline,
  isPending,
  onEndsAtChange,
  onNoDeadlineToggle,
}: Props) {
  return (
    <div className="space-y-1.5">
      <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide px-1">
        마감일
      </label>
      <div className="flex items-center gap-2 flex-wrap">

        {/* 마감일 입력창 */}
        <input
          type="datetime-local"
          value={endsAt}
          onChange={(e) => onEndsAtChange(e.target.value)}
          min={getMinDatetimeLocal()}
          disabled={isPending || isNoDeadline}
          className="bg-background border border-border rounded-lg px-4 py-2.5 text-sm text-foreground focus:border-ring focus:outline-none disabled:opacity-50 disabled:bg-muted transition-all"
        />

        {/* 기한 없음 Toggle */}
        <button
          type="button"
          onClick={onNoDeadlineToggle}
          disabled={isPending}
          className={`inline-flex items-center gap-1.5 px-4 py-2.5 rounded-full text-sm font-semibold border transition-all disabled:opacity-50 ${
            isNoDeadline
              ? "bg-primary/80 text-primary-foreground border-2 border-primary"
              : "bg-background text-muted-foreground border border-border hover:border-ring"
          }`}
        >
          {isNoDeadline && <LuCheck className="size-4" />}
          기한 없음
        </button>
      </div>
    </div>
  );
}
