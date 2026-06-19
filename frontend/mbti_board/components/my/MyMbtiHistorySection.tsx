"use client";

import { Suspense } from "react";
import { LuFlaskConical } from "react-icons/lu";
import { getMbtiById } from "@/constants/mbtiData";
import { useMyMbtiHistorySuspense } from "@/hooks/useUser";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import { formatDate } from "@/lib/dateUtils";

const SCORE_DIMS = [
  { pos: "E", neg: "I", key: "eiScore" as const },
  { pos: "S", neg: "N", key: "snScore" as const },
  { pos: "T", neg: "F", key: "tfScore" as const },
  { pos: "J", neg: "P", key: "jpScore" as const },
];

function scoreLabel(val: number, pos: string, neg: string) {
  return val >= 0 ? `${pos}${val}` : `${neg}${Math.abs(val)}`;
}

function MyMbtiHistorySectionInner() {
  const { data: history } = useMyMbtiHistorySuspense();

  return (
    <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/20 shadow-sm">
      <div className="flex items-center gap-2 mb-4">
        <LuFlaskConical className="w-4 h-4 text-muted-foreground" />
        <h2 className="font-heading text-base font-bold text-foreground">MBTI 검사 이력</h2>
        <span className="ml-auto text-xs text-muted-foreground">{history.length}회</span>
      </div>

      {history.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-28 rounded-xl border border-dashed border-outline-variant/50 bg-surface-container-low">
          <LuFlaskConical className="w-6 h-6 text-outline-variant mb-2" />
          <p className="text-sm text-on-surface-variant">아직 검사 이력이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-1">
          {history.map((item, idx) => {
            const meta = getMbtiById(item.mbtiId);
            const color = meta?.colorCode ?? "#A8A29E";
            const scores = SCORE_DIMS.map(({ pos, neg, key }) =>
              scoreLabel(item[key], pos, neg)
            ).join(" · ");

            return (
              <div
                key={item.id}
                className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-surface-container-low transition-colors"
              >
                <span className="w-5 text-center text-xs text-muted-foreground shrink-0">
                  {idx + 1}
                </span>
                <span
                  className="shrink-0 text-xs font-bold px-2 py-0.5 rounded-md text-white"
                  style={{ backgroundColor: color }}
                >
                  {item.mbtiId}
                </span>
                <span className="flex-1 text-xs text-muted-foreground tracking-wide">
                  {scores}
                </span>
                <span className="shrink-0 text-xs text-muted-foreground">
                  {formatDate(item.createdAt)}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function MyMbtiHistorySection() {
  return (
    <QueryResetErrorBoundary>
      <Suspense
        fallback={
          <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/20 shadow-sm animate-pulse">
            <div className="flex items-center gap-2 mb-4">
              <div className="w-4 h-4 rounded bg-surface-container-low" />
              <div className="h-4 w-28 rounded bg-surface-container-low" />
            </div>
            <div className="space-y-2">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="h-10 rounded-lg bg-surface-container-low" />
              ))}
            </div>
          </div>
        }
      >
        <MyMbtiHistorySectionInner />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
