"use client";

import { useState } from "react";
import { useSuspenseQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import Image from "next/image";
import { fetchMyPosts, deletePost } from "@/lib/api/post";
import { formatRelativeTime } from "@/lib/dateUtils";
import { POST_TYPE, POST_TYPE_LABELS } from "@/types/post";
import type { PostType } from "@/types/post";
import { postHref } from "@/lib/boardUtils";
import PostTypeBadge from "@/components/common/PostTypeBadge";
import {
  MdChatBubble, MdBarChart,
  MdEdit, MdDelete, MdChevronLeft, MdChevronRight,
} from "react-icons/md";

const TABS: { label: string; value: PostType | undefined }[] = [
  { label: "전체",                             value: undefined },
  { label: POST_TYPE_LABELS[POST_TYPE.FREE],   value: POST_TYPE.FREE },
  { label: POST_TYPE_LABELS[POST_TYPE.VOTE],   value: POST_TYPE.VOTE },
  { label: POST_TYPE_LABELS[POST_TYPE.DEBATE], value: POST_TYPE.DEBATE },
];


const PAGE_SIZE = 10;

function buildPageNumbers(current: number, total: number): number[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const pages: number[] = [0];
  if (current > 3) pages.push(-1);
  const start = Math.max(1, current - 1);
  const end = Math.min(total - 2, current + 1);
  for (let i = start; i <= end; i++) pages.push(i);
  if (current < total - 4) pages.push(-1);
  pages.push(total - 1);
  return pages;
}

export default function MyPostsSection() {
  const queryClient = useQueryClient();
  const [selectedType, setSelectedType] = useState<PostType | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data } = useSuspenseQuery({
    queryKey: ["myPosts", selectedType, page],
    queryFn: () => fetchMyPosts({ type: selectedType, sort: "LATEST", page, size: PAGE_SIZE }),
  });

  const { mutate: remove, isPending: isDeleting } = useMutation({
    mutationFn: (id: number) => deletePost(id),
    onSuccess: () => {
      if (data.content.length === 1 && page > 0) {
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

  const pageNumbers = buildPageNumbers(page, data.totalPages);

  return (
    <section>
      {/* 헤더 + 필터 탭 */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-primary flex items-baseline gap-2">
          내 게시글
          <span className="text-sm font-normal text-on-surface-variant">{data.totalElements}개</span>
        </h1>
        <div className="flex items-center gap-1 p-1 bg-surface-container rounded-xl">
          {TABS.map((tab) => (
            <button
              key={tab.label}
              type="button"
              onClick={() => handleTabChange(tab.value)}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-all ${
                selectedType === tab.value
                  ? "bg-surface-container-lowest text-primary shadow-sm"
                  : "text-on-surface-variant hover:text-on-surface"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* 목록 */}
      {data.content.length === 0 ? (
        <p className="py-16 text-center text-sm text-on-surface-variant">아직 작성한 게시글이 없습니다.</p>
      ) : (
        <>
          <div className="flex flex-col border-t border-outline-variant/20">
            {data.content.map((post) => {
              const detailHref = postHref(post.typeCode, post.id);
              const thumbnail = post.imageUrls?.[0];

              return (
                <article
                  key={post.id}
                  className="group flex items-center gap-5 p-5 border-b border-outline-variant/20 hover:bg-surface-container-low transition-colors"
                >
                  {/* 썸네일 */}
                  <div className="relative w-16 h-16 shrink-0 rounded-xl overflow-hidden">
                    {thumbnail ? (
                      <Image
                        src={thumbnail}
                        alt=""
                        fill
                        className="object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                    ) : (
                      <div className="w-full h-full border-2 border-dashed border-outline-variant rounded-xl" />
                    )}
                  </div>

                  {/* 본문 */}
                  <div className="grow min-w-0 flex flex-col justify-between gap-2">
                    <div className="flex flex-col gap-1">
                      <PostTypeBadge type={post.typeCode} className="self-start" />
                      <Link href={detailHref} className="block hover:underline min-w-0">
                        <p className="text-sm font-semibold text-primary truncate">{post.title}</p>
                      </Link>
                    </div>

                    <div className="flex items-center gap-4 text-xs text-on-surface-variant">
                      <span className="w-12 shrink-0">{formatRelativeTime(post.createdAt)}</span>
                      <span className="flex items-center gap-1">
                        <MdBarChart className="text-base" />
                        {post.viewCount}
                      </span>
                      <span className="flex items-center gap-1">
                        <MdChatBubble className="text-base" />
                        {post.commentCount}
                      </span>
                    </div>
                  </div>

                  {/* 수정·삭제 */}
                  <div className="shrink-0 flex items-center gap-1">
                    <Link
                      href={detailHref}
                      title="수정"
                      className="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-highest transition-colors"
                    >
                      <MdEdit className="text-lg" />
                    </Link>
                    <button
                      type="button"
                      onClick={() => handleDelete(post.id, post.title)}
                      disabled={isDeleting}
                      title="삭제"
                      className="p-2 rounded-full text-error hover:bg-error-container/30 transition-colors disabled:opacity-40"
                    >
                      <MdDelete className="text-lg" />
                    </button>
                  </div>
                </article>
              );
            })}
          </div>

          {/* 페이지네이션 */}
          {data.totalPages > 1 && (
            <nav className="mt-12 flex justify-center items-center gap-2">
              <button
                type="button"
                onClick={() => setPage((p) => p - 1)}
                disabled={page === 0}
                className="w-10 h-10 flex items-center justify-center rounded-full border border-outline-variant text-on-surface-variant hover:bg-surface-container transition-colors disabled:opacity-30"
              >
                <MdChevronLeft className="text-xl" />
              </button>

              {pageNumbers.map((num, i) =>
                num === -1 ? (
                  <span key={`ellipsis-${i}`} className="w-10 h-10 flex items-center justify-center text-on-surface-variant">
                    ...
                  </span>
                ) : (
                  <button
                    key={num}
                    type="button"
                    onClick={() => setPage(num)}
                    className={`w-10 h-10 flex items-center justify-center rounded-full text-sm font-semibold transition-colors ${
                      page === num
                        ? "bg-primary text-on-primary"
                        : "text-on-surface-variant hover:bg-surface-container"
                    }`}
                  >
                    {num + 1}
                  </button>
                )
              )}

              <button
                type="button"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last}
                className="w-10 h-10 flex items-center justify-center rounded-full border border-outline-variant text-on-surface-variant hover:bg-surface-container transition-colors disabled:opacity-30"
              >
                <MdChevronRight className="text-xl" />
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  );
}
