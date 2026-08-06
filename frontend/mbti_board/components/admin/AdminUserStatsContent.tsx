"use client";

import { useState } from "react";
import {
  Tooltip, XAxis, YAxis, CartesianGrid,
  LineChart, Line, Legend,
  ResponsiveContainer,
} from "recharts";
import {
  MdGroup, MdTimerOff, MdTrendingUp, MdTrendingDown,
  MdPendingActions,
} from "react-icons/md";
import StatsRefreshButton from "@/components/admin/StatsRefreshButton";
import GrowthBadge from "@/components/admin/GrowthBadge";
import MbtiDistributionChart from "@/components/admin/MbtiDistributionChart";
import { useAdminUserStats, useRefreshAdminUserStats } from "@/hooks/useAdmin";
import { USER_STATE_LABELS, USER_STATE_COLORS, USER_ROLE_COLORS } from "@/types/admin";
import type { UserState } from "@/types/user";
import {
  CHART_AXIS_COLOR, CHART_GRID_COLOR, chunkWeekly, computeGrowth, formatTrendDate, withPercentLabel,
  type TrendMode,
} from "@/lib/adminStatsUtils";

const MBTI_VERIFIED_LABELS: Record<string, string> = {
  TESTED:  "검사 완료",
  SELF:    "직접 입력",
  DEFAULT: "미설정",
};
const MBTI_VERIFIED_COLORS: Record<string, string> = {
  TESTED:  "#31170a",
  SELF:    "#7d562d",
  DEFAULT: "#d4c3bc",
};

export default function AdminUserStatsContent() {
  const { data } = useAdminUserStats();
  const { refresh, isRefreshing } = useRefreshAdminUserStats();

  const stateData = data.byState.map((e) => ({
    name: USER_STATE_LABELS[e.name as UserState] ?? e.name,
    value: e.value,
    fill: USER_STATE_COLORS[e.name as UserState] ?? "#888",
  }));
  const stateTotal = stateData.reduce((s, e) => s + e.value, 0);

  const adminCount  = data.byRole.find((e) => e.name === "ADMIN")?.value  ?? 0;
  const testerCount = data.byRole.find((e) => e.name === "TESTER")?.value ?? 0;

  const mbtiVerifiedData = data.byMbtiVerified.map((e) => ({
    name: MBTI_VERIFIED_LABELS[e.name] ?? e.name,
    value: e.value,
    fill: MBTI_VERIFIED_COLORS[e.name] ?? "#888",
  }));
  const mbtiVerifiedTotal = mbtiVerifiedData.reduce((s, e) => s + e.value, 0);

  const loginTotal = data.socialLoginCount + data.localLoginCount;
  const socialPct = loginTotal > 0 ? Math.round((data.socialLoginCount / loginTotal) * 100) : 0;

  const inactivePct = data.totalUsers > 0
    ? ((data.inactiveCount / data.totalUsers) * 100).toFixed(1)
    : "0.0";

  const inactiveDiff = data.inactiveCount - data.lastMonthInactiveCount;

  const { diff: growthDiff, pct: growthPct } = computeGrowth(data.thisMonthNewUsers, data.lastMonthNewUsers);

  const [trendMode, setTrendMode] = useState<TrendMode>("daily");

  // 가입·탈퇴 날짜 합집합으로 병합
  const signupMap = new Map(data.signupTrend.map((e) => [e.date, e.value]));
  const withdrawalMap = new Map(data.withdrawalTrend.map((e) => [e.date, e.value]));
  const allDates = Array.from(
    new Set([...data.signupTrend.map((e) => e.date), ...data.withdrawalTrend.map((e) => e.date)])
  ).sort();
  const mergedTrend = allDates.map((date) => ({
    date,
    signups: signupMap.get(date) ?? 0,
    withdrawals: withdrawalMap.get(date) ?? 0,
  }));

  const weeklyMerged = chunkWeekly(mergedTrend, (chunk) => ({
    signups: chunk.reduce((sum, d) => sum + d.signups, 0),
    withdrawals: chunk.reduce((sum, d) => sum + d.withdrawals, 0),
  }));

  const trendData = trendMode === "weekly" ? weeklyMerged : mergedTrend;

  const mbtiDataWithLabel = withPercentLabel(data.byMbti);

  return (
    <div className="p-6 max-w-6xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">사용자 통계 데이터</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            플랫폼 사용자 성장세와 활동 패턴을 분석합니다.
          </p>
        </div>
        <StatsRefreshButton isRefreshing={isRefreshing} onRefresh={refresh} />
      </div>

      <div className="grid grid-cols-12 gap-5">

        {/* 총 유저 수 */}
        <div className="col-span-12 md:col-span-4 rounded-2xl border border-outline-variant bg-card p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <MdGroup size={18} className="text-secondary" />
              <p className="text-sm text-on-surface-variant">전체 사용자 수</p>
            </div>
            <p className="text-4xl font-bold text-foreground mt-1">
              {data.totalUsers.toLocaleString()}
            </p>
          </div>
          <div className="mt-4 pt-4 border-t border-outline-variant grid grid-cols-2 divide-x divide-outline-variant">
            <div className="pr-4">
              <p className="text-xs text-on-surface-variant mb-1">이번달 신규</p>
              <div className="flex items-center gap-1.5">
                <span className="text-xl font-semibold text-foreground">
                  {data.thisMonthNewUsers.toLocaleString()}
                </span>
                <GrowthBadge diff={growthDiff} pct={growthPct} />
              </div>
            </div>
            <div className="pl-4">
              <p className="text-xs text-on-surface-variant mb-1">지난달 신규</p>
              <span className="text-xl font-semibold text-foreground">
                {data.lastMonthNewUsers.toLocaleString()}
              </span>
            </div>
          </div>
        </div>

        {/* 장기 미접속 */}
        <div className="col-span-12 md:col-span-4 rounded-2xl border border-outline-variant bg-card p-6 flex flex-col justify-between">
          <div>
            <div className="flex justify-between items-start mb-4">
              <span className="inline-flex p-3 bg-outline-variant/20 rounded-2xl">
                <MdTimerOff size={22} className="text-on-surface-variant" />
              </span>
              {inactiveDiff !== 0 && (
                <span className={`flex items-center gap-1 text-xs font-bold ${inactiveDiff > 0 ? "text-red-500" : "text-emerald-600"}`}>
                  {inactiveDiff > 0
                    ? <MdTrendingUp size={16} />
                    : <MdTrendingDown size={16} />}
                  {inactiveDiff > 0 ? "+" : ""}{inactiveDiff.toLocaleString()}명
                </span>
              )}
            </div>
            <p className="text-sm text-on-surface-variant">장기 미접속 (90일+)</p>
            <p className="text-4xl font-bold text-foreground mt-1">
              {data.inactiveCount.toLocaleString()}
            </p>
          </div>
          <div className="mt-4 pt-4 border-t border-outline-variant">
            <p className="text-xs text-on-surface-variant">
              전체의 약{" "}
              <span className="font-bold text-foreground">{inactivePct}%</span>
            </p>
          </div>
        </div>

        {/* 로그인 유형 */}
        <div className="col-span-12 md:col-span-4 rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-5">로그인 유형 분석</p>
          <div className="h-3 w-full rounded-full overflow-hidden flex mb-5">
            <div className="h-full bg-primary transition-all" style={{ width: `${socialPct}%` }} />
            <div className="h-full bg-secondary transition-all" style={{ width: `${100 - socialPct}%` }} />
          </div>
          <div className="space-y-2.5 text-sm">
            <div className="flex justify-between items-center">
              <span className="flex items-center gap-2 text-on-surface-variant">
                <span className="w-2.5 h-2.5 rounded-full bg-primary shrink-0" />
                소셜 로그인
              </span>
              <span className="font-bold text-foreground">{socialPct}%</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="flex items-center gap-2 text-on-surface-variant">
                <span className="w-2.5 h-2.5 rounded-full bg-secondary shrink-0" />
                로컬 로그인
              </span>
              <span className="font-bold text-foreground">{100 - socialPct}%</span>
            </div>
          </div>
        </div>

        {/* 순 성장 추이 */}
        <div className="col-span-12 lg:col-span-8 rounded-2xl border border-outline-variant bg-card p-6">
          <div className="flex justify-between items-center mb-6">
            <p className="text-base font-semibold text-foreground">순 성장 추이</p>
            <div className="flex rounded-lg border border-outline-variant overflow-hidden text-xs font-medium">
              {(["daily", "weekly"] as const).map((mode) => (
                <button
                  key={mode}
                  onClick={() => setTrendMode(mode)}
                  className={`px-3 py-1.5 transition-colors ${
                    trendMode === mode
                      ? "bg-primary text-white"
                      : "text-on-surface-variant hover:bg-accent"
                  }`}
                >
                  {mode === "daily" ? "일별" : "주별"}
                </button>
              ))}
            </div>
          </div>
          {mergedTrend.length === 0 ? (
            <p className="text-sm text-on-surface-variant text-center py-14">데이터 없음</p>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={trendData} margin={{ left: 0, right: 16, top: 4, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID_COLOR} vertical={false} />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 11 }}
                  interval={trendMode === "weekly" ? 0 : 5}
                  tickFormatter={(v) => formatTrendDate(v, trendMode)}
                  stroke={CHART_AXIS_COLOR}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  allowDecimals={false}
                  tick={{ fontSize: 11 }}
                  stroke={CHART_AXIS_COLOR}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  labelFormatter={(v) => formatTrendDate(String(v), trendMode)}
                  formatter={(v, name) => [
                    typeof v === "number" ? v.toLocaleString() : v,
                    name === "signups" ? "신규 가입" : "탈퇴",
                  ]}
                />
                <Legend
                  iconType="circle"
                  iconSize={8}
                  wrapperStyle={{ fontSize: 11 }}
                  formatter={(value) => value === "signups" ? "신규 가입" : "탈퇴"}
                />
                <Line
                  type="monotone"
                  dataKey="signups"
                  stroke="#4A7C59"
                  strokeWidth={2.5}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
                <Line
                  type="monotone"
                  dataKey="withdrawals"
                  stroke="#9B2A2A"
                  strokeWidth={2.5}
                  dot={false}
                  activeDot={{ r: 4 }}
                  strokeDasharray="4 2"
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* 계정 상태 분포 */}
        <div className="col-span-12 lg:col-span-4 rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-5">계정 상태 분포</p>
          <div className="space-y-3">
            {stateData.map((e) => {
              const pct = stateTotal > 0 ? (e.value / stateTotal) * 100 : 0;
              return (
                <div key={e.name}>
                  <div className="flex justify-between items-center mb-1 text-xs">
                    <span className="flex items-center gap-1.5 text-on-surface-variant">
                      <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: e.fill }} />
                      {e.name}
                    </span>
                    <span className="font-semibold text-foreground">{pct.toFixed(1)}%</span>
                  </div>
                  <div className="h-2 w-full bg-outline-variant/20 rounded-full overflow-hidden">
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

        {/* 관리자 계정 현황 */}
        <div className="col-span-12 lg:col-span-6 rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-4">관리자 계정 현황</p>

          <div className="flex gap-3 mb-5">
            <div className="flex-1 rounded-xl border border-outline-variant p-3 text-center">
              <p className="text-xs text-on-surface-variant mb-1">어드민</p>
              <p className="text-2xl font-bold text-foreground">{adminCount.toLocaleString()}명</p>
            </div>
            <div className="flex-1 rounded-xl border border-outline-variant p-3 text-center">
              <p className="text-xs text-on-surface-variant mb-1">테스터</p>
              <p className="text-2xl font-bold text-foreground">{testerCount.toLocaleString()}명</p>
            </div>
          </div>

          <p className="text-xs text-on-surface-variant mb-2">최근 30일 신규 생성</p>
          {data.roleTrend.length === 0 ? (
            <div className="flex items-center justify-center h-20 rounded-xl bg-outline-variant/10">
              <p className="text-xs text-on-surface-variant">최근 30일간 변동 없음</p>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={130}>
              <LineChart data={data.roleTrend} margin={{ left: 0, right: 8, top: 4, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID_COLOR} vertical={false} />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 10 }}
                  interval="preserveStartEnd"
                  tickFormatter={(v) => formatTrendDate(v, "daily")}
                  stroke={CHART_AXIS_COLOR}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  allowDecimals={false}
                  tick={{ fontSize: 10 }}
                  stroke={CHART_AXIS_COLOR}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip formatter={(v) => typeof v === "number" ? v.toLocaleString() : v} />
                <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 11 }} />
                <Line type="monotone" dataKey="adminCount"  name="어드민" stroke={USER_ROLE_COLORS.ADMIN}  strokeWidth={2} dot={{ r: 3 }} activeDot={{ r: 5 }} />
                <Line type="monotone" dataKey="testerCount" name="테스터" stroke={USER_ROLE_COLORS.TESTER} strokeWidth={2} dot={{ r: 3 }} activeDot={{ r: 5 }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* MBTI 인증 현황 */}
        <div className="col-span-12 lg:col-span-6 rounded-2xl border border-outline-variant bg-card p-6">
          <p className="text-base font-semibold text-foreground mb-5">MBTI 인증 현황</p>
          <div className="space-y-5">
            {mbtiVerifiedData.map((e) => {
              const pct = mbtiVerifiedTotal > 0 ? (e.value / mbtiVerifiedTotal) * 100 : 0;
              return (
                <div key={e.name}>
                  <div className="flex justify-between mb-1.5 text-sm">
                    <span className="text-on-surface-variant">{e.name}</span>
                    <span className="font-bold text-foreground">{pct.toFixed(1)}%</span>
                  </div>
                  <div className="h-3 w-full bg-outline-variant/20 rounded-full overflow-hidden">
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

        {/* PENDING 장기 체류 */}
        <div className="col-span-12 md:col-span-6 rounded-2xl border border-amber-200 bg-amber-50/60 p-6 flex flex-col justify-between">
          <div>
            <div className="flex justify-between items-start mb-4">
              <span className="inline-flex p-3 bg-amber-100 rounded-2xl">
                <MdPendingActions size={22} className="text-amber-600" />
              </span>
              {data.longPendingCount > 0 && (
                <span className="text-xs font-semibold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full">
                  주의
                </span>
              )}
            </div>
            <p className="text-sm text-amber-700">PENDING 장기 체류 (7일+)</p>
            <p className="text-4xl font-bold text-amber-900 mt-1">
              {data.longPendingCount.toLocaleString()}
            </p>
          </div>
          <div className="mt-4 pt-4 border-t border-amber-200">
            <p className="text-xs text-amber-700">
              이메일 인증 대기 상태로 7일 이상 경과한 계정입니다.
              이메일 발송 문제가 있을 수 있습니다.
            </p>
          </div>
        </div>

      </div>

      {/* MBTI 유형 분포 (전체) */}
      <MbtiDistributionChart
        title="MBTI 유형 분포"
        data={mbtiDataWithLabel}
        unitLabel="유저 수"
      />
    </div>
  );
}
