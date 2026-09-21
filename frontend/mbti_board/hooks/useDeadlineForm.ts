import { useState } from "react";
import { toast } from "@/components/common/Toast";
import { getDefaultDeadline } from "@/lib/dateUtils";

/** VOTE/DEBATE 공용 마감일 입력 상태 — 기본값 3개월, "3개월" 버튼으로 재설정 및 유효성 검증 */
export function useDeadlineForm() {
  const [endsAt, setEndsAtRaw] = useState(getDefaultDeadline());
  const [isDefaultDeadline, setIsDefaultDeadline] = useState(true);

  const setEndsAt = (value: string) => {
    setEndsAtRaw(value);
    setIsDefaultDeadline(false);
  };

  const handleSetThreeMonths = () => {
    setEndsAtRaw(getDefaultDeadline());
    setIsDefaultDeadline(true);
  };

  const validateDeadline = (): boolean => {
    if (!!endsAt && new Date(endsAt) <= new Date()) {
      toast.error("마감일은 현재 시각 이후여야 합니다.");
      return false;
    }
    return true;
  };

  return { endsAt, isDefaultDeadline, setEndsAt, handleSetThreeMonths, validateDeadline };
}
