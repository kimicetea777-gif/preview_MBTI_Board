"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { fetchMyPosts, deletePost } from "@/lib/api/post";
import { formatRelativeTime } from "@/lib/dateUtils";
import { ROUTES } from "@/lib/routes";
import { POST_TYPE, POST_TYPE_LABELS } from "@/types/post";
import type { PostType } from "@/types/post";

const POST_DETAIL_HREF: Record<PostType, (id: number) => string> = {
  [POST_TYPE.FREE]:   (id) => ROUTES.board.freePost(id),
  [POST_TYPE.VOTE]:   (id) => ROUTES.board.votePost(id),
  [POST_TYPE.DEBATE]: (id) => ROUTES.board.debatePost(id),
};

const TABS: { label: string; value: PostType | undefined }[] = [
  { label: "전체",                             value: undefined },
  { label: POST_TYPE_LABELS[POST_TYPE.FREE],   value: POST_TYPE.FREE },
  { label: POST_TYPE_LABELS[POST_TYPE.VOTE],   value: POST_TYPE.VOTE },
  { label: POST_TYPE_LABELS[POST_TYPE.DEBATE], value: POST_TYPE.DEBATE },
];

const PAGE_SIZE = 10;

export default function MyPostsSection() {
  const queryClient = useQueryClient();
  const [selectedType, setSelectedType] = useState<PostType | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["myPosts", selectedType, page],
    queryFn: () => fetchMyPosts({ type: selectedType, sort: "LATEST", page, size: PAGE_SIZE }),
  });

  const { mutate: remove, isPending: isDeleting } = useMutation({
    mutationFn: (id: number) => deletePost(id),
    onSuccess: () => {
      // 현재 페이지 게시글이 1개였다면 이전 페이지로
      if (data?.content.length === 1 && page > 0) {
        setPage((p) => p - 1);
      } else {
        queryClient.invalidateQueries({ queryKey: ["myPosts"] });
      }
    },
  });

  const handleTabChange = (type: PostType | undefined) => {
    setSelectedType(type);
    setPage(0);
  };

  const handleDelete = (id: number, title: string) => {
    if (!window.confirm(`"${title}" 게시글을 삭제하시겠습니까?`)) return;
    remove(id);
  };

  return (
    <section>
      {/* 탭 필터 */}
      <nav className="flex gap-1 mb-4 border-b border-gray-200">
        {TABS.map((tab) => (
          <button
            key={tab.label}
            onClick={() => handleTabChange(tab.value)}
            className={[
              "px-3 py-2 text-sm font-medium transition-colors",
              selectedType === tab.value
                ? "border-b-2 border-gray-900 text-gray-900"
                : "text-gray-500 hover:text-gray-700",
            ].join(" ")}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      {/* 목록 */}
      {isLoading && (
        <p className="py-6 text-center text-sm text-gray-400">불러오는 중...</p>
      )}

      {isError && (
        <p className="py-6 text-center text-sm text-red-500">게시글을 불러올 수 없습니다.</p>
      )}

      {!isLoading && !isError && data?.content.length === 0 && (
        <p className="py-6 text-center text-sm text-gray-400">아직 작성한 게시글이 없습니다.</p>
      )}

      {!isLoading && !isError && data && data.content.length > 0 && (
        <>
          <ul>
            {data.content.map((post) => {
              const detailHref = POST_DETAIL_HREF[post.typeCode](post.id);
              return (
                <li key={post.id} className="border-b border-gray-100 last:border-0 py-3 px-1">
                  <div className="flex items-start gap-3">
                    <span className="shrink-0 mt-0.5 text-xs font-medium text-gray-400 w-10">
                      [{POST_TYPE_LABELS[post.typeCode]}]
                    </span>
                    <div className="flex-1 min-w-0">
                      <Link
                        href={detailHref}
                        className="block hover:underline"
                      >
                        <p className="text-sm font-medium text-gray-900 truncate">{post.title}</p>
                      </Link>
                      <div className="mt-1 flex items-center gap-2 text-xs text-gray-400">
                        <span>댓글 {post.commentCount}</span>
                        <span>조회 {post.viewCount}</span>
                        <span>·</span>
                        <span>{formatRelativeTime(post.createdAt)}</span>
                      </div>
                    </div>
                    {/* 수정·삭제 버튼 */}
                    <div className="shrink-0 flex items-center gap-1">
                      <Link
                        href={detailHref}
                        className="px-2 py-1 text-xs rounded border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors"
                      >
                        수정
                      </Link>
                      <button
                        onClick={() => handleDelete(post.id, post.title)}
                        disabled={isDeleting}
                        className="px-2 py-1 text-xs rounded border border-red-200 text-red-500 hover:bg-red-50 transition-colors disabled:opacity-40"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>

          {/* 페이지네이션 */}
          {data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-4 pt-4 border-t border-gray-100">
              <button
                onClick={() => setPage((p) => p - 1)}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50 transition-colors"
              >
                이전
              </button>
              <span className="text-sm text-gray-500">
                {page + 1} / {data.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last}
                className="px-3 py-1.5 text-sm rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50 transition-colors"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
