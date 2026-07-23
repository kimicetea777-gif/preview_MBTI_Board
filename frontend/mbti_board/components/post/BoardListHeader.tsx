"use client";

import { SORT_OPTIONS } from "@/types/post";
import type { PostSortType, PostType, ViewMode } from "@/types/post";
import ViewModeToggle from "@/components/common/ViewModeToggle";
import MbtiSelect from "@/components/common/MbtiSelect";
import { BOARD_META } from "@/constants/navigation";

interface Props {
  type: PostType | undefined;
  sort: PostSortType;
  onSortChange: (sort: PostSortType) => void;
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
  authorMbti: string | undefined;
  onAuthorMbtiChange: (mbti: string | undefined) => void;
}

export default function BoardListHeader({
  type,
  sort,
  onSortChange,
  viewMode,
  onViewModeChange,
  authorMbti,
  onAuthorMbtiChange,
}: Props) {
  const { title, description } = BOARD_META[type ?? "ALL"];

  return (
    <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 px-8 pt-8 mb-8">
      {/*----------------------------------------------게시판 설명, 인사말----------------------------------------------- */}
      <div>
        <h1 className="text-3xl font-bold text-foreground tracking-tight mb-1">{title}</h1>
        <p className="text-base text-on-surface-variant">{description}</p>
      </div>
      <div className="flex flex-wrap items-center justify-between md:justify-end gap-2 shrink-0 w-full md:w-auto">
        {/*----------------------------------------------필터: 게시글 정렬 (최신순 인기순..)----------------------------------------------- */}
        <div className="flex items-center gap-1 bg-surface-container px-1 py-1 rounded-full w-full md:w-auto">
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => onSortChange(opt.value)}
              className={`flex-1 md:flex-none text-center px-4 py-1.5 rounded-full text-xs font-semibold transition-all ${
                sort === opt.value
                  ? "bg-card shadow-sm text-foreground"
                  : "text-on-surface-variant hover:text-foreground"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        {/*----------------------------------------------목록 보기 모드 (카드 보기, 리스트 보기)----------------------------------------------- */}
        <ViewModeToggle viewMode={viewMode} onChange={onViewModeChange} />

        {/* 넓은 화면 전용 줄바꿈: 정렬+뷰모드 다음 줄에 MBTI */}
        <div className="hidden md:block md:basis-full" />

        {/*----------------------------------------------필터: MBTI----------------------------------------------- */}
        <div className="h-9.5 flex items-center bg-surface-container p-1 rounded-full">
          <MbtiSelect
            value={authorMbti}
            onChange={onAuthorMbtiChange}
            placeholder="전체 유형"
            align="end"
            className="w-28 h-full rounded-full border-0 bg-card shadow-sm text-foreground font-semibold"
          />
        </div>
      </div>
    </div>
  );
}
