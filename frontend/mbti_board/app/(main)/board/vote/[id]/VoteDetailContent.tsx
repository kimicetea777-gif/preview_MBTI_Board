"use client";

import { useState } from "react";
import { useRouter, notFound } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { usePostDetail, postKeys } from "@/hooks/usePosts";
import { deletePost } from "@/lib/api/post";
import { castVote } from "@/lib/api/vote";
import { useAuthStore } from "@/store/authStore";
import RichContentViewer from "@/components/post/RichContentViewer";
import MbtiBadge from "@/components/common/MbtiBadge";
import CommentSection from "@/components/post/CommentSection";
import { formatDate, getDeadlineLabel } from "@/lib/dateUtils";
import { isTiptapEmpty } from "@/lib/editor";
import { ROUTES } from "@/lib/routes";
import { toast } from "@/components/common/Toast";

interface Props {
  id: number;
}

function DetailSkeleton() {
  return (
    <section className="max-w-2xl mx-auto px-4 py-6 space-y-4 animate-pulse">
      <div className="h-6 bg-gray-200 rounded w-2/3" />
      <div className="h-4 bg-gray-200 rounded w-1/4" />
      <div className="space-y-2 mt-6">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-4 bg-gray-200 rounded" />
        ))}
      </div>
      <div className="h-40 bg-gray-100 rounded-lg mt-4" />
    </section>
  );
}

export default function VoteDetailContent({ id }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isLoggedIn = useAuthStore((s) => s.accessToken) !== null;

  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null);
  const [isChangingVote, setIsChangingVote] = useState(false);

  const { data: post, isLoading, isError, error } = usePostDetail(id);

  const voteMutation = useMutation({
    mutationFn: (optionId: number) => castVote(id, optionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: postKeys.detail(id) });
      setIsChangingVote(false);
      toast.success("투표가 완료되었습니다.");
    },
    onError: (err) => {
      const message = isAxiosError(err)
        ? (err.response?.data?.message ?? "투표 중 오류가 발생했습니다.")
        : "투표 중 오류가 발생했습니다.";
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deletePost(id, undefined),
    onSuccess: () => router.push(ROUTES.board.vote),
    onError: () => toast.error("삭제 중 오류가 발생했습니다."),
  });

  if (isError) {
    if (isAxiosError(error) && error.response?.status === 404) notFound();
    return (
      <section className="max-w-2xl mx-auto px-4 py-6">
        <p className="text-sm text-red-500">오류가 발생했습니다. 다시 시도해 주세요.</p>
      </section>
    );
  }

  if (isLoading || !post || !post.voteResult) return <DetailSkeleton />;

  const voteResult = post.voteResult;

  const totalVotes = voteResult.options.reduce((sum, o) => sum + o.voteCount, 0);
  const hasVoted = voteResult.myVotedOptionId !== null;
  const showResults = voteResult.isEnded || (hasVoted && !isChangingVote);
  const deadlineLabel = getDeadlineLabel(voteResult.endsAt, voteResult.isEnded);

  const handleVote = () => {
    if (selectedOptionId === null) return;
    voteMutation.mutate(selectedOptionId);
  };

  const handleDelete = () => {
    if (!window.confirm("게시글을 삭제하시겠습니까?")) return;
    deleteMutation.mutate();
  };

  return (
    <section className="max-w-2xl mx-auto px-4 py-6">
      <article className="space-y-4">
        <header className="space-y-2 border-b border-gray-100 pb-4">
          <h1 className="text-xl font-bold text-gray-900">{post.title}</h1>
          <div className="flex flex-wrap items-center gap-2 text-xs text-gray-500">
            <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} />
            <span>{post.authorNickname}</span>
            <span>·</span>
            <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
            <span>조회 {post.viewCount}</span>
          </div>
        </header>

        {!isTiptapEmpty(post.content) && (
          <section>
            <RichContentViewer content={post.content} />
          </section>
        )}

        {/* 투표 섹션 */}
        <section className="border border-gray-200 rounded-lg p-4 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-gray-700">투표</h2>
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
              voteResult.isEnded
                ? "bg-gray-100 text-gray-500"
                : "bg-gray-900 text-white"
            }`}>
              {deadlineLabel}
            </span>
          </div>

          {/* 항상 표시: 현재 투표 현황 */}
          <ul className="space-y-3">
            {voteResult.options.map((option) => {
              const pct = totalVotes > 0
                ? Math.round((option.voteCount / totalVotes) * 100)
                : 0;
              const isMyVote = option.id === voteResult.myVotedOptionId;
              return (
                <li key={option.id} className="space-y-1">
                  <div className="flex items-center justify-between text-sm">
                    <span className={isMyVote ? "font-semibold text-gray-900" : "text-gray-700"}>
                      {option.optionText}{isMyVote && " ✓"}
                    </span>
                    <span className="text-gray-500 tabular-nums">
                      {pct}% ({option.voteCount}표)
                    </span>
                  </div>
                  <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        isMyVote ? "bg-gray-900" : "bg-gray-300"
                      }`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </li>
              );
            })}
          </ul>

          {/* 투표 선택 UI: 미투표 또는 투표 변경 중일 때 */}
          {!showResults && (
            <ul className="space-y-2 pt-3 border-t border-gray-100">
              {voteResult.options.map((option) => (
                <li key={option.id}>
                  <label className={`flex items-center gap-3 p-3 rounded-lg border transition-colors ${
                    selectedOptionId === option.id
                      ? "border-gray-900 bg-gray-50"
                      : "border-gray-200 hover:border-gray-400"
                  } ${!isLoggedIn ? "cursor-not-allowed opacity-60" : "cursor-pointer"}`}>
                    <input
                      type="radio"
                      name="voteOption"
                      value={String(option.id)}
                      checked={selectedOptionId === option.id}
                      onChange={() => setSelectedOptionId(option.id)}
                      disabled={!isLoggedIn || voteMutation.isPending}
                      className="accent-gray-900"
                    />
                    <span className="text-sm text-gray-700">{option.optionText}</span>
                  </label>
                </li>
              ))}
            </ul>
          )}

          <div className="flex items-center justify-between pt-2 border-t border-gray-100">
            {showResults ? (
              hasVoted && !voteResult.isEnded && (
                <button
                  type="button"
                  onClick={() => {
                    setSelectedOptionId(voteResult.myVotedOptionId);
                    setIsChangingVote(true);
                  }}
                  className="text-xs text-gray-500 hover:text-gray-900 transition-colors"
                >
                  투표 변경
                </button>
              )
            ) : (
              isLoggedIn ? (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={handleVote}
                    disabled={selectedOptionId === null || voteMutation.isPending}
                    className="rounded-lg bg-gray-900 px-4 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                  >
                    {voteMutation.isPending ? "투표 중..." : "투표하기"}
                  </button>
                  {isChangingVote && (
                    <button
                      type="button"
                      onClick={() => setIsChangingVote(false)}
                      disabled={voteMutation.isPending}
                      className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 transition-colors"
                    >
                      취소
                    </button>
                  )}
                </div>
              ) : (
                <p className="text-xs text-gray-400">로그인 후 투표에 참여할 수 있습니다.</p>
              )
            )}
            <span className="text-xs text-gray-400 ml-auto">총 {totalVotes}표</span>
          </div>
        </section>

        {post.isOwner && (
          <footer className="pt-2 border-t border-gray-100 flex justify-end">
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className="rounded-lg border border-red-200 px-4 py-2 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50 transition-colors"
            >
              {deleteMutation.isPending ? "삭제 중..." : "삭제"}
            </button>
          </footer>
        )}
      </article>

      <CommentSection postId={id} />
    </section>
  );
}
