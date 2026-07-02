"use client";

import { useState } from "react";
import { MdPsychology, MdRepeat } from "react-icons/md";
import { useAdminMbtiTestStats, useRefreshAdminMbtiStats } from "@/hooks/useAdmin";
import StatsRefreshButton from "@/components/admin/StatsRefreshButton";
import GrowthBadge from "@/components/admin/GrowthBadge";
import TrendAreaChart from "@/components/admin/TrendAreaChart";
import MbtiDistributionChart from "@/components/admin/MbtiDistributionChart";
import { chunkWeekly, computeGrowth, withPercentLabel, type TrendMode } from "@/lib/adminStatsUtils";

// EI/SN/TF/JP 각 지표에서 점수 > 0 이면 앞 글자, < 0 이면 뒷 글자가 우세
const DIMENSION_LABELS: { key: "ei" | "sn" | "tf" | "jp"; pos: string; neg: string; color: string }[] = [
  { key: "ei", pos: "E (외향)",  neg: "I (내향)", color: "#4A7C59" },
  { key: "sn", pos: "S (감각)",  neg: "N (직관)", color: "#2A5A7A" },
  { key: "tf", pos: "T (사고)",  neg: "F (감정)", color: "#7A3B1E" },
  { key: "jp", pos: "J (판단)",  neg: "P (인식)", color: "#5A3A7A" },
];

const RETAKE_COLORS = ["#4A7C59", "#2A5A7A", "#7A3B1E"];

export default function AdminMbtiTestStatsContent() {
  const { data } = useAdminMbtiTestStats();
  const { refresh, isRefreshing } = useRefreshAdminMbtiStats();
  const [trendMode, setTrendMode] = useState<TrendMode>("daily");

  const { diff: growthDiff, pct: growthPct } = computeGrowth(data.thisMonthTests, data.lastMonthTests);

  // avgScore > 0 → 앞 글자 우세, < 0 → 뒷 글자 우세
  // 표시용: 절댓값을 0~100 척도로 정규화 (점수 범위 -100~100 가정)
  const avgScores = {
    ei: data.avgEiScore,
    sn: data.avgSnScore,
    tf: data.avgTfScore,
    jp: data.avgJpScore,
  };

  const weeklyTrend = chunkWeekly(data.testTrend, (chunk) => ({
    value: chunk.reduce((sum, d) => sum + d.value, 0),
  }));
  const trendData = trendMode === "weekly" ? weeklyTrend : data.testTrend;

  const resultWithLabel = withPercentLabel(data.byResult);

  const retakeTotal = data.byRetakeCount.reduce((s, e) => s + e.value, 0);

  return (
    <div className="p-6 max-w-6xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">MBTI 검사 통계 데이터</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            검사 참여 현황과 결과 분포를 분석합니다.
          </p>
        </div>
        <StatsRefreshButton isRefreshing={isRefreshing} onRefresh={refresh} />
      </div>

      <div className="grid grid-cols-12 gap-5">

        {/* 총 검사 횟수 */}
        <div className="col-span-12 md:col-span-4 rounded-2xl border border-outline-variant bg-card p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <MdPsychology size={18} className="text-secondary" />
              <p className="text-sm text-on-surface-variant">누적 검사 횟수</p>
            </div>
            <p className="text-4xl font-bold text-foreground mt-1">
              {data.totalTests.toLocaleString()}
            </p>
          </div>
          <div className="mt-4 pt-4 border-t border-outline-variant grid grid-cols-2 divide-x divide-outline-variant">
            <div className="pr-4 min-w-0">
              <p className="text-xs text-on-surface-variant mb-1">이번달 검사</p>
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="text-xl font-semibold text-foreground">
                  {data.thisMonthTests.toLocaleString()}
                </span>
                <GrowthBadge diff={growthDiff} pct={growthPct} />
              </div>
            </div>
            <div className="pl-4">
              <p className="text-xs text-on-surface-variant mb-1">지난달 검사</p>
              <span className="text-xl font-semibold text-foreground">
                {data.lastMonthTests.toLocaleString()}
              </span>
            </div>
          </div>
        </div>

        {/* 지표별 평균 점수 */}
        <div className="col-span-12 md:col-span-4 rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-4">지표별 평균 성향</p>
          <div className="space-y-3">
            {DIMENSION_LABELS.map(({ key, pos, neg, color }) => {
              const score = avgScores[key];
              const dominant = score >= 0 ? pos : neg;
              // 0 중심으로 50% 기준점, 절댓값이 클수록 치우침
              const pct = Math.min(Math.round((Math.abs(score) / 16) * 100), 100);
              return (
                <div key={key}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-foreground font-medium">{dominant}</span>
                    <span className="text-on-surface-variant text-xs">{score.toFixed(1)}pt</span>
                  </div>
                  <div className="h-2 rounded-full bg-surface-variant overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700"
                      style={{ width: `${pct}%`, backgroundColor: color }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 재검사 빈도 */}
        <div className="col-span-12 md:col-span-4 rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-4">
            <span className="inline-flex items-center gap-1.5">
              <MdRepeat size={18} className="text-secondary" />
              재검사 빈도
            </span>
          </p>
          <div className="space-y-3">
            {data.byRetakeCount.map((e, i) => {
              const pct = retakeTotal > 0
                ? ((e.value / retakeTotal) * 100).toFixed(1)
                : "0";
              return (
                <div key={e.name}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-foreground font-medium">{e.name}</span>
                    <span className="text-on-surface-variant">
                      {e.value.toLocaleString()}명 ({pct}%)
                    </span>
                  </div>
                  <div className="h-2 rounded-full bg-surface-variant overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700"
                      style={{ width: `${pct}%`, backgroundColor: RETAKE_COLORS[i] ?? "#888" }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <TrendAreaChart
          title="최근 30일 검사 추이"
          data={trendData}
          trendMode={trendMode}
          onTrendModeChange={setTrendMode}
          color="#5A3A7A"
          gradientId="mbtiTestGrad"
          unitLabel="검사 횟수"
        />

      </div>

      {/* 검사 결과 MBTI 분포 */}
      <MbtiDistributionChart
        title="검사 결과 MBTI 분포"
        data={resultWithLabel}
        unitLabel="검사 횟수"
      />
    </div>
  );
}
