"use client";

import { useState } from "react";
import { LuChartBar, LuChevronDown, LuChevronUp } from "react-icons/lu";
import { useFunStats } from "@/hooks/useMbti";
import { getMbtiColor } from "@/constants/mbtiData";
import type { FunStatsResponse, FunOptionStats } from "@/types/mbti";
import { VIEW_MODE } from "@/types/post";
import type { ViewMode } from "@/types/post";
import ViewModeToggle from "@/components/common/ViewModeToggle";

const PREVIEW_COUNT = 3;
const LOAD_MORE_STEP = 3;

export default function FunStatsContent() {
  const { data: stats } = useFunStats();
  const [visibleCount, setVisibleCount] = useState(PREVIEW_COUNT);
  const [viewMode, setViewMode] = useState<ViewMode>(VIEW_MODE.LIST);

  const visible = stats.slice(0, visibleCount);
  const remaining = stats.length - visibleCount;

  return (
    <div className="flex flex-col gap-3 p-8">
      <header className="flex items-start justify-between gap-4 mb-1">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <LuChartBar className="w-5 h-5 text-primary" />
            <h1 className="text-xl font-bold text-foreground">Fun Question 통계</h1>
          </div>
          <p className="text-sm text-muted-foreground">모든 사용자의 응답을 MBTI별로 집계한 결과입니다.</p>
        </div>
        <ViewModeToggle viewMode={viewMode} onChange={setViewMode} />
      </header>

      {visible.map((question) => (
        <QuestionCard
          key={`${question.questionId}-${viewMode}`}
          question={question}
          viewMode={viewMode}
        />
      ))}

      {stats.length === 0 ? (
        <p className="py-16 text-center text-sm text-muted-foreground">등록된 질문이 없습니다.</p>
      ) : remaining > 0 ? (
        <button
          onClick={() => setVisibleCount((c) => c + LOAD_MORE_STEP)}
          className="mt-2 w-full rounded-xl border border-border py-3 text-sm text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
        >
          질문 더 보기 ({remaining}개)
        </button>
      ) : (
        <p className="py-4 text-center text-xs text-muted-foreground opacity-50">모든 질문을 불러왔습니다.</p>
      )}
    </div>
  );
}

function QuestionCard({
  question,
  viewMode,
}: {
  question: FunStatsResponse;
  viewMode: ViewMode;
}) {
  const totalResponses = question.options.reduce((sum, o) => sum + o.totalCount, 0);
  const isList = viewMode === VIEW_MODE.LIST;
  const [open, setOpen] = useState(false);

  return (
    <section className="bg-card rounded-2xl border border-border shadow-[0_4px_12px_rgba(0,0,0,0.05)] overflow-hidden">
      <div
        className={`flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-10 px-6 py-5 ${isList ? "cursor-pointer select-none hover:bg-accent/50 transition-colors" : ""}`}
        onClick={isList ? () => setOpen((v) => !v) : undefined}
      >

        {/* Questiomn Title */}
        <span className="flex-1 min-w-0 text-base font-semibold text-card-foreground leading-snug">
          {question.title}
        </span>

        {/* Total Partic */}
        <div className="flex items-center justify-between sm:justify-end gap-3 sm:shrink-0">
          <div className="text-left sm:text-right">
            <p className="text-[11px] text-muted-foreground">참여자</p>
            <p className="text-sm font-bold text-foreground tabular-nums">
              {totalResponses.toLocaleString()}명
            </p>
          </div>
          {isList && (
            open
              ? <LuChevronUp className="w-4 h-4 text-muted-foreground" />
              : <LuChevronDown className="w-4 h-4 text-muted-foreground" />
          )}
        </div>
      </div>

      {/* Detail Info */}
      {(!isList || open) && (
        <div className="px-6 pb-6 flex flex-col gap-5 border-t border-dashed border-border pt-5">
          {question.options.map((opt) => (
            <OptionBar key={opt.optionId} option={opt} totalResponses={totalResponses} />
          ))}
        </div>
      )}
    </section>
  );
}

function OptionBar({
  option,
  totalResponses,
}: {
  option: FunOptionStats;
  totalResponses: number;
}) {
  const [expanded, setExpanded] = useState(false);

  const pct = totalResponses > 0 ? Math.round((option.totalCount / totalResponses) * 100) : 0;

  const sorted = [...option.mbtiCounts].sort((a, b) => b.count - a.count);
  const barSegments = sorted.slice(0, 3);
  const barOther = option.totalCount - barSegments.reduce((s, m) => s + m.count, 0);

  const legendItems = expanded ? sorted : sorted.slice(0, 3);
  const hiddenCount = sorted.length - 3;

  return (
    <div>
      <div className="flex justify-between items-end mb-1.5">
        <span className="text-sm font-medium text-card-foreground">{option.optionText}</span>
        <span className="text-xs text-muted-foreground tabular-nums ml-3 shrink-0">
          {pct}% ({option.totalCount.toLocaleString()}명)
        </span>
      </div>

      <div className="h-3 bg-muted rounded-full overflow-hidden flex">
        {option.totalCount > 0 ? (
          <>
            {barSegments.map((m) => (
              <div
                key={m.mbtiId}
                className="h-full transition-all duration-700"
                style={{
                  width: `${(m.count / option.totalCount) * 100}%`,
                  backgroundColor: getMbtiColor(m.mbtiId),
                }}
                title={`${m.mbtiId}: ${m.count.toLocaleString()}명`}
              />
            ))}
            {barOther > 0 && (
              <div
                className="h-full bg-muted-foreground/25"
                style={{ width: `${(barOther / option.totalCount) * 100}%` }}
              />
            )}
          </>
        ) : null}
      </div>

      {sorted.length > 0 && (
        <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1">
          {legendItems.map((m) => (
            <div key={m.mbtiId} className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <div
                className="w-2 h-2 rounded-full shrink-0"
                style={{ backgroundColor: getMbtiColor(m.mbtiId) }}
              />
              <span className="font-medium text-card-foreground">{m.mbtiId}</span>
              <span className="tabular-nums">{m.count.toLocaleString()}명</span>
            </div>
          ))}
          {hiddenCount > 0 && (
            <button
              onClick={() => setExpanded((v) => !v)}
              className="ml-auto flex items-center gap-0.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
            >
              {expanded
                ? <><LuChevronUp className="w-3 h-3" /> 접기</>
                : <><LuChevronDown className="w-3 h-3" /> {hiddenCount}개 더 보기</>
              }
            </button>
          )}
        </div>
      )}
    </div>
  );
}
