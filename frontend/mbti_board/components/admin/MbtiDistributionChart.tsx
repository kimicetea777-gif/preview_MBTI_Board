"use client";

import {
  Tooltip, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, ResponsiveContainer,
} from "recharts";
import { getMbtiColor } from "@/constants/mbtiData";
import { CHART_AXIS_COLOR, CHART_GRID_COLOR } from "@/lib/adminStatsUtils";

interface MbtiDistributionChartProps {
  title: string;
  data: { name: string; value: number; label: string }[];
  unitLabel: string;
}

export default function MbtiDistributionChart({ title, data, unitLabel }: MbtiDistributionChartProps) {
  if (data.length === 0) return null;

  return (
    <div className="rounded-2xl border border-outline-variant bg-card p-6">
      <p className="text-base font-semibold text-foreground mb-4">{title}</p>
      <ResponsiveContainer width="100%" height={320}>
        <BarChart data={data} layout="vertical" margin={{ left: 16, right: 24 }}>
          <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke={CHART_GRID_COLOR} />
          <XAxis
            type="number"
            allowDecimals={false}
            tick={{ fontSize: 11 }}
            stroke={CHART_AXIS_COLOR}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            type="category"
            dataKey="label"
            width={110}
            tick={{ fontSize: 11 }}
            stroke={CHART_AXIS_COLOR}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip formatter={(v) => [typeof v === "number" ? v.toLocaleString() : v, unitLabel]} />
          <Bar dataKey="value" name={unitLabel} radius={[0, 4, 4, 0]}>
            {data.map((e) => (
              <Cell key={e.name} fill={getMbtiColor(e.name)} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
