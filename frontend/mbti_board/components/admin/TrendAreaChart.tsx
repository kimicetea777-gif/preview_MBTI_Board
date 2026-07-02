"use client";

import {
  Tooltip, AreaChart, Area, XAxis, YAxis, CartesianGrid, ResponsiveContainer,
} from "recharts";
import { CHART_AXIS_COLOR, CHART_GRID_COLOR, formatTrendDate, type TrendMode } from "@/lib/adminStatsUtils";

interface TrendAreaChartProps {
  title: string;
  data: { date: string; value: number }[];
  trendMode: TrendMode;
  onTrendModeChange: (mode: TrendMode) => void;
  color: string;
  gradientId: string;
  unitLabel: string;
}

export default function TrendAreaChart({
  title, data, trendMode, onTrendModeChange, color, gradientId, unitLabel,
}: TrendAreaChartProps) {
  return (
    <div className="col-span-12 rounded-2xl border border-outline-variant bg-card p-6">
      <div className="flex items-center justify-between mb-4">
        <p className="text-base font-semibold text-foreground">{title}</p>
        <div className="flex gap-1 rounded-xl bg-surface-variant p-1">
          {(["daily", "weekly"] as const).map((m) => (
            <button
              key={m}
              onClick={() => onTrendModeChange(m)}
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
        <AreaChart data={data} margin={{ left: 0, right: 16, bottom: 8 }}>
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={color} stopOpacity={0.25} />
              <stop offset="95%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART_GRID_COLOR} />
          <XAxis
            dataKey="date"
            tickFormatter={(v) => formatTrendDate(v, trendMode)}
            tick={{ fontSize: 11 }}
            stroke={CHART_AXIS_COLOR}
            axisLine={false}
            tickLine={false}
            interval={trendMode === "daily" ? 4 : 0}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fontSize: 11 }}
            stroke={CHART_AXIS_COLOR}
            axisLine={false}
            tickLine={false}
            width={30}
          />
          <Tooltip
            labelFormatter={(v) => formatTrendDate(String(v), trendMode)}
            formatter={(v) => [typeof v === "number" ? v.toLocaleString() : v, unitLabel]}
          />
          <Area
            type="monotone"
            dataKey="value"
            stroke={color}
            strokeWidth={2}
            fill={`url(#${gradientId})`}
            dot={false}
            activeDot={{ r: 4 }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
