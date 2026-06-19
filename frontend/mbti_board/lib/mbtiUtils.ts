import type { MbtiTestScores, TotalQuestion } from "@/types/mbti";
import { MBTI_PENDING_SCORES_KEY } from "@/types/mbti";
import { sessionStorage } from "@/lib/storageUtils";

export const hasPendingScores = () => sessionStorage.has(MBTI_PENDING_SCORES_KEY);

// sessionStorage에서 꺼낸 값의 shape·범위 검증 (TypeScript as 단언은 런타임 보장 없음)
export function isValidScores(v: unknown): v is MbtiTestScores {
  if (!v || typeof v !== "object") return false;
  const s = v as Record<string, unknown>;
  return (
    typeof s.eiScore === "number" && s.eiScore >= -16 && s.eiScore <= 16 &&
    typeof s.snScore === "number" && s.snScore >= -16 && s.snScore <= 16 &&
    typeof s.tfScore === "number" && s.tfScore >= -16 && s.tfScore <= 16 &&
    typeof s.jpScore === "number" && s.jpScore >= -16 && s.jpScore <= 16
  );
}

export function computeScores(
  questions: TotalQuestion[],
  answers: Record<number, number>
): MbtiTestScores {
  let eiScore = 0, snScore = 0, tfScore = 0, jpScore = 0;
  for (const q of questions) {
    if (q.type !== "MBTI") continue;
    const sel = answers[q.id];
    if (sel === undefined) continue;
    const delta = q.isReverse ? -sel : sel;
    if      (q.category === "EI") eiScore += delta;
    else if (q.category === "SN") snScore += delta;
    else if (q.category === "TF") tfScore += delta;
    else if (q.category === "JP") jpScore += delta;
  }
  return { eiScore, snScore, tfScore, jpScore };
}

export function deriveMbtiId({ eiScore, snScore, tfScore, jpScore }: MbtiTestScores): string {
  return [
    eiScore >= 0 ? "E" : "I",
    snScore >= 0 ? "S" : "N",
    tfScore >= 0 ? "T" : "F",
    jpScore >= 0 ? "J" : "P",
  ].join("");
}
