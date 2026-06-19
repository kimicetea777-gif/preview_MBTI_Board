"use client";

import { useState } from "react";
import {
  Tooltip, Cell,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  AreaChart, Area, ResponsiveContainer,
} from "recharts";
import { getMbtiColor } from "@/constants/mbtiData";
import { MdPsychology, MdRepeat, MdTrendingUp, MdTrendingDown } from "react-icons/md";
import { useAdminMbtiTestStats } from "@/hooks/useAdmin";

function toKorean(mmdd: string) {
  const [m, d] = mmdd.split("-");
  return `${m}월 ${d}일`;
}

function formatTrendDate(date: string, mode: "daily" | "weekly") {
  if (mode === "weekly") {
    const [start, end] = date.split("~");
    const [, ed] = end.split("-");
    return `${toKorean(start)}~${ed}일`;
  }
  return toKorean(date);
}

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
  const [trendMode, setTrendMode] = useState<"daily" | "weekly">("daily");

  const growthDiff = data.thisMonthTests - data.lastMonthTests;
  const growthPct = data.lastMonthTests > 0
    ? ((Math.abs(growthDiff) / data.lastMonthTests) * 100).toFixed(1)
    : null;

  // avgScore > 0 → 앞 글자 우세, < 0 → 뒷 글자 우세
  // 표시용: 절댓값을 0~100 척도로 정규화 (점수 범위 -100~100 가정)
  const avgScores = {
    ei: data.avgEiScore,
    sn: data.avgSnScore,
    tf: data.avgTfScore,
    jp: data.avgJpScore,
  };

  const weeklyTrend = (() => {
    const result = [];
    for (let i = 0; i < data.testTrend.length; i += 7) {
      const chunk = data.testTrend.slice(i, i + 7);
      result.push({
        date: `${chunk[0].date}~${chunk[chunk.length - 1].date}`,
        value: chunk.reduce((sum, d) => sum + d.value, 0),
      });
    }
    return result;
  })();
  const trendData = trendMode === "weekly" ? weeklyTrend : data.testTrend;

  const resultTotal = data.byResult.reduce((s, e) => s + e.value, 0);
  const resultWithLabel = data.byResult.map((e) => ({
    ...e,
    label: resultTotal > 0 ? `${e.name} (${((e.value / resultTotal) * 100).toFixed(1)}%)` : e.name,
  }));

  const retakeTotal = data.byRetakeCount.reduce((s, e) => s + e.value, 0);

  return (
    <div className="p-6 max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">MBTI 검사 통계 데이터</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          검사 참여 현황과 결과 분포를 분석합니다.
        </p>
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
                {growthPct !== null && (
                  <span className={`inline-flex items-center gap-0.5 text-xs font-medium px-1.5 py-0.5 rounded-full ${
                    growthDiff >= 0
                      ? "bg-green-100 text-green-700"
                      : "bg-red-100 text-red-700"
                  }`}>
                    {growthDiff >= 0 ? <MdTrendingUp size={12} /> : <MdTrendingDown size={12} />}
                    {growthPct}%
                  </span>
                )}
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

        {/* 최근 30일 검사 추이 */}
        <div className="col-span-12 rounded-2xl border border-outline-variant bg-card p-6">
          <div className="flex items-center justify-between mb-4">
            <p className="text-base font-semibold text-foreground">최근 30일 검사 추이</p>
            <div className="flex gap-1 rounded-xl bg-surface-variant p-1">
              {(["daily", "weekly"] as const).map((m) => (
                <button
                  key={m}
                  onClick={() => setTrendMode(m)}
                  className={`px-3 py-1 text-xs rounded-lg transition-colors ${
                    trendMode === m
                      ? "bg-card text-foreground font-semibold shadow-sm"
                      : "text-on-surface-variant hover:text-foreground"
                  }`}
                >
                  {m === "daily" ? "일별" : "주별"}
                </button>
              ))}
            </div>
          </div>
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={trendData} margin={{ left: 0, right: 16, bottom: 8 }}>
              <defs>
                <linearGradient id="mbtiTestGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#5A3A7A" stopOpacity={0.25} />
                  <stop offset="95%" stopColor="#5A3A7A" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e4e2de" />
              <XAxis
                dataKey="date"
                tickFormatter={(v) => formatTrendDate(v, trendMode)}
                tick={{ fontSize: 11 }}
                stroke="#82746e"
                axisLine={false}
                tickLine={false}
                interval={trendMode === "daily" ? 4 : 0}
              />
              <YAxis
                allowDecimals={false}
                tick={{ fontSize: 11 }}
                stroke="#82746e"
                axisLine={false}
                tickLine={false}
                width={30}
              />
              <Tooltip
                labelFormatter={(v) => formatTrendDate(String(v), trendMode)}
                formatter={(v) => [typeof v === "number" ? v.toLocaleString() : v, "검사 횟수"]}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#5A3A7A"
                strokeWidth={2}
                fill="url(#mbtiTestGrad)"
                dot={false}
                activeDot={{ r: 4 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

      </div>

      {/* 검사 결과 MBTI 분포 */}
      {data.byResult.length > 0 && (
        <div className="rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-4">검사 결과 MBTI 분포</p>
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={resultWithLabel} layout="vertical" margin={{ left: 16, right: 24 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e4e2de" />
              <XAxis
                type="number"
                allowDecimals={false}
                tick={{ fontSize: 11 }}
                stroke="#82746e"
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                type="category"
                dataKey="label"
                width={110}
                tick={{ fontSize: 11 }}
                stroke="#82746e"
                axisLine={false}
                tickLine={false}
              />
              <Tooltip formatter={(v) => [typeof v === "number" ? v.toLocaleString() : v, "검사 횟수"]} />
              <Bar dataKey="value" name="검사 횟수" radius={[0, 4, 4, 0]}>
                {resultWithLabel.map((e) => (
                  <Cell key={e.name} fill={getMbtiColor(e.name)} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
