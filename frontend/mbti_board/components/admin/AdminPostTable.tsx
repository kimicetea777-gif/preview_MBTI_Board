"use client";

import { useState } from "react";
import { useAdminPosts, useChangePostVisibility } from "@/hooks/useAdmin";
import { POST_TYPE, POST_TYPE_LABELS } from "@/types/post";
import type { PostType } from "@/types/post";
import type { AxiosError } from "axios";

const PAGE_SIZE = 20;

export default function AdminPostTable() {
  const [typeFilter, setTypeFilter] = useState<PostType | "">("");
  const [visibleFilter, setVisibleFilter] = useState<"" | "true" | "false">("");
  const [page, setPage] = useState(0);

  const params = {
    typeCode: typeFilter || undefined,
    isVisible: visibleFilter === "" ? undefined : visibleFilter === "true",
    page,
    size: PAGE_SIZE,
  };

  const { data, isLoading, isError, error } = useAdminPosts(params);
  const changeVisibility = useChangePostVisibility();

  const is403 = isError && (error as AxiosError)?.response?.status === 403;

  if (isLoading) return <p className="py-8 text-center text-gray-500">불러오는 중...</p>;
  if (is403) return <p className="py-8 text-center text-red-500">관리자 권한이 없습니다.</p>;
  if (isError) return <p className="py-8 text-center text-red-500">데이터를 불러올 수 없습니다.</p>;

  const posts = data?.content ?? [];

  return (
    <section>
      <div className="mb-4 flex flex-wrap gap-2">
        <select
          value={typeFilter}
          onChange={(e) => { setTypeFilter(e.target.value as PostType | ""); setPage(0); }}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
        >
          <option value="">전체 유형</option>
          {(Object.keys(POST_TYPE) as PostType[]).map((t) => (
            <option key={t} value={t}>{POST_TYPE_LABELS[t]}</option>
          ))}
        </select>
        <select
          value={visibleFilter}
          onChange={(e) => { setVisibleFilter(e.target.value as "" | "true" | "false"); setPage(0); }}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
        >
          <option value="">전체 (숨김 포함)</option>
          <option value="true">공개</option>
          <option value="false">숨김</option>
        </select>
      </div>

      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50 text-xs uppercase text-gray-500">
            <tr>
              <th className="px-4 py-3 text-left">ID</th>
              <th className="px-4 py-3 text-left">유형</th>
              <th className="px-4 py-3 text-left">제목</th>
              <th className="px-4 py-3 text-left">작성자</th>
              <th className="px-4 py-3 text-right">조회</th>
              <th className="px-4 py-3 text-right">댓글</th>
              <th className="px-4 py-3 text-left">상태</th>
              <th className="px-4 py-3 text-left">작성일</th>
              <th className="px-4 py-3 text-center">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {posts.length === 0 ? (
              <tr>
                <td colSpan={9} className="py-8 text-center text-gray-400">
                  게시글이 없습니다.
                </td>
              </tr>
            ) : (
              posts.map((post) => (
                <tr
                  key={post.id}
                  className={`hover:bg-gray-50 ${!post.isVisible ? "opacity-60" : ""}`}
                >
                  <td className="px-4 py-3 text-gray-400">{post.id}</td>
                  <td className="px-4 py-3">
                    <span className="rounded bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">
                      {POST_TYPE_LABELS[post.typeCode] ?? post.typeCode}
                    </span>
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <p className="truncate font-medium text-gray-900">{post.title}</p>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{post.authorNickname}</td>
                  <td className="px-4 py-3 text-right text-gray-500">{post.viewCount}</td>
                  <td className="px-4 py-3 text-right text-gray-500">{post.commentCount}</td>
                  <td className="px-4 py-3">
                    {post.isVisible ? (
                      <span className="text-xs font-medium text-green-600">공개</span>
                    ) : (
                      <span className="text-xs font-medium text-red-500">숨김</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {new Date(post.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      disabled={changeVisibility.isPending}
                      onClick={() =>
                        changeVisibility.mutate({
                          id: post.id,
                          data: { visible: !post.isVisible },
                        })
                      }
                      className={`rounded px-2 py-1 text-xs font-medium transition-colors disabled:opacity-50 ${
                        post.isVisible
                          ? "bg-red-50 text-red-600 hover:bg-red-100"
                          : "bg-green-50 text-green-700 hover:bg-green-100"
                      }`}
                    >
                      {post.isVisible ? "숨김" : "복구"}
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
