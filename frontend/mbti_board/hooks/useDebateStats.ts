import { useMemo } from "react";
import { calcDebatePct } from "@/lib/debateUtils";
import type { DebateResult } from "@/types/post";

export function useDebateStats(debateResult: DebateResult | undefined) {
  return useMemo(() => {
    if (!debateResult) return { totalDebate: 0, proPct: 50, conPct: 50 };
    const { proPct, conPct } = calcDebatePct(debateResult.proCount, debateResult.conCount);
    return { totalDebate: debateResult.proCount + debateResult.conCount, proPct, conPct };
  }, [debateResult]);
}
