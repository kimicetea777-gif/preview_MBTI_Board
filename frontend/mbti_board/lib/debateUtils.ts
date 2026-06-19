export function calcDebatePct(proCount: number, conCount: number): { proPct: number; conPct: number } {
  const total = proCount + conCount;
  const proPct = total === 0 ? 50 : Math.round((proCount / total) * 100);
  return { proPct, conPct: 100 - proPct };
}
