"use client";

import { Suspense } from "react";
import { useMyActivityStatsSuspense, useMyProfileSuspense } from "@/hooks/useUser";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import { formatDate, formatTime } from "@/lib/dateUtils";

function MyActivityStatsInner() {
  const { data: activityStats } = useMyActivityStatsSuspense();
  const { data: profile } = useMyProfileSuspense();

  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm">
      {/* <h3 className="font-heading text-base font-bold text-foreground mb-4">활동 통계</h3> */}
      <div className="flex flex-col gap-3">
        <div className="flex gap-3">
          {[
            { label: "게시글", value: activityStats.postCount.toLocaleString() },
            { label: "댓글",   value: activityStats.commentCount.toLocaleString() },
          ].map((stat) => (
            <div key={stat.label} className="flex-1 flex items-center justify-between gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <p className="text-[11px] text-muted-foreground uppercase tracking-wider">{stat.label}</p>
              <p className="text-sm font-semibold text-foreground mr-0.5">{stat.value}</p>
            </div>
          ))}
        </div>
        <div className="flex items-center justify-between gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
          <div>
            <p className="text-[11px] text-muted-foreground uppercase tracking-wider">최근 검사 MBTI</p>
            {activityStats.latestTestDate && (
              <p className="text-[11px] text-muted-foreground mt-0.5">{formatDate(activityStats.latestTestDate)}</p>
            )}
          </div>
          <p className="text-sm font-semibold text-foreground mr-3">{activityStats.latestTestMbtiId ?? "—"}</p>
        </div>
        <div className="flex items-center justify-between gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
          <p className="text-[11px] text-muted-foreground uppercase tracking-wider">마지막 접속</p>
          <p className="text-sm font-semibold text-foreground mr-3">
            {profile.lastLoginAt ? (
              <span className="flex flex-col items-end">
                <span>{formatTime(profile.lastLoginAt)}</span>
                <span className="text-xs text-muted-foreground font-normal">{formatDate(profile.lastLoginAt)}</span>
              </span>
            ) : "—"}
          </p>
        </div>
      </div>
    </div>
  );
}

export default function MyActivityStats() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={
        <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm animate-pulse">
          <div className="h-5 w-20 rounded bg-surface-container-low mb-4" />
          <div className="flex flex-col gap-3">
            <div className="flex gap-3">
              <div className="flex-1 h-12 rounded-xl bg-surface-container-low" />
              <div className="flex-1 h-12 rounded-xl bg-surface-container-low" />
            </div>
            <div className="h-12 rounded-xl bg-surface-container-low" />
          </div>
        </div>
      }>
        <MyActivityStatsInner />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
