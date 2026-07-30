"use client";

import { useState } from "react";
import { FiX, FiPlus, FiTrash2 } from "react-icons/fi";
import { useCreateAdminFunQuestion } from "@/hooks/useAdmin";

const INPUT_CLS =
  "w-full rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none";

const MIN_OPTIONS = 2;

interface Props {
  onClose: () => void;
}

export default function AdminFunQuestionCreateModal({ onClose }: Props) {
  const [title, setTitle] = useState("");
  const [options, setOptions] = useState<string[]>(["", ""]);

  const createQuestion = useCreateAdminFunQuestion(onClose);

  const isValid = title.trim().length > 0 && options.every((o) => o.trim().length > 0);

  function handleOptionChange(index: number, text: string) {
    setOptions((prev) => prev.map((o, i) => (i === index ? text : o)));
  }

  function addOption() {
    setOptions((prev) => [...prev, ""]);
  }

  function removeOption(index: number) {
    setOptions((prev) => prev.filter((_, i) => i !== index));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isValid || createQuestion.isPending) return;
    createQuestion.mutate({
      title,
      options: options.map((optionText) => ({ optionText })),
    });
  }

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-xl border border-outline-variant bg-surface-container-lowest shadow-xl">
        <header className="flex items-center justify-between border-b border-outline-variant px-5 py-4">
          <span className="text-sm font-semibold text-on-surface">FUN 문항 추가</span>
          <button
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-md text-on-surface-variant hover:bg-surface-container-low"
          >
            <FiX size={16} />
          </button>
        </header>

        <form onSubmit={handleSubmit} className="space-y-4 px-5 py-4">
          {/* 문제 */}
          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">문제</label>
            <textarea
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              rows={3}
              maxLength={300}
              placeholder="예: 주말에 가장 하고 싶은 일은?"
              className={`${INPUT_CLS} resize-none`}
            />
            <p className="mt-1 text-right text-xs text-on-surface-variant">{title.length}/300</p>
          </div>

          {/* 선택지 */}
          <div>
            <div className="mb-2 flex items-center justify-between">
              <label className="block text-xs font-medium text-on-surface-variant">
                선택지 ({options.length}개, 최소 {MIN_OPTIONS}개)
              </label>
              <button
                type="button"
                onClick={addOption}
                className="flex items-center gap-1 text-xs text-primary hover:opacity-80"
              >
                <FiPlus size={14} /> 선택지 추가
              </button>
            </div>
            <div className="space-y-2">
              {options.map((optionText, i) => (
                <div key={i} className="flex items-center gap-2">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-surface-container-low text-xs font-medium text-on-surface-variant">
                    {i}
                  </span>
                  <input
                    value={optionText}
                    onChange={(e) => handleOptionChange(i, e.target.value)}
                    required
                    maxLength={100}
                    placeholder={`선택지 ${i + 1}`}
                    className={INPUT_CLS}
                  />
                  <button
                    type="button"
                    onClick={() => removeOption(i)}
                    disabled={options.length <= MIN_OPTIONS}
                    className="shrink-0 rounded-md p-1.5 text-on-surface-variant hover:bg-destructive/10 hover:text-destructive disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-on-surface-variant"
                  >
                    <FiTrash2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          </div>

          <p className="text-xs text-on-surface-variant">
            새 문항은 기본적으로 검사 출제에 노출됩니다. 등록 후에는 선택지 추가·삭제·순서 변경이 불가하니 신중히 입력해 주세요.
          </p>

          <footer className="flex justify-end gap-2 border-t border-outline-variant pt-4">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-outline-variant px-4 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container-low"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={!isValid || createQuestion.isPending}
              className="rounded-md bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/80 disabled:opacity-40"
            >
              {createQuestion.isPending ? "추가 중..." : "추가"}
            </button>
          </footer>
        </form>
      </div>
    </>
  );
}
