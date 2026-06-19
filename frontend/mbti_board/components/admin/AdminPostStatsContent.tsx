"use client";

import { useState } from "react";
import {
  Tooltip, Cell,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  AreaChart, Area, ResponsiveContainer,
} from "recharts";
import { getMbtiColor } from "@/constants/mbtiData";
import { MdArticle, MdTrendingUp, MdTrendingDown } from "react-icons/md";
import { useAdminPostStats } from "@/hooks/useAdmin";
import { POST_TYPE_LABELS } from "@/types/post";
import type { PostType } from "@/types/post";

const POST_TYPE_COLORS: Record<string, string> = {
  FREE:   "#4A7C59",
  VOTE:   "#2A5A7A",
  DEBATE: "#7A3B1E",
};

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

export default function AdminPostStatsContent() {
  const { data } = useAdminPostStats();
  const [trendMode, setTrendMode] = useState<"daily" | "weekly">("daily");

  const growthDiff = data.thisMonthNewPosts - data.lastMonthNewPosts;
  const growthPct = data.lastMonthNewPosts > 0
    ? ((Math.abs(growthDiff) / data.lastMonthNewPosts) * 100).toFixed(1)
    : null;

  const typeData = data.byType.map((e) => ({
    name: POST_TYPE_LABELS[e.name as PostType] ?? e.name,
    value: e.value,
    fill: POST_TYPE_COLORS[e.name] ?? "#888",
  }));
  const typeTotal = typeData.reduce((s, e) => s + e.value, 0);

  const weeklyTrend = (() => {
    const result = [];
    for (let i = 0; i < data.postTrend.length; i += 7) {
      const chunk = data.postTrend.slice(i, i + 7);
      result.push({
        date: `${chunk[0].date}~${chunk[chunk.length - 1].date}`,
        value: chunk.reduce((sum, d) => sum + d.value, 0),
      });
    }
    return result;
  })();
  const trendData = trendMode === "weekly" ? weeklyTrend : data.postTrend;

  const mbtiTotal = data.byMbti.reduce((s, e) => s + e.value, 0);
  const mbtiDataWithLabel = data.byMbti.map((e) => ({
    ...e,
    label: mbtiTotal > 0 ? `${e.name} (${((e.value / mbtiTotal) * 100).toFixed(1)}%)` : e.name,
  }));

  return (
    <div className="p-6 max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">게시글 통계 데이터</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          게시글 작성 현황과 유형별 활동 패턴을 분석합니다.
        </p>
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

        {/* 최근 30일 게시글 추이 */}
        <div className="col-span-12 rounded-2xl border border-outline-variant bg-card p-6">
          <div className="flex items-center justify-between mb-4">
            <p className="text-base font-semibold text-foreground">최근 30일 게시글 추이</p>
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
                <linearGradient id="postGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#4A7C59" stopOpacity={0.25} />
                  <stop offset="95%" stopColor="#4A7C59" stopOpacity={0} />
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
                formatter={(v) => [typeof v === "number" ? v.toLocaleString() : v, "게시글 수"]}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#4A7C59"
                strokeWidth={2}
                fill="url(#postGrad)"
                dot={false}
                activeDot={{ r: 4 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

      </div>

      {/* MBTI별 게시글 분포 */}
      {data.byMbti.length > 0 && (
        <div className="rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-4">MBTI별 게시글 작성 분포</p>
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={mbtiDataWithLabel} layout="vertical" margin={{ left: 16, right: 24 }}>
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
              <Tooltip formatter={(v) => [typeof v === "number" ? v.toLocaleString() : v, "게시글 수"]} />
              <Bar dataKey="value" name="게시글 수" radius={[0, 4, 4, 0]}>
                {mbtiDataWithLabel.map((e) => (
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
