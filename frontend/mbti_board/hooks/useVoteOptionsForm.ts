import { useState } from "react";
import { VOTE_OPTION_MIN, VOTE_OPTION_MAX } from "@/types/post";
import type { MbtiMetadata } from "@/types/mbti";

export interface OptionRow {
  optionText: string;
  mbtiTarget: string;
}

/** 투표 게시글 선택지 입력 상태 — 직접 입력 모드와 MBTI 16유형 고정 모드 토글 지원 */
export function useVoteOptionsForm(mbtiList: MbtiMetadata[]) {
  const [isMbtiMode, setIsMbtiMode] = useState(false);
  const [options, setOptions] = useState<OptionRow[]>([
    { optionText: "", mbtiTarget: "" },
    { optionText: "", mbtiTarget: "" },
  ]);

  const toggleMbtiMode = () => {
    if (!isMbtiMode) {
      const mbtiOptions = mbtiList.map((m) => ({ optionText: m.id, mbtiTarget: m.id }));
      setOptions(mbtiOptions.length >= VOTE_OPTION_MIN ? mbtiOptions : options);
    } else {
      setOptions([
        { optionText: "", mbtiTarget: "" },
        { optionText: "", mbtiTarget: "" },
      ]);
    }
    setIsMbtiMode((prev) => !prev);
  };

  const addOption = () => {
    if (options.length >= VOTE_OPTION_MAX) return;
    setOptions((prev) => [...prev, { optionText: "", mbtiTarget: "" }]);
  };

  const removeOption = (index: number) => {
    if (options.length <= VOTE_OPTION_MIN) return;
    setOptions((prev) => prev.filter((_, i) => i !== index));
  };

  const updateOptionText = (index: number, value: string) => {
    setOptions((prev) =>
      prev.map((o, i) => (i === index ? { ...o, optionText: value } : o))
    );
  };

  return { isMbtiMode, options, toggleMbtiMode, addOption, removeOption, updateOptionText };
}
