"use client";

import { useState, useEffect, useRef, type ReactNode } from "react";
import { useDebateStats } from "@/hooks/useDebateStats";
import { useDebateStance } from "@/hooks/useDebateStance";
import DebateProgressBar from "@/components/post/DebateProgressBar";
import { getDeadlineLabel } from "@/lib/dateUtils";
import { toast } from "@/components/common/Toast";
import { STANCE, type DebateStance } from "@/types/comment";
import { MdThumbUp, MdThumbDown, MdSchedule, MdGroup } from "react-icons/md";
import type { DebateResult } from "@/types/post";

const STANCE_VARIANTS: Record<typeof STANCE.PRO | typeof STANCE.CON, { disabledCls: string; selectedCls: string; defaultCls: string }> = {
  [STANCE.PRO]: {
    disabledCls: "border-2 border-secondary/40 text-secondary/50 cursor-not-allowed",
    selectedCls: "bg-secondary text-secondary-foreground shadow-md",
    defaultCls: "border-2 border-secondary text-secondary hover:bg-secondary/10",
  },
  [STANCE.CON]: {
    disabledCls: "border-2 border-primary/40 text-primary/50 cursor-not-allowed",
    selectedCls: "bg-primary text-primary-foreground shadow-md",
    defaultCls: "border-2 border-primary text-primary hover:bg-primary/10",
  },
};

interface StanceButtonProps {
  stance: typeof STANCE.PRO | typeof STANCE.CON;
  label: string;
  icon: ReactNode;
  selectedStance: DebateStance | null;
  isLoggedIn: boolean;
  disabled: boolean;
  onVote: (next: DebateStance | null, prev: DebateStance | null) => void;
}

function StanceButton({ stance, label, icon, selectedStance, isLoggedIn, disabled, onVote }: StanceButtonProps) {
  const { disabledCls, selectedCls, defaultCls } = STANCE_VARIANTS[stance];
  const isSelected = selectedStance === stance;

  return (
    <button
      type="button"
      onClick={() => {
        if (!isLoggedIn) {
          toast.info("로그인 후 의견을 선택할 수 있습니다.");
          return;
        }
        onVote(isSelected ? null : stance, selectedStance);
      }}
      disabled={disabled}
      className={`w-full flex items-center justify-center gap-3 h-14 rounded-xl text-sm font-semibold transition-all active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed ${
        !isLoggedIn ? disabledCls : isSelected ? selectedCls : defaultCls
      }`}
    >
      {icon}
      {label}
    </button>
  );
}

interface Props {
  postId: number;
  debateResult: DebateResult;
  isLoggedIn: boolean;
  onStanceChange: (stance: DebateStance | null) => void;
}

export default function DebateSection({ postId, debateResult, isLoggedIn, onStanceChange }: Props) {
  const [selectedStance, setSelectedStance] = useState<DebateStance | null>(null);
  const stanceInitializedRef = useRef(false);

  const { totalDebate, proPct, conPct } = useDebateStats(debateResult);
  const stanceMutation = useDebateStance(postId);

  useEffect(() => {
    if (!stanceInitializedRef.current) {
      const initial = (debateResult.myStance as DebateStance) ?? null;
      setSelectedStance(initial);
      onStanceChange(initial);
      stanceInitializedRef.current = true;
    }
  }, [debateResult, onStanceChange]);

  const handleVote = (next: DebateStance | null, prev: DebateStance | null) => {
    setSelectedStance(next);
    onStanceChange(next);
    stanceMutation.mutate(next, {
      onError: () => {
        setSelectedStance(prev);
        onStanceChange(prev);
      },
    });
  };

  return (
    <section
      className="bg-surface-container-lowest rounded-2xl p-8 border border-surface-container"
      style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.08)" }}
    >
      {/* ---- Percentages ---- */}
      <div className="flex justify-between items-end mb-4">
        <div>
          <p className="text-sm font-semibold text-secondary mb-1">{debateResult.proLabel}</p>
          <p
            className="text-3xl font-extrabold"
            style={{
              background: "linear-gradient(90deg, #7a5645 0%, #be927e 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
              backgroundClip: "text",
            }}
          >
            {proPct}%
          </p>
        </div>
        <div className="text-right">
          <p className="text-sm font-semibold text-primary mb-1">{debateResult.conLabel}</p>
          <p
            className="text-3xl font-extrabold"
            style={{
              background: "linear-gradient(90deg, #31170a 0%, #50443f 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
              backgroundClip: "text",
            }}
          >
            {conPct}%
          </p>
        </div>
      </div>

      {/* ---- Gradient progress bar ---- */}
      <div className="mb-8">
        <DebateProgressBar proPct={proPct} conPct={conPct} />
      </div>

      {/* ---- Stance buttons / Closed state ---- */}
      {!debateResult.isClosed ? (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <StanceButton
              stance={STANCE.PRO}
              label={debateResult.proLabel}
              icon={<MdThumbUp className="text-xl" />}
              selectedStance={selectedStance}
              isLoggedIn={isLoggedIn}
              disabled={stanceMutation.isPending}
              onVote={handleVote}
            />
            <StanceButton
              stance={STANCE.CON}
              label={debateResult.conLabel}
              icon={<MdThumbDown className="text-xl" />}
              selectedStance={selectedStance}
              isLoggedIn={isLoggedIn}
              disabled={stanceMutation.isPending}
              onVote={handleVote}
            />
          </div>

          <div className="flex items-center justify-between mt-6">
            {(() => {
              const label = getDeadlineLabel(debateResult.endsAt, debateResult.isClosed, "토론 종료");
              const isCountdown = label.startsWith("D-");
              return (
                <span className="flex items-center gap-1.5 text-xs px-3 py-1 rounded-full font-semibold border bg-primary/10 text-primary border-primary/20">
                  <MdSchedule className="text-sm" />
                  {isCountdown ? <>종료까지 <span className="font-bold">{label}</span></> : <span className="font-bold">{label}</span>}
                </span>
              );
            })()}
            <span className="flex items-center gap-1.5 text-xs text-on-surface-variant">
              <MdGroup className="text-sm" />
              {totalDebate.toLocaleString()}명 참여
            </span>
          </div>
        </div>
      ) : (
        <div className="flex items-center justify-between">
          <span className="flex items-center gap-1.5 text-xs px-3 py-1 rounded-full font-semibold border bg-surface-container text-on-surface-variant border-outline-variant">
            <MdSchedule className="text-sm" />
            <span className="font-bold">토론 종료</span>
          </span>
          <span className="flex items-center gap-1.5 text-sm text-on-surface-variant">
            <MdGroup className="text-sm" />
            {totalDebate.toLocaleString()}명 참여
          </span>
        </div>
      )}
    </section>
  );
}
