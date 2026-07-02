export type TrendMode = "daily" | "weekly";

export const CHART_AXIS_COLOR = "#82746e";
export const CHART_GRID_COLOR = "#e4e2de";

function toKorean(mmdd: string) {
  const [m, d] = mmdd.split("-");
  return `${m}월 ${d}일`;
}

export function formatTrendDate(date: string, mode: TrendMode) {
  if (mode === "weekly") {
    const [start, end] = date.split("~");
    const [, ed] = end.split("-");
    return `${toKorean(start)}~${ed}일`;
  }
  return toKorean(date);
}

// 일별 추이 데이터를 7일 단위로 묶어 주별 추이로 집계
export function chunkWeekly<T extends { date: string }>(
  data: T[],
  aggregate: (chunk: T[]) => Omit<T, "date">,
): T[] {
  const result: T[] = [];
  for (let i = 0; i < data.length; i += 7) {
    const chunk = data.slice(i, i + 7);
    result.push({
      date: `${chunk[0].date}~${chunk[chunk.length - 1].date}`,
      ...aggregate(chunk),
    } as T);
  }
  return result;
}

export function computeGrowth(current: number, previous: number) {
  const diff = current - previous;
  const pct = previous > 0 ? ((Math.abs(diff) / previous) * 100).toFixed(1) : null;
  return { diff, pct };
}

export function withPercentLabel<T extends { name: string; value: number }>(list: T[]) {
  const total = list.reduce((s, e) => s + e.value, 0);
  return list.map((e) => ({
    ...e,
    label: total > 0 ? `${e.name} (${((e.value / total) * 100).toFixed(1)}%)` : e.name,
  }));
}
