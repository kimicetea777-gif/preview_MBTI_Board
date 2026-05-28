"use client";

import { useState } from "react";
import { useAdminComments, useChangeCommentVisibility } from "@/hooks/useAdmin";
import type { AxiosError } from "axios";

const PAGE_SIZE = 20;

export default function AdminCommentTable() {
  const [postIdInput, setPostIdInput] = useState("");
  const [appliedPostId, setAppliedPostId] = useState<number | undefined>();
  const [visibleFilter, setVisibleFilter] = useState<"" | "true" | "false">("");
  const [page, setPage] = useState(0);

  const params = {
    postId: appliedPostId,
    isVisible: visibleFilter === "" ? undefined : visibleFilter === "true",
    page,
    size: PAGE_SIZE,
  };

  const { data, isLoading, isError, error } = useAdminComments(params);
  const changeVisibility = useChangeCommentVisibility();

  function handleFilterSubmit(e: React.FormEvent) {
    e.preventDefault();
    const num = postIdInput ? Number(postIdInput) : undefined;
    setAppliedPostId(num !== undefined && !isNaN(num) ? num : undefined);
    setPage(0);
  }

  const is403 = isError && (error as AxiosError)?.response?.status === 403;

  if (isLoading) return <p className="py-8 text-center text-gray-500">불러오는 중...</p>;
  if (is403) return <p className="py-8 text-center text-red-500">관리자 권한이 없습니다.</p>;
  if (isError) return <p className="py-8 text-center text-red-500">데이터를 불러올 수 없습니다.</p>;

  const comments = data?.content ?? [];

  return (
    <section>
      <form onSubmit={handleFilterSubmit} className="mb-4 flex flex-wrap gap-2">
        <input
          type="number"
          placeholder="게시글 ID"
          value={postIdInput}
          onChange={(e) => setPostIdInput(e.target.value)}
          className="w-32 rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
        />
        <select
          value={visibleFilter}
          onChange={(e) => { setVisibleFilter(e.target.value as "" | "true" | "false"); setPage(0); }}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
        >
          <option value="">전체 (숨김 포함)</option>
          <option value="true">공개</option>
          <option value="false">숨김</option>
        </select>
        <button
          type="submit"
          className="rounded-md bg-gray-900 px-3 py-1.5 text-sm text-white hover:bg-gray-700"
        >
          검색
        </button>
      </form>

      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50 text-xs uppercase text-gray-500">
            <tr>
              <th className="px-4 py-3 text-left">ID</th>
              <th className="px-4 py-3 text-left">게시글</th>
              <th className="px-4 py-3 text-left">내용</th>
              <th className="px-4 py-3 text-left">작성자</th>
              <th className="px-4 py-3 text-left">상태</th>
              <th className="px-4 py-3 text-left">작성일</th>
              <th className="px-4 py-3 text-center">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {comments.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-gray-400">
                  댓글이 없습니다.
                </td>
              </tr>
            ) : (
              comments.map((comment) => (
                <tr
                  key={comment.id}
                  className={`hover:bg-gray-50 ${!comment.isVisible ? "opacity-60" : ""}`}
                >
                  <td className="px-4 py-3 text-gray-400">{comment.id}</td>
                  <td className="px-4 py-3 text-gray-500">
                    #{comment.postId}
                    {comment.parentId !== null && (
                      <span className="ml-1 text-xs text-gray-400">(대댓글)</span>
                    )}
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <p className="truncate text-gray-700">{comment.content}</p>
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    {comment.authorNickname}
                    {comment.isGuest && (
                      <span className="ml-1 text-xs text-gray-400">(비회원)</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {comment.isVisible ? (
                      <span className="text-xs font-medium text-green-600">공개</span>
                    ) : (
                      <span className="text-xs font-medium text-red-500">숨김</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {new Date(comment.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      disabled={changeVisibility.isPending}
                      onClick={() =>
                        changeVisibility.mutate({
                          id: comment.id,
                          data: { visible: !comment.isVisible },
                        })
                      }
                      className={`rounded px-2 py-1 text-xs font-medium transition-colors disabled:opacity-50 ${
                        comment.isVisible
                          ? "bg-red-50 text-red-600 hover:bg-red-100"
                          : "bg-green-50 text-green-700 hover:bg-green-100"
                      }`}
                    >
                      {comment.isVisible ? "숨김" : "복구"}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50 disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-sm text-gray-600">
            {page + 1} / {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={data.last}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50 disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </section>
  );
}
