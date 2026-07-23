"use client";

import { useState } from "react";
import { LuTrash2, LuCirclePlus, LuCornerLeftUp, LuChevronDown, LuChevronUp } from "react-icons/lu";
import { VOTE_OPTION_MIN, VOTE_OPTION_MAX } from "@/types/post";
import type { OptionRow } from "@/hooks/useVoteOptionsForm";

interface MbtiItem {
  id: string;
}

interface Props {
  options: OptionRow[];
  isMbtiMode: boolean;
  mbtiList: MbtiItem[];
  isPending: boolean;
  onToggleMbtiMode: () => void;
  onAddOption: () => void;
  onRemoveOption: (index: number) => void;
  onUpdateOptionText: (index: number, value: string) => void;
}

export default function VoteOptionsField({
  options,
  isMbtiMode,
  mbtiList,
  isPending,
  onToggleMbtiMode,
  onAddOption,
  onRemoveOption,
  onUpdateOptionText,
}: Props) {
  const [isOptionsExpanded, setIsOptionsExpanded] = useState(false);
  const isCollapsed = isMbtiMode && !isOptionsExpanded;

  return (
    <fieldset className="border border-border rounded-xl p-5 space-y-4">
      <legend className="px-2 text-sm font-semibold text-foreground">투표 선택지</legend>

      <div className="flex flex-col items-start gap-1 -mt-4">
        <button
          type="button"
          onClick={onToggleMbtiMode}
          disabled={isPending || mbtiList.length < VOTE_OPTION_MIN}
          className={`px-4 py-1.5 rounded-full text-xs font-semibold border transition-all disabled:opacity-50 ${
            isMbtiMode
              ? "bg-primary text-primary-foreground border-primary shadow-sm"
              : "bg-background text-muted-foreground border-border hover:border-ring"
          }`}
        >
          MBTI 투표 {isMbtiMode ? "ON" : "OFF"}
        </button>
        {!isMbtiMode && (
          <span className="flex items-center gap-1 pl-2 text-xs text-muted-foreground">
            <LuCornerLeftUp className="size-3.5" />
            MBTI 유형별 선택지가 자동 생성됩니다.
          </span>
        )}
      </div>

      <div className={`relative ${isCollapsed ? "max-h-64 overflow-hidden sm:max-h-none sm:overflow-visible" : ""}`}>
        <ul
          className={`grid gap-2 ${
            options.length >= 7
              ? "grid-cols-1 sm:grid-cols-2 md:grid-cols-3"
              : options.length >= 5
              ? "grid-cols-1 sm:grid-cols-2"
              : "grid-cols-1"
          }`}
        >
          {/* 투표 입력한 선택지 확인 */}
          {options.map((option, index) => (
            <li key={index} className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground w-5 shrink-0 text-center">
                {index + 1}.
              </span>
              {isMbtiMode ? (
                <span className="flex-1 bg-muted border border-border rounded-lg px-4 py-2.5 text-sm text-foreground">
                  {option.optionText}
                </span>
              ) : (
                <input
                  type="text"
                  value={option.optionText}
                  onChange={(e) => onUpdateOptionText(index, e.target.value)}
                  maxLength={100}
                  placeholder={`항목 ${index + 1}`}
                  disabled={isPending}
                  className="max-w-full text-ellipsis bg-background border border-border rounded-lg px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none disabled:opacity-50 transition-all"
                />
              )}
              <button
                type="button"
                onClick={() => onRemoveOption(index)}
                disabled={isPending || options.length <= VOTE_OPTION_MIN}
                className="p-2 text-muted-foreground hover:text-destructive disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                aria-label="선택지 삭제"
              >
                <LuTrash2 className="size-4" />
              </button>
            </li>
          ))}
        </ul>

        {/* 일정 부분 가리기/보이기 - 투명 레이어*/}
        {isCollapsed && (
          <div className="absolute inset-x-0 bottom-0 h-10 bg-linear-to-t from-card to-transparent sm:hidden" />
        )}
      </div>

      {/* 일정 부분 가리기/보이기 버튼*/}
      {isMbtiMode && (
        <button
          type="button"
          onClick={() => setIsOptionsExpanded((prev) => !prev)}
          className="flex items-center gap-1 text-sm text-primary font-semibold hover:opacity-70 px-1 transition-opacity sm:hidden"
        >
          {isOptionsExpanded ? (
            <>
              <LuChevronUp className="size-4" />
              접기
            </>
          ) : (
            <>
              <LuChevronDown className="size-4" />
              모든 선택지 보기 ({options.length}개)
            </>
          )}
        </button>
      )}

      {!isMbtiMode && (
        <button
          type="button"
          onClick={onAddOption}
          disabled={isPending || options.length >= VOTE_OPTION_MAX}
          className="flex items-center gap-1.5 text-sm text-primary font-semibold hover:opacity-70 px-1 disabled:opacity-40 disabled:cursor-not-allowed transition-opacity"
        >
          <LuCirclePlus className="size-4" />
          항목 추가하기 ({options.length}/{VOTE_OPTION_MAX})
        </button>
      )}
    </fieldset>
  );
}
