"use client";

import { useState } from "react";
import { FiChevronDown, FiChevronUp } from "react-icons/fi";
import { useAdminMbtiQuestions, useAdminFunQuestions } from "@/hooks/useAdmin";
import AdminMbtiQuestionCreateModal from "@/components/admin/AdminMbtiQuestionCreateModal";
import AdminMbtiQuestionDetailModal from "@/components/admin/AdminMbtiQuestionDetailModal";
import AdminFunQuestionCreateModal from "@/components/admin/AdminFunQuestionCreateModal";
import AdminFunQuestionDetailModal from "@/components/admin/AdminFunQuestionDetailModal";
import AdminMbtiVisibilityButton from "@/components/admin/AdminMbtiVisibilityButton";
import AdminFunVisibilityButton from "@/components/admin/AdminFunVisibilityButton";
import { formatDate } from "@/lib/dateUtils";
import type { AdminMbtiQuestion, AdminFunQuestion } from "@/types/mbti";

const CATEGORY_STYLE: Record<"EI" | "SN" | "TF" | "JP", string> = {
  EI: "bg-blue-100 text-blue-700",
  SN: "bg-green-100 text-green-700",
  TF: "bg-purple-100 text-purple-700",
  JP: "bg-orange-100 text-orange-700",
};

// 기본 문항이 많아 FUN 문항이 가려지는 문제 때문에 처음엔 일부만 보여줌
const MBTI_PREVIEW_COUNT = 5;

export default function AdminMbtiContent() {
  const { data: mbtiQuestions } = useAdminMbtiQuestions();
  const { data: funQuestions } = useAdminFunQuestions();
  const [selected, setSelected] = useState<AdminMbtiQuestion | null>(null);
  const [selectedFun, setSelectedFun] = useState<AdminFunQuestion | null>(null);
  const [isMbtiExpanded, setIsMbtiExpanded] = useState(false);
  const [isCreatingMbti, setIsCreatingMbti] = useState(false);
  const [isCreatingFun, setIsCreatingFun] = useState(false);

  const mbtiHiddenCount = mbtiQuestions.filter((q) => !q.isVisible).length;
  const funHiddenCount  = funQuestions.filter((q) => !q.isVisible).length;

  const hasMoreMbti = mbtiQuestions.length > MBTI_PREVIEW_COUNT;
  const visibleMbtiQuestions = isMbtiExpanded ? mbtiQuestions : mbtiQuestions.slice(0, MBTI_PREVIEW_COUNT);

  return (
    <>
      <section className="flex flex-col gap-8">
        {/* MBTI 기본 문항 */}
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-foreground">
              MBTI 기본 문항{" "}
              <span className="text-sm font-normal text-on-surface-variant">
                ({mbtiQuestions.length}개
                {mbtiHiddenCount > 0 && <span className="text-destructive"> · 숨김 {mbtiHiddenCount}개</span>})
              </span>
            </h2>
            <button
              type="button"
              onClick={() => setIsCreatingMbti(true)}
              className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/80"
            >
              문항 추가
            </button>
          </div>
          <div className="overflow-x-auto rounded-lg border border-outline-variant">
            <table className="min-w-full text-sm">
              <thead className="bg-surface-container-low text-xs uppercase text-on-surface-variant whitespace-nowrap">
                <tr>
                  <th className="px-4 py-3 text-left">ID</th>
                  <th className="px-4 py-3 text-left">카테고리</th>
                  <th className="px-4 py-3 text-center">역방향</th>
                  <th className="px-4 py-3 text-left">문제</th>
                  <th className="px-4 py-3 text-center">노출</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant bg-surface-container-lowest">
                {visibleMbtiQuestions.map((q) => (
                  <tr
                    key={q.id}
                    onClick={() => setSelected(q)}
                    className={`cursor-pointer hover:bg-surface-container-low ${!q.isVisible ? "opacity-40" : ""}`}
                  >
                    <td className="px-4 py-3 text-on-surface-variant">{q.id}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-block w-10 rounded py-0.5 text-center text-xs font-semibold ${CATEGORY_STYLE[q.category]}`}>
                        {q.category}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      {q.isReverse ? (
                        <span className="text-xs font-medium text-orange-500">역방향</span>
                      ) : (
                        <span className="text-xs text-on-surface-variant">정방향</span>
                      )}
                    </td>
                    <td className="max-w-xs px-4 py-3 text-on-surface">
                      <p className="truncate">{q.title}</p>
                    </td>
                    <td className="px-4 py-3 text-center whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                      <AdminMbtiVisibilityButton id={q.id} isVisible={q.isVisible} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {hasMoreMbti && (
            <div className="mt-2 flex justify-center">
              <button
                type="button"
                onClick={() => setIsMbtiExpanded((prev) => !prev)}
                className="flex items-center gap-1 rounded-md border border-outline-variant px-3 py-1.5 text-xs text-on-surface-variant hover:bg-surface-container-low transition-colors"
              >
                {isMbtiExpanded ? (
                  <>접기 <FiChevronUp /></>
                ) : (
                  <>펼치기 ({mbtiQuestions.length - MBTI_PREVIEW_COUNT}개 더보기) <FiChevronDown /></>
                )}
              </button>
            </div>
          )}
        </div>

        {/* FUN 문항 */}
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-foreground">
              FUN 문항{" "}
              <span className="text-sm font-normal text-on-surface-variant">
                ({funQuestions.length}개
                {funHiddenCount > 0 && <span className="text-destructive"> · 숨김 {funHiddenCount}개</span>})
              </span>
            </h2>
            <button
              type="button"
              onClick={() => setIsCreatingFun(true)}
              className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/80"
            >
              문항 추가
            </button>
          </div>
          <div className="overflow-x-auto rounded-lg border border-outline-variant">
            <table className="min-w-full text-sm">
              <thead className="bg-surface-container-low text-xs uppercase text-on-surface-variant whitespace-nowrap">
                <tr>
                  <th className="px-4 py-3 text-left">ID</th>
                  <th className="px-4 py-3 text-left">문제</th>
                  <th className="px-4 py-3 text-right">선택지 수</th>
                  <th className="px-4 py-3 text-center">노출</th>
                  <th className="px-4 py-3 text-right">수정일</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant bg-surface-container-lowest">
                {funQuestions.map((q) => (
                  <tr
                    key={q.id}
                    onClick={() => setSelectedFun(q)}
                    className={`cursor-pointer hover:bg-surface-container-low ${!q.isVisible ? "opacity-40" : ""}`}
                  >
                    <td className="px-4 py-3 text-on-surface-variant">{q.id}</td>
                    <td className="max-w-xs px-4 py-3 text-on-surface">
                      <p className="truncate">{q.title}</p>
                    </td>
                    <td className="px-4 py-3 text-right text-on-surface-variant">{q.options.length}개</td>
                    <td className="px-4 py-3 text-center whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                      <AdminFunVisibilityButton id={q.id} isVisible={q.isVisible} />
                    </td>
                    <td className="px-4 py-3 text-right text-xs text-on-surface-variant whitespace-nowrap">
                      {formatDate(q.updatedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      {selected && (
        <AdminMbtiQuestionDetailModal
          question={selected}
          onClose={() => setSelected(null)}
        />
      )}

      {selectedFun && (
        <AdminFunQuestionDetailModal
          question={selectedFun}
          onClose={() => setSelectedFun(null)}
        />
      )}

      {isCreatingMbti && (
        <AdminMbtiQuestionCreateModal onClose={() => setIsCreatingMbti(false)} />
      )}

      {isCreatingFun && (
        <AdminFunQuestionCreateModal onClose={() => setIsCreatingFun(false)} />
      )}
    </>
  );
}
