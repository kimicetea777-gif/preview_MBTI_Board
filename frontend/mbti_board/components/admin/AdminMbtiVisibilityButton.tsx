"use client";

import { useChangeAdminMbtiQuestionVisibility } from "@/hooks/useAdmin";

interface Props {
  id: number;
  isVisible: boolean;
}

export default function AdminMbtiVisibilityButton({ id, isVisible }: Props) {
  const changeVisibility = useChangeAdminMbtiQuestionVisibility();

  return (
    <button
      onClick={() => changeVisibility.mutate({ id, visible: !isVisible })}
      disabled={changeVisibility.isPending}
      className={`rounded px-2 py-1 text-xs font-medium transition-colors disabled:opacity-40 ${
        isVisible
          ? "text-green-600 hover:bg-green-50"
          : "text-on-surface-variant hover:bg-surface-container-low"
      }`}
    >
      {isVisible ? "표시" : "숨김"}
    </button>
  );
}
