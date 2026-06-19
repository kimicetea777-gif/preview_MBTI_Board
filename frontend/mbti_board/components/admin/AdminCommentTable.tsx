"use client";

import { useState } from "react";
import { useAdminComments, useChangeCommentVisibility } from "@/hooks/useAdmin";
import { SORT_DIR } from "@/types/admin";
import type { SortDir } from "@/types/admin";
import { POST_TYPE } from "@/types/post";
import type { PostType } from "@/types/post";
import { ROUTES } from "@/constants/routes";

const PAGE_SIZE = 20;

const POST_BOARD_ROUTE: Record<PostType, (id: number) => string> = {
  [POST_TYPE.FREE]: ROUTES.board.freePost,
  [POST_TYPE.VOTE]: ROUTES.board.votePost,
  [POST_TYPE.DEBATE]: ROUTES.board.debatePost,
};

const INPUT_CLS = "rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none";

const INITIAL_FILTERS = {
  postIdInput: "",
  visibleFilter: "" as "" | "true" | "false",
  authorNickname: "",
  sortDir: SORT_DIR.DESC as SortDir,
};

type AppliedParams = {
  postId?: number;
  isVisible?: boolean;
  authorNickname?: string;
  sortDir: SortDir;
  page: number;
};

const INITIAL_APPLIED: AppliedParams = { sortDir: SORT_DIR.DESC, page: 0 };

export default function AdminCommentTable() {
  const [postIdInput, setPostIdInput] = useState(INITIAL_FILTERS.postIdInput);
  const [visibleFilter, setVisibleFilter] = useState<"" | "true" | "false">(INITIAL_FILTERS.visibleFilter);
  const [authorNickname, setAuthorNickname] = useState(INITIAL_FILTERS.authorNickname);
  const [sortDir, setSortDir] = useState<SortDir>(INITIAL_FILTERS.sortDir);
  const [appliedParams, setAppliedParams] = useState<AppliedParams>(INITIAL_APPLIED);

  const { data } = useAdminComments({
    postId: appliedParams.postId,
    isVisible: appliedParams.isVisible,
    authorNickname: appliedParams.authorNickname,
    sortDir: appliedParams.sortDir,
    page: appliedParams.page,
    size: PAGE_SIZE,
  });
  const changeVisibility = useChangeCommentVisibility();

  function handleFilterSubmit(e: React.FormEvent) {
    e.preventDefault();
    const num = postIdInput ? Number(postIdInput) : undefined;
    setAppliedParams({
      postId: num !== undefined && !isNaN(num) ? num : undefined,
      isVisible: visibleFilter === "" ? undefined : visibleFilter === "true",
      authorNickname: authorNickname || undefined,
      sortDir,
      page: 0,
    });
  }

  function handleReset() {
    setPostIdInput(INITIAL_FILTERS.postIdInput);
    setVisibleFilter(INITIAL_FILTERS.visibleFilter);
    setAuthorNickname(INITIAL_FILTERS.authorNickname);
    setSortDir(INITIAL_FILTERS.sortDir);
    setAppliedParams(INITIAL_APPLIED);
  }

  const comments = data.content;

  return (
    <section>
      <form onSubmit={handleFilterSubmit} className="mb-4 flex flex-wrap gap-2">
        <input
          type="number"
          placeholder="게시글 ID"
          value={postIdInput}
          onChange={(e) => setPostIdInput(e.target.value)}
          className={`w-32 ${INPUT_CLS}`}
        />
        <input
          type="text"
          placeholder="작성자 닉네임 검색"
          value={authorNickname}
          onChange={(e) => setAuthorNickname(e.target.value)}
          className={INPUT_CLS}
        />
        <select
          value={visibleFilter}
          onChange={(e) => setVisibleFilter(e.target.value as "" | "true" | "false")}
          className={INPUT_CLS}
        >
          <option value="">전체 (숨김 포함)</option>
          <option value="true">공개</option>
          <option value="false">숨김</option>
        </select>
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
              <th className="px-4 py-3 text-left">게시글</th>
              <th className="px-4 py-3 text-left">내용</th>
              <th className="px-4 py-3 text-left">작성자</th>
              <th className="px-4 py-3 text-left">상태</th>
              <th className="px-4 py-3 text-left">작성일</th>
              <th className="px-4 py-3 text-center">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant bg-surface-container-lowest">
            {comments.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-on-surface-variant">
                  댓글이 없습니다.
                </td>
              </tr>
            ) : (
              comments.map((comment) => (
                <tr
                  key={comment.id}
                  onClick={() => {
                    const routeFn = POST_BOARD_ROUTE[comment.postTypeCode as PostType];
                    if (routeFn) window.open(routeFn(comment.postId), "_blank");
                  }}
                  className={`cursor-pointer hover:bg-surface-container-low ${!comment.isVisible ? "opacity-60" : ""}`}
                >
                  <td className="px-4 py-3 text-on-surface-variant">{comment.id}</td>
                  <td className="px-4 py-3 text-on-surface-variant">
                    #{comment.postId}
                    {comment.parentId !== null && (
                      <span className="ml-1 text-xs text-on-surface-variant/60">(대댓글)</span>
                    )}
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <p className="truncate font-medium text-on-surface">{comment.content}</p>
                  </td>
                  <td className="px-4 py-3 text-on-surface-variant">
                    {comment.authorNickname}
                    {comment.isGuest && (
                      <span className="ml-1 text-xs text-on-surface-variant/60">(비회원)</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {comment.isVisible ? (
                      <span className="whitespace-nowrap text-xs font-medium text-green-600">공개</span>
                    ) : (
                      <span className="whitespace-nowrap text-xs font-medium text-red-500">숨김</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-on-surface-variant">
                    {new Date(comment.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td
                    className="px-4 py-3 text-center"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      disabled={changeVisibility.isPending}
                      onClick={() =>
                        changeVisibility.mutate({
                          id: comment.id,
                          data: { visible: !comment.isVisible },
                        })
                      }
                      className={`rounded px-2 py-1 text-xs font-medium whitespace-nowrap transition-colors disabled:opacity-50 ${
                        comment.isVisible
                          ? "bg-red-50 text-red-600 hover:bg-red-100"
                          : "border border-outline-variant text-on-surface-variant hover:bg-surface-container-low"
                      }`}
                    >
                      {comment.isVisible ? "숨기기" : "복구"}
                    </button>
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
