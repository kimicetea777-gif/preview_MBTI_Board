"use client";

import { MdTrendingUp, MdTrendingDown } from "react-icons/md";

interface GrowthBadgeProps {
  diff: number;
  pct: string | null;
}

export default function GrowthBadge({ diff, pct }: GrowthBadgeProps) {
  if (pct === null) return null;

  return (
    <span className={`inline-flex items-center gap-0.5 text-xs font-medium px-1.5 py-0.5 rounded-full ${
      diff >= 0
        ? "bg-green-100 text-green-700"
        : "bg-red-100 text-red-700"
    }`}>
      {diff >= 0 ? <MdTrendingUp size={12} /> : <MdTrendingDown size={12} />}
      {pct}%
    </span>
  );
}
