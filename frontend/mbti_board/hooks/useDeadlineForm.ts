import { useState } from "react";
import { toast } from "@/components/common/Toast";

/** VOTE/DEBATE 공용 마감일 입력 상태 — "기한 없음" 토글 및 마감일 유효성 검증 */
export function useDeadlineForm() {
  const [endsAt, setEndsAt] = useState("");
  const [isNoDeadline, setIsNoDeadline] = useState(false);

  const handleNoDeadlineToggle = () => {
    setIsNoDeadline((prev) => !prev);
    setEndsAt("");
  };

  const validateDeadline = (): boolean => {
    if (!isNoDeadline && !!endsAt && new Date(endsAt) <= new Date()) {
      toast.error("마감일은 현재 시각 이후여야 합니다.");
      return false;
    }
    return true;
  };

  return { endsAt, isNoDeadline, setEndsAt, handleNoDeadlineToggle, validateDeadline };
}
