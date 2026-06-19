"use client";

import { useState } from "react";
import { FiX } from "react-icons/fi";
import { useUpdateAdminMbtiQuestion, useChangeAdminMbtiQuestionVisibility } from "@/hooks/useAdmin";
import { formatDate } from "@/lib/dateUtils";
import type { AdminMbtiQuestion } from "@/types/mbti";

const CATEGORY_OPTIONS = ["EI", "SN", "TF", "JP"] as const;
type Category = typeof CATEGORY_OPTIONS[number];

const CATEGORY_STYLE: Record<Category, string> = {
  EI: "bg-blue-100 text-blue-700",
  SN: "bg-green-100 text-green-700",
  TF: "bg-purple-100 text-purple-700",
  JP: "bg-orange-100 text-orange-700",
};

const INPUT_CLS =
  "w-full rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none";

interface Props {
  question: AdminMbtiQuestion;
  onClose: () => void;
}

export default function AdminMbtiQuestionDetailModal({ question, onClose }: Props) {
  const [title, setTitle] = useState(question.title);
  const [category, setCategory] = useState<Category>(question.category);
  const [isReverse, setIsReverse] = useState(question.isReverse);

  const updateQuestion = useUpdateAdminMbtiQuestion(onClose);
  const changeVisibility = useChangeAdminMbtiQuestionVisibility();

  const isDirty =
    title !== question.title ||
    category !== question.category ||
    isReverse !== question.isReverse;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isDirty || updateQuestion.isPending) return;
    updateQuestion.mutate({ id: question.id, data: { title, category, isReverse } });
  }

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-xl border border-outline-variant bg-surface-container-lowest shadow-xl">
        <header className="flex items-center justify-between border-b border-outline-variant px-5 py-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-on-surface-variant">문항 #{question.id}</span>
            <span className={`inline-block w-10 rounded py-0.5 text-center text-xs font-semibold ${CATEGORY_STYLE[question.category]}`}>
              {question.category}
            </span>
            <span className={`rounded px-1.5 py-0.5 text-xs font-medium ${question.isVisible ? "bg-green-100 text-green-700" : "bg-surface-container-low text-on-surface-variant"}`}>
              {question.isVisible ? "표시" : "숨김"}
            </span>
          </div>
          <button
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-md text-on-surface-variant hover:bg-surface-container-low"
          >
            <FiX size={16} />
          </button>
        </header>

        <form onSubmit={handleSubmit} className="space-y-4 px-5 py-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">문제</label>
            <textarea
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              rows={3}
              maxLength={300}
              className={`${INPUT_CLS} resize-none`}
            />
            <p className="mt-1 text-right text-xs text-on-surface-variant">{title.length}/300</p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-medium text-on-surface-variant">카테고리</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value as Category)}
                className={INPUT_CLS}
              >
                {CATEGORY_OPTIONS.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-on-surface-variant">방향</label>
              <select
                value={isReverse ? "true" : "false"}
                onChange={(e) => setIsReverse(e.target.value === "true")}
                className={INPUT_CLS}
              >
                <option value="false">정방향</option>
                <option value="true">역방향</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between rounded-md border border-outline-variant px-3 py-2">
            <div>
              <p className="text-xs font-medium text-on-surface">검사 출제 노출</p>
              <p className="text-xs text-on-surface-variant">숨기면 무작위 출제에서 제외됩니다.</p>
            </div>
            <button
              type="button"
              onClick={() => changeVisibility.mutate({ id: question.id, visible: !question.isVisible })}
              disabled={changeVisibility.isPending}
              className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-40 ${
                question.isVisible
                  ? "bg-destructive/10 text-destructive hover:bg-destructive/20"
                  : "bg-green-100 text-green-700 hover:bg-green-200"
              }`}
            >
              {question.isVisible ? "숨기기" : "표시하기"}
            </button>
          </div>

          <p className="text-right text-xs text-on-surface-variant">
            마지막 수정: {formatDate(question.updatedAt)}
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
              disabled={!isDirty || updateQuestion.isPending}
              className="rounded-md bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/80 disabled:opacity-40"
            >
              {updateQuestion.isPending ? "저장 중..." : "저장"}
            </button>
          </footer>
        </form>
      </div>
    </>
  );
}
