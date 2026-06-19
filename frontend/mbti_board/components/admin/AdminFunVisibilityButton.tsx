"use client";

import { useChangeAdminFunQuestionVisibility } from "@/hooks/useAdmin";

interface Props {
  id: number;
  isVisible: boolean;
}

export default function AdminFunVisibilityButton({ id, isVisible }: Props) {
  const changeVisibility = useChangeAdminFunQuestionVisibility();

  return (
    <button
      onClick={() => changeVisibility.mutate({ id, visible: !isVisible })}
      disabled={changeVisibility.isPending}
      className={`rounded px-2 py-1 text-xs font-medium transition-colors disabled:opacity-40 ${
        isVisible
          ? "text-green-600 bg-green-100 hover:bg-green-50"
          : "text-on-surface-variant bg-red-100 hover:bg-surface-container-low"
      }`}
    >
      {isVisible ? "표시" : "숨김"}
    </button>
  );
}
