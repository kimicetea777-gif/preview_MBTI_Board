"use client";

import { useState } from "react";
import { FiX } from "react-icons/fi";
import { useCreateAdminMbtiQuestion } from "@/hooks/useAdmin";

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
  onClose: () => void;
}

export default function AdminMbtiQuestionCreateModal({ onClose }: Props) {
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState<Category>("EI");
  const [isReverse, setIsReverse] = useState(false);

  const createQuestion = useCreateAdminMbtiQuestion(onClose);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || createQuestion.isPending) return;
    createQuestion.mutate({ title, category, isReverse });
  }

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-xl border border-outline-variant bg-surface-container-lowest shadow-xl">
        <header className="flex items-center justify-between border-b border-outline-variant px-5 py-4">
          <span className="text-sm font-semibold text-on-surface">MBTI 문항 추가</span>
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
              placeholder="예: 나는 새로운 사람들과 만나는 자리를 즐긴다."
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
              <span className={`mt-1 inline-block w-10 rounded py-0.5 text-center text-xs font-semibold ${CATEGORY_STYLE[category]}`}>
                {category}
              </span>
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

          <p className="text-xs text-on-surface-variant">
            새 문항은 기본적으로 검사 출제에 노출됩니다. 카테고리·방향이 실제 채점 균형에 영향을 주니 신중히 입력해 주세요.
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
              disabled={!title.trim() || createQuestion.isPending}
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
