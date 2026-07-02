"use client";

import { useState } from "react";
import { MdArticle } from "react-icons/md";
import { useAdminPostStats, useRefreshAdminPostStats } from "@/hooks/useAdmin";
import StatsRefreshButton from "@/components/admin/StatsRefreshButton";
import GrowthBadge from "@/components/admin/GrowthBadge";
import TrendAreaChart from "@/components/admin/TrendAreaChart";
import MbtiDistributionChart from "@/components/admin/MbtiDistributionChart";
import { chunkWeekly, computeGrowth, withPercentLabel, type TrendMode } from "@/lib/adminStatsUtils";
import { POST_TYPE_LABELS } from "@/types/post";
import type { PostType } from "@/types/post";

const POST_TYPE_COLORS: Record<string, string> = {
  FREE:   "#4A7C59",
  VOTE:   "#2A5A7A",
  DEBATE: "#7A3B1E",
};

export default function AdminPostStatsContent() {
  const { data } = useAdminPostStats();
  const { refresh, isRefreshing } = useRefreshAdminPostStats();
  const [trendMode, setTrendMode] = useState<TrendMode>("daily");

  const { diff: growthDiff, pct: growthPct } = computeGrowth(data.thisMonthNewPosts, data.lastMonthNewPosts);

  const typeData = data.byType.map((e) => ({
    name: POST_TYPE_LABELS[e.name as PostType] ?? e.name,
    value: e.value,
    fill: POST_TYPE_COLORS[e.name] ?? "#888",
  }));
  const typeTotal = typeData.reduce((s, e) => s + e.value, 0);

  const weeklyTrend = chunkWeekly(data.postTrend, (chunk) => ({
    value: chunk.reduce((sum, d) => sum + d.value, 0),
  }));
  const trendData = trendMode === "weekly" ? weeklyTrend : data.postTrend;

  const mbtiDataWithLabel = withPercentLabel(data.byMbti);

  return (
    <div className="p-6 max-w-6xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">게시글 통계 데이터</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            게시글 작성 현황과 유형별 활동 패턴을 분석합니다.
          </p>
        </div>
        <StatsRefreshButton isRefreshing={isRefreshing} onRefresh={refresh} />
      </div>

      <div className="grid grid-cols-12 gap-5">

        {/* 총 게시글 수 */}
        <div className="col-span-12 md:col-span-6 rounded-2xl border border-outline-variant bg-card p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <MdArticle size={18} className="text-secondary" />
              <p className="text-sm text-on-surface-variant">총 게시글 수</p>
            </div>
            <p className="text-4xl font-bold text-foreground mt-1">
              {data.totalPosts.toLocaleString()}
            </p>
          </div>
          <div className="mt-4 pt-4 border-t border-outline-variant grid grid-cols-2 divide-x divide-outline-variant">
            <div className="pr-4">
              <p className="text-xs text-on-surface-variant mb-1">이번달 신규</p>
              <div className="flex items-center gap-1.5">
                <span className="text-xl font-semibold text-foreground">
                  {data.thisMonthNewPosts.toLocaleString()}
                </span>
                <GrowthBadge diff={growthDiff} pct={growthPct} />
              </div>
            </div>
            <div className="pl-4">
              <p className="text-xs text-on-surface-variant mb-1">지난달 신규</p>
              <span className="text-xl font-semibold text-foreground">
                {data.lastMonthNewPosts.toLocaleString()}
              </span>
            </div>
          </div>
        </div>

        {/* 게시판 타입 분포 */}
        <div className="col-span-12 md:col-span-6 rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-4">게시판 타입 분포</p>
          <div className="space-y-3">
            {typeData.map((e) => {
              const pct = typeTotal > 0 ? ((e.value / typeTotal) * 100).toFixed(1) : "0";
              return (
                <div key={e.name}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-foreground font-medium">{e.name}</span>
                    <span className="text-on-surface-variant">
                      {e.value.toLocaleString()} ({pct}%)
                    </span>
                  </div>
                  <div className="h-2 rounded-full bg-surface-variant overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700"
                      style={{ width: `${pct}%`, backgroundColor: e.fill }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <TrendAreaChart
          title="최근 30일 게시글 추이"
          data={trendData}
          trendMode={trendMode}
          onTrendModeChange={setTrendMode}
          color="#4A7C59"
          gradientId="postGrad"
          unitLabel="게시글 수"
        />

      </div>

      {/* MBTI별 게시글 분포 */}
      <MbtiDistributionChart
        title="MBTI별 게시글 작성 분포"
        data={mbtiDataWithLabel}
        unitLabel="게시글 수"
      />
    </div>
  );
}
