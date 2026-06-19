"use client";

import { MdGridView, MdList } from "react-icons/md";
import { VIEW_MODE } from "@/types/post";
import type { ViewMode } from "@/types/post";

export default function ViewModeToggle({
  viewMode,
  onChange,
}: {
  viewMode: ViewMode;
  onChange: (mode: ViewMode) => void;
}) {
  const isCard = viewMode === VIEW_MODE.CARD;

  return (
    <button
      type="button"
      onClick={() => onChange(isCard ? VIEW_MODE.LIST : VIEW_MODE.CARD)}
      className="flex items-center bg-surface-container px-1 py-1 rounded-full hover:bg-accent/60 transition-colors"
      title={isCard ? "리스트 보기로 전환" : "카드 보기로 전환"}
    >
      <span className={`px-2 py-1.5 rounded-full transition-all ${isCard ? "bg-card shadow-sm text-foreground" : "text-on-surface-variant"}`}>
        <MdGridView size={18} />
      </span>
      <span className={`px-2 py-1.5 rounded-full transition-all ${!isCard ? "bg-card shadow-sm text-foreground" : "text-on-surface-variant"}`}>
        <MdList size={18} />
      </span>
    </button>
  );
}
