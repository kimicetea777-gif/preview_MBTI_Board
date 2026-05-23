"use client";

import { useState } from "react";

export const MODAL_ACTION = { UPDATE: "update", DELETE: "delete" } as const;
export type ModalAction = (typeof MODAL_ACTION)[keyof typeof MODAL_ACTION];

const ACTION_LABEL: Record<ModalAction, string> = {
  update: "수정",
  delete: "삭제",
};

interface Props {
  open: boolean;
  action: ModalAction;
  onConfirm: (password: string) => void;
  onClose: () => void;
  isPending: boolean;
  error?: string;
}

export default function GuestPasswordModal({
  open,
  action,
  onConfirm,
  onClose,
  isPending,
  error,
}: Props) {
  const [password, setPassword] = useState("");

  if (!open) return null;

  const isPasswordValid = password.length >= 4;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isPasswordValid) return;
    onConfirm(password);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={onClose}
    >
      <section
        className="w-full max-w-sm rounded-xl bg-white p-6 shadow-lg space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-base font-semibold text-gray-900">
          (비회원) {ACTION_LABEL[action]} 확인
        </h2>
        <p className="text-sm text-gray-500">
          게시글 작성 시 입력한 비밀번호를 입력해 주세요.
        </p>

        <form onSubmit={handleSubmit} className="space-y-3">
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호 (4자 이상)"
            autoComplete="off"
            minLength={4}
            maxLength={72}
            disabled={isPending}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
          />
          {error && <p className="text-xs text-red-500">{error}</p>}

          <div className="flex gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="flex-1 rounded-lg border border-gray-300 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isPending || !isPasswordValid}
              className="flex-1 rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
            >
              {isPending ? "처리 중..." : ACTION_LABEL[action]}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
