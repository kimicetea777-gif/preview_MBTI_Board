"use client";

import { useState } from "react";
import { useAdminPosts, useChangePostVisibility, useDeleteAdminPost } from "@/hooks/useAdmin";
import { POST_TYPE, POST_TYPE_LABELS, POST_TYPE_META } from "@/types/post";
import type { PostType } from "@/types/post";
import { SORT_DIR } from "@/types/admin";
import type { SortDir } from "@/types/admin";
import { ROUTES } from "@/constants/routes";

const PAGE_SIZE = 20;

const INPUT_CLS = "rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none";

const POST_BOARD_ROUTE: Record<PostType, (id: number) => string> = {
  FREE: ROUTES.board.freePost,
  VOTE: ROUTES.board.votePost,
  DEBATE: ROUTES.board.debatePost,
};

const INITIAL_FILTERS = {
  typeCode: "" as PostType | "",
  isVisible: "" as "" | "true" | "false",
  authorNickname: "",
  sortDir: SORT_DIR.DESC as SortDir,
  page: 0,
};

export default function AdminPostTable() {
  const [typeCode, setTypeCode] = useState<PostType | "">(INITIAL_FILTERS.typeCode);
  const [isVisible, setIsVisible] = useState<"" | "true" | "false">(INITIAL_FILTERS.isVisible);
  const [authorNickname, setAuthorNickname] = useState(INITIAL_FILTERS.authorNickname);
  const [sortDir, setSortDir] = useState<SortDir>(INITIAL_FILTERS.sortDir);
  const [appliedParams, setAppliedParams] = useState(INITIAL_FILTERS);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  const { data } = useAdminPosts({
    typeCode: appliedParams.typeCode || undefined,
    isVisible: appliedParams.isVisible === "" ? undefined : appliedParams.isVisible === "true",
    authorNickname: appliedParams.authorNickname || undefined,
    sortDir: appliedParams.sortDir,
    page: appliedParams.page,
    size: PAGE_SIZE,
  });

  const changeVisibility = useChangePostVisibility();
  const deletePost = useDeleteAdminPost(() => setConfirmDeleteId(null));

  const posts = data.content;

  function handleFilterSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setAppliedParams({ typeCode, isVisible, authorNickname, sortDir, page: 0 });
  }

  function handleReset() {
    setTypeCode(INITIAL_FILTERS.typeCode);
    setIsVisible(INITIAL_FILTERS.isVisible);
    setAuthorNickname(INITIAL_FILTERS.authorNickname);
    setSortDir(INITIAL_FILTERS.sortDir);
    setAppliedParams(INITIAL_FILTERS);
  }

  return (
    <section>
      <form onSubmit={handleFilterSubmit} className="mb-4 flex flex-wrap gap-2">
        <select
          value={typeCode}
          onChange={(e) => setTypeCode(e.target.value as PostType | "")}
          className={INPUT_CLS}
        >
          <option value="">전체 유형</option>
          {(Object.keys(POST_TYPE) as PostType[]).map((t) => (
            <option key={t} value={t}>{POST_TYPE_LABELS[t]}</option>
          ))}
        </select>
        <select
          value={isVisible}
          onChange={(e) => setIsVisible(e.target.value as "" | "true" | "false")}
          className={INPUT_CLS}
        >
          <option value="">전체 (숨김 포함)</option>
          <option value="true">공개</option>
          <option value="false">숨김</option>
        </select>
        <input
          type="text"
          placeholder="작성자 닉네임 검색"
          value={authorNickname}
          onChange={(e) => setAuthorNickname(e.target.value)}
          className={INPUT_CLS}
        />
        <select
          value={sortDir}
          onChange={(e) => setSortDir(e.target.value as SortDir)}
          className={INPUT_CLS}
        >
          <option value={SORT_DIR.DESC}>작성일 | 내림차순</option>
          <option value={SORT_DIR.ASC}>작성일 | 오름차순</option>
        </select>
        <button
          type="submit"
          className="rounded-md bg-primary/70 px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/80"
        >
          검색
        </button>
        <button
          type="button"
          onClick={handleReset}
          className="rounded-md border border-outline-variant px-3 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container-low"
        >
          초기화
        </button>
      </form>

      <div className="overflow-x-auto rounded-lg border border-outline-variant">
        <table className="min-w-full text-sm">
          <thead className="bg-surface-container-low text-xs uppercase text-on-surface-variant whitespace-nowrap">
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
          <tbody className="divide-y divide-outline-variant bg-surface-container-lowest">
            {posts.length === 0 ? (
              <tr>
                <td colSpan={9} className="py-8 text-center text-on-surface-variant">
                  게시글이 없습니다.
                </td>
              </tr>
            ) : (
              posts.map((post) => (
                <tr
                  key={post.id}
                  onClick={() => window.open(POST_BOARD_ROUTE[post.typeCode](post.id), "_blank")}
                  className={`cursor-pointer hover:bg-surface-container-low ${!post.isVisible ? "opacity-60" : ""}`}
                >
                  <td className="px-4 py-3 text-on-surface-variant">{post.id}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs font-medium whitespace-nowrap ${POST_TYPE_META[post.typeCode]?.color ?? ""}`}>
                      {POST_TYPE_LABELS[post.typeCode] ?? post.typeCode}
                    </span>
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <p className="truncate font-medium text-on-surface">{post.title}</p>
                  </td>
                  <td className="px-4 py-3 text-on-surface-variant">{post.authorNickname}</td>
                  <td className="px-4 py-3 text-right text-on-surface-variant">{post.viewCount}</td>
                  <td className="px-4 py-3 text-right text-on-surface-variant">{post.commentCount}</td>
                  <td className="px-4 py-3">
                    {post.isVisible ? (
                      <span className="whitespace-nowrap text-xs font-medium text-green-600">공개</span>
                    ) : (
                      <span className="whitespace-nowrap text-xs font-medium text-red-500">숨김</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-on-surface-variant">
                    {new Date(post.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td
                    className="px-4 py-3 text-center"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {confirmDeleteId === post.id ? (
                      <div className="flex items-center justify-center gap-1">
                        <button
                          onClick={() => setConfirmDeleteId(null)}
                          className="rounded px-2 py-1 text-xs font-medium border border-outline-variant text-on-surface-variant hover:bg-surface-container-low"
                        >
                          취소
                        </button>
                        <button
                          onClick={() => deletePost.mutate(post.id)}
                          disabled={deletePost.isPending}
                          className="rounded px-2 py-1 text-xs font-medium bg-red-600 text-white hover:bg-red-700 disabled:opacity-50"
                        >
                          확인
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center justify-center gap-1">
                        <button
                          onClick={() => changeVisibility.mutate({ id: post.id, data: { visible: !post.isVisible } })}
                          disabled={changeVisibility.isPending}
                          className="rounded px-2 py-1 text-xs font-medium whitespace-nowrap disabled:opacity-50 border border-outline-variant text-on-surface-variant hover:bg-surface-container-low"
                        >
                          {post.isVisible ? "숨기기" : "복구"}
                        </button>
                        <button
                          onClick={() => setConfirmDeleteId(post.id)}
                          className="rounded px-2 py-1 text-xs font-medium whitespace-nowrap bg-red-50 text-red-600 hover:bg-red-100"
                        >
                          삭제
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <button
            onClick={() => setAppliedParams((p) => ({ ...p, page: Math.max(0, p.page - 1) }))}
            disabled={appliedParams.page === 0}
            className="rounded-md border border-outline-variant px-3 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container-low disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-sm text-on-surface-variant">
            {appliedParams.page + 1} / {data.totalPages}
          </span>
          <button
            onClick={() => setAppliedParams((p) => ({ ...p, page: Math.min(data.totalPages - 1, p.page + 1) }))}
            disabled={data.last}
            className="rounded-md border border-outline-variant px-3 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container-low disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </section>
  );
}
