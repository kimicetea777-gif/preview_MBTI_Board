"use client";

import { useState } from "react";
import { MdComment, MdVisibilityOff } from "react-icons/md";
import { useAdminCommentStats, useRefreshAdminCommentStats } from "@/hooks/useAdmin";
import StatsRefreshButton from "@/components/admin/StatsRefreshButton";
import GrowthBadge from "@/components/admin/GrowthBadge";
import TrendAreaChart from "@/components/admin/TrendAreaChart";
import MbtiDistributionChart from "@/components/admin/MbtiDistributionChart";
import { chunkWeekly, computeGrowth, withPercentLabel, type TrendMode } from "@/lib/adminStatsUtils";

export default function AdminCommentStatsContent() {
  const { data } = useAdminCommentStats();
  const { refresh, isRefreshing } = useRefreshAdminCommentStats();
  const [trendMode, setTrendMode] = useState<TrendMode>("daily");

  const hiddenPct = data.totalComments > 0
    ? ((data.hiddenCount / data.totalComments) * 100).toFixed(1)
    : "0.0";

  const { diff: growthDiff, pct: growthPct } = computeGrowth(data.thisMonthNewComments, data.lastMonthNewComments);

  const weeklyTrend = chunkWeekly(data.commentTrend, (chunk) => ({
    value: chunk.reduce((sum, d) => sum + d.value, 0),
  }));
  const trendData = trendMode === "weekly" ? weeklyTrend : data.commentTrend;

  const mbtiDataWithLabel = withPercentLabel(data.byMbti);

  return (
    <div className="p-6 max-w-6xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">댓글 통계 데이터</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            댓글 작성 현황을 분석합니다.
          </p>
        </div>
        <StatsRefreshButton isRefreshing={isRefreshing} onRefresh={refresh} />
      </div>

      <div className="grid grid-cols-12 gap-5">

        {/* 총 댓글 수 */}
        <div className="col-span-12 md:col-span-6 rounded-2xl border border-outline-variant bg-card p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <MdComment size={18} className="text-secondary" />
              <p className="text-sm text-on-surface-variant">총 댓글 수</p>
            </div>
            <p className="text-4xl font-bold text-foreground mt-1">
              {data.totalComments.toLocaleString()}
            </p>
          </div>
          <div className="mt-4 pt-4 border-t border-outline-variant grid grid-cols-2 divide-x divide-outline-variant">
            <div className="pr-4">
              <p className="text-xs text-on-surface-variant mb-1">이번달 신규</p>
              <div className="flex items-center gap-1.5">
                <span className="text-xl font-semibold text-foreground">
                  {data.thisMonthNewComments.toLocaleString()}
                </span>
                <GrowthBadge diff={growthDiff} pct={growthPct} />
              </div>
            </div>
            <div className="pl-4">
              <p className="text-xs text-on-surface-variant mb-1">지난달 신규</p>
              <span className="text-xl font-semibold text-foreground">
                {data.lastMonthNewComments.toLocaleString()}
              </span>
            </div>
          </div>
        </div>

        {/* 숨겨진 댓글 */}
        <div className="col-span-12 md:col-span-6 rounded-2xl border border-outline-variant bg-card p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <MdVisibilityOff size={18} className="text-error" />
              <p className="text-sm text-on-surface-variant">숨겨진 댓글</p>
            </div>
            <p className="text-4xl font-bold text-foreground mt-1">
              {data.hiddenCount.toLocaleString()}
            </p>
          </div>
          <div className="mt-4 pt-4 border-t border-outline-variant">
            <p className="text-xs text-on-surface-variant mb-1">전체 대비</p>
            <span className="text-xl font-semibold text-foreground">{hiddenPct}%</span>
            <div className="mt-2 h-2 rounded-full bg-surface-variant overflow-hidden">
              <div
                className="h-full rounded-full bg-error/70 transition-all duration-700"
                style={{ width: `${Math.min(parseFloat(hiddenPct), 100)}%` }}
              />
            </div>
          </div>
        </div>

        <TrendAreaChart
          title="최근 30일 댓글 추이"
          data={trendData}
          trendMode={trendMode}
          onTrendModeChange={setTrendMode}
          color="#2A5A7A"
          gradientId="commentGrad"
          unitLabel="댓글 수"
        />

      </div>

      {/* MBTI별 댓글 분포 */}
      <MbtiDistributionChart
        title="MBTI별 댓글 작성 분포"
        data={mbtiDataWithLabel}
        unitLabel="댓글 수"
      />
    </div>
  );
}
