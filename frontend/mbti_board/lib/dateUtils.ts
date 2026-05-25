export function getDeadlineLabel(endsAt: string | null, isEnded: boolean): string {
  if (isEnded) return "투표 종료";
  if (!endsAt) return "기한 없음";
  const days = Math.ceil((new Date(endsAt).getTime() - Date.now()) / 86_400_000);
  if (days <= 0) return "오늘 마감";
  return `D-${days}`;
}

export function getMinDatetimeLocal(): string {
  const d = new Date(Date.now() + 60_000);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("ko-KR");
}

const TIME_THRESHOLDS: { limit: number; format: (diff: number) => string }[] = [
  { limit: 60_000,        format: ()  => "방금 전" },
  { limit: 3_600_000,     format: (d) => `${Math.floor(d / 60_000)}분 전` },
  { limit: 86_400_000,    format: (d) => `${Math.floor(d / 3_600_000)}시간 전` },
  { limit: 2_592_000_000, format: (d) => `${Math.floor(d / 86_400_000)}일 전` },
];

export function formatRelativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  return TIME_THRESHOLDS.find(({ limit }) => diff < limit)?.format(diff) ?? formatDate(iso);
}
