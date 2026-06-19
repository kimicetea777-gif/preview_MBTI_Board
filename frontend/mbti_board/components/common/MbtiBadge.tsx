"use client";

import { useState, useCallback } from "react";
import { createPortal } from "react-dom";
import type { MbtiVerified } from "@/types/user";
import MbtiTypePreviewCard from "@/components/mbti/MbtiTypePreviewCard";
import { getMbtiById } from "@/constants/mbtiData";

const VERIFIED_META: Record<MbtiVerified, { color: string; title: string; showMbti: boolean }> = {
  TESTED:  { color: "#2E5C40", title: "검사인증", showMbti: true },
  SELF:    { color: "#7DB27A", title: "자기입력", showMbti: true },
  DEFAULT: { color: "#A8A29E", title: "미인증",   showMbti: false },
};

interface MbtiBadgeProps {
  mbti: string;
  verified: MbtiVerified;
  className?: string;
}

const CARD_WIDTH = 208; // w-52

export default function MbtiBadge({ mbti, verified, className = "" }: MbtiBadgeProps) {
  const { color, title, showMbti } = VERIFIED_META[verified];
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState({ top: 0, left: 0 });

  const mbtiData = showMbti ? getMbtiById(mbti) : undefined;

  const handleClick = useCallback(
    (e: React.MouseEvent<HTMLSpanElement>) => {
      if (!showMbti) return;
      e.stopPropagation();
      const rect = e.currentTarget.getBoundingClientRect();
      const left =
        rect.left + CARD_WIDTH > window.innerWidth ? rect.right - CARD_WIDTH : rect.left;
      setPos({ top: rect.bottom + 8, left });
      setOpen((v) => !v);
    },
    [showMbti],
  );

  return (
    <>
      <span
        className={`inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-white ${showMbti ? "cursor-pointer" : ""} ${className}`}
        style={{ backgroundColor: color }}
        title={title}
        onClick={handleClick}
      >
        {showMbti ? mbti : title}
      </span>
      {open && showMbti && typeof document !== "undefined" &&
        createPortal(
          <>
            <div
              className="fixed inset-0 z-40"
              onClick={(e) => { e.stopPropagation(); setOpen(false); }}
            />
            <div className="fixed z-50 w-52" style={{ top: pos.top, left: pos.left }}>
              {mbtiData && <MbtiTypePreviewCard mbti={mbtiData} />}
            </div>
          </>,
          document.body,
        )}
    </>
  );
}
