"use client";

import { MdRefresh } from "react-icons/md";

interface Props {
  isRefreshing: boolean;
  onRefresh: () => void;
}

export default function StatsRefreshButton({ isRefreshing, onRefresh }: Props) {
  return (
    <button
      onClick={onRefresh}
      disabled={isRefreshing}
      className="shrink-0 inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-outline-variant bg-card text-sm font-medium text-on-surface-variant hover:bg-accent transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
    >
      <MdRefresh size={16} className={isRefreshing ? "animate-spin" : ""} />
      {isRefreshing ? "갱신 중..." : "캐시 갱신"}
    </button>
  );
}
