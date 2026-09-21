"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { castVote, fetchVoteMbtiStats } from "@/lib/api/vote";
import { postKeys } from "@/hooks/usePosts";
import { getDeadlineLabel } from "@/lib/dateUtils";
import { toast } from "@/components/common/Toast";
import { getMbtiColor } from "@/constants/mbtiData";
import { MdBarChart, MdCheckCircle, MdGroup, MdSchedule } from "react-icons/md";
import type { VoteMbtiCount, VoteResult } from "@/types/post";

const VOTE_OPTION_VARIANT = {
  disabledCls: "border-2 border-primary/40 text-primary/50 cursor-not-allowed",
  selectedCls: "border-2 border-primary bg-primary text-primary-foreground shadow-md cursor-pointer",
  defaultCls: "border-2 border-primary text-primary hover:bg-primary/10 cursor-pointer",
};

interface Props {
  postId: number;
  voteResult: VoteResult;
  isLoggedIn: boolean;
}

export default function VoteSection({ postId, voteResult, isLoggedIn }: Props) {
  const queryClient = useQueryClient();
  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null);
  const [isChangingVote, setIsChangingVote] = useState(false);
  const [showMbtiBreakdown, setShowMbtiBreakdown] = useState(false);

  const { data: mbtiStats, isLoading: isMbtiStatsLoading, isError: isMbtiStatsError } = useQuery({
    queryKey: postKeys.voteMbtiStats(postId),
    queryFn: () => fetchVoteMbtiStats(postId),
    enabled: showMbtiBreakdown,
    staleTime: 1000 * 60,
  });

  const mbtiCountsByOption = useMemo(() => {
    const map = new Map<number, VoteMbtiCount[]>();
    mbtiStats?.options.forEach((o) => map.set(o.optionId, o.mbtiCounts));
    return map;
  }, [mbtiStats]);

  const voteMutation = useMutation({
    mutationFn: (optionId: number) => castVote(postId, optionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) });
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

  const totalVotes = voteResult.options.reduce((sum, o) => sum + o.voteCount, 0);
  const hasVoted = voteResult.myVotedOptionId !== null;
  const showResults = voteResult.isEnded || (hasVoted && !isChangingVote);
  const deadlineLabel = getDeadlineLabel(voteResult.endsAt, voteResult.isEnded);
  const showsVoteActionButtons = !showResults && isLoggedIn;

  const optionCount = voteResult.options.length;
  const resultGridCls =
    optionCount >= 7 ? "grid grid-cols-3 gap-x-6 gap-y-4" :
    optionCount >= 5 ? "grid grid-cols-2 gap-x-6 gap-y-4" :
    "space-y-4";

  const handleVote = () => {
    if (selectedOptionId === null) return;
    voteMutation.mutate(selectedOptionId);
  };

  return (
    <section
      className="bg-surface-container-lowest rounded-2xl p-8 border border-surface-container"
      style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.08)" }}
    >
      {/* ---- Vote Results ---- */}
      <div className="flex items-center justify-between mb-3">
        <span className="text-[11px] text-on-surface-variant">
          {showMbtiBreakdown && isMbtiStatsLoading && "MBTI 통계 불러오는 중..."}
          {showMbtiBreakdown && isMbtiStatsError && "MBTI 통계를 불러오지 못했습니다."}
        </span>
        <button
          type="button"
          onClick={() => setShowMbtiBreakdown((v) => !v)}
          className="flex items-center gap-1 text-xs text-on-surface-variant hover:text-primary transition-colors"
        >
          <MdBarChart className="text-sm" />
          {showMbtiBreakdown ? "MBTI별 통계 숨기기" : "MBTI별 통계 보기"}
        </button>
      </div>

      <ul className={`${resultGridCls} mb-6`}>
        {voteResult.options.map((option) => {
          const pct = totalVotes > 0
            ? Math.round((option.voteCount / totalVotes) * 100)
            : 0;
          const isMyVote = option.id === voteResult.myVotedOptionId;
          return (
            <li key={option.id}>
              <div className="flex items-center justify-between mb-1.5">
                <span className={`text-sm font-semibold flex items-center gap-1 ${isMyVote ? "text-primary" : "text-on-surface"}`}>
                  {option.optionText}
                  {isMyVote && <MdCheckCircle className="text-primary" size={14} />}
                </span>
                <span className="text-xs text-on-surface-variant tabular-nums">
                  {pct}% ({option.voteCount.toLocaleString()}표)
                </span>
              </div>
              <div className="h-2.5 rounded-full bg-surface-container overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-700 ${isMyVote ? "bg-primary" : "bg-outline-variant"}`}
                  style={{ width: `${pct}%` }}
                />
              </div>
              {showMbtiBreakdown && !isMbtiStatsLoading && !isMbtiStatsError && (
                <MbtiBreakdownBar mbtiCounts={mbtiCountsByOption.get(option.id) ?? []} />
              )}
            </li>
          );
        })}
      </ul>

      {/* ---- Vote Selection ---- */}
      {!showResults && (
        <ul className="flex flex-wrap gap-3 pb-6 border-b border-outline-variant">
          {voteResult.options.map((option) => (
            <li key={option.id}>
              <label
                onClick={!isLoggedIn ? () => toast.warning("로그인 후 투표에 참여할 수 있습니다.") : undefined}
                className={`inline-flex items-center gap-3 p-3 rounded-xl transition-all ${
                  !isLoggedIn
                    ? VOTE_OPTION_VARIANT.disabledCls
                    : selectedOptionId === option.id
                    ? VOTE_OPTION_VARIANT.selectedCls
                    : VOTE_OPTION_VARIANT.defaultCls
                }`}
              >
                <input
                  type="radio"
                  name="voteOption"
                  value={String(option.id)}
                  checked={selectedOptionId === option.id}
                  onChange={() => setSelectedOptionId(option.id)}
                  disabled={!isLoggedIn || voteMutation.isPending}
                  className="sr-only"
                />
                <span className="text-sm font-medium">{option.optionText}</span>
              </label>
            </li>
          ))}
        </ul>
      )}

      {/* ---- Bottom Row ---- */}
      <div
        className={`flex mt-6 ${
          showsVoteActionButtons
            ? "flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
            : "items-center justify-between"
        }`}
      >
        <div className="flex items-center">
          <span className={`flex items-center gap-1.5 text-xs px-3 py-1 rounded-full font-semibold border ${
            voteResult.isEnded
              ? "bg-surface-container text-on-surface-variant border-outline-variant"
              : "bg-primary/10 text-primary border-primary/20"
          }`}>
            <MdSchedule className="text-sm" />
            <span className="font-bold">{deadlineLabel}</span>
          </span>
          <span className="flex items-center gap-1.5 text-xs text-on-surface-variant ml-3">
            <MdGroup className="text-sm" />
            총 {totalVotes.toLocaleString()}표
          </span>
        </div>

        <div className={`flex items-center gap-4 ${showsVoteActionButtons ? "self-end sm:self-auto" : ""}`}>
          {showResults ? (
            hasVoted && !voteResult.isEnded && (
              <button
                type="button"
                onClick={() => {
                  setSelectedOptionId(voteResult.myVotedOptionId);
                  setIsChangingVote(true);
                }}
                className="text-xs text-on-surface-variant hover:text-primary underline underline-offset-2 transition-colors"
              >
                투표 변경
              </button>
            )
          ) : (
            isLoggedIn ? (
              <div className="flex items-center gap-2">
                {isChangingVote && (
                  <button
                    type="button"
                    onClick={() => setIsChangingVote(false)}
                    disabled={voteMutation.isPending}
                    className="rounded-lg border border-outline-variant px-4 py-2 text-sm text-on-surface-variant hover:bg-surface-container disabled:opacity-50 transition-colors"
                  >
                    취소
                  </button>
                )}
                <button
                  type="button"
                  onClick={handleVote}
                  disabled={selectedOptionId === null || voteMutation.isPending}
                  className="rounded-lg bg-primary text-primary-foreground px-6 py-2 text-sm font-semibold hover:opacity-90 active:scale-[0.98] disabled:opacity-50 transition-all"
                >
                  {voteMutation.isPending ? "투표 중..." : "투표하기"}
                </button>
              </div>
            ) : (
              <p className="text-xs text-on-surface-variant">로그인 후 투표에 참여할 수 있습니다.</p>
            )
          )}
        </div>
      </div>
    </section>
  );
}

function MbtiBreakdownBar({ mbtiCounts }: { mbtiCounts: VoteMbtiCount[] }) {
  const [expanded, setExpanded] = useState(false);
  const total = mbtiCounts.reduce((sum, m) => sum + m.count, 0);

  if (total === 0) {
    return <p className="mt-2 text-[11px] text-on-surface-variant">아직 투표한 MBTI가 없습니다.</p>;
  }

  const sorted = [...mbtiCounts].sort((a, b) => b.count - a.count);
  const segments = sorted.slice(0, 3);
  const otherCount = total - segments.reduce((sum, m) => sum + m.count, 0);
  const legendItems = expanded ? sorted : segments;
  const hiddenCount = sorted.length - segments.length;

  return (
    <div className="mt-2">
      <div className="h-1.5 rounded-full bg-surface-container overflow-hidden flex">
        {segments.map((m) => (
          <div
            key={m.mbtiId}
            className="h-full"
            style={{ width: `${(m.count / total) * 100}%`, backgroundColor: getMbtiColor(m.mbtiId) }}
            title={`${m.mbtiId}: ${m.count.toLocaleString()}표`}
          />
        ))}
        {otherCount > 0 && (
          <div className="h-full bg-outline-variant/50" style={{ width: `${(otherCount / total) * 100}%` }} />
        )}
      </div>
      <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1">
        {legendItems.map((m) => (
          <span key={m.mbtiId} className="flex items-center gap-1 text-[11px] text-on-surface-variant">
            <span className="w-1.5 h-1.5 rounded-full shrink-0" style={{ backgroundColor: getMbtiColor(m.mbtiId) }} />
            <span className="font-medium text-on-surface">{m.mbtiId}</span>
            <span className="tabular-nums">{m.count.toLocaleString()}</span>
          </span>
        ))}
        {hiddenCount > 0 && (
          <button
            type="button"
            onClick={() => setExpanded((v) => !v)}
            className="text-[11px] text-on-surface-variant hover:text-primary underline underline-offset-2"
          >
            {expanded ? "접기" : `+${hiddenCount}개 더 보기`}
          </button>
        )}
      </div>
    </div>
  );
}
