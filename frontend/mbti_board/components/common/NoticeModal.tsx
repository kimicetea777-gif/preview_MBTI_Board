"use client";

import { useState } from "react";
import { MdClose, MdChevronLeft, MdChevronRight } from "react-icons/md";
import { useNotice } from "@/hooks/useNotice";

export default function NoticeModal() {
  const { notices, isVisible, close, hideFor24Hours } = useNotice();
  const [index, setIndex] = useState(0);

  if (!isVisible) return null;

  const total = notices.length;
  // 백그라운드 재조회로 notices가 줄어들어 index가 범위를 벗어나면 첫 공지로 폴백 (effect 없이 렌더 중 파생)
  const safeIndex = index < total ? index : 0;
  const current = notices[safeIndex];

  function prev() {
    setIndex((i) => (i - 1 + total) % total);
  }

  function next() {
    setIndex((i) => (i + 1) % total);
  }

  return (
    <>
      {/* 오버레이 */}
      <div className="fixed inset-0 z-50 bg-black/40" onClick={close} />

      {/* 모달 */}
      <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-outline-variant bg-surface-container-lowest shadow-2xl">

        {/* 헤더 */}
        <header className="flex items-center justify-between border-b border-outline-variant px-5 py-4">
          <h2 className="text-sm font-semibold text-on-surface">공지사항</h2>
          <button
            onClick={close}
            className="rounded-full p-1 text-on-surface-variant hover:bg-accent transition-colors"
            aria-label="닫기"
          >
            <MdClose size={20} />
          </button>
        </header>

        {/* 본문 */}
        <article className="px-5 py-5 min-h-40">
          <h3 className="mb-3 text-base font-semibold text-on-surface">{current.title}</h3>
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-on-surface-variant">
            {current.content}
          </p>
        </article>

        {/* 캐러셀 네비게이션 (공지 2개 이상일 때만) */}
        {total > 1 && (
          <div className="flex items-center justify-center gap-3 pb-4">
            <button
              onClick={prev}
              className="rounded-full p-1 text-on-surface-variant hover:bg-accent transition-colors"
              aria-label="이전 공지"
            >
              <MdChevronLeft size={22} />
            </button>

            {/* Dot indicator */}
            <div className="flex gap-1.5">
              {notices.map((notice, i) => (
                <button
                  key={notice.id}
                  onClick={() => setIndex(i)}
                  aria-label={`${i + 1}번 공지`}
                  className={`h-2 rounded-full transition-all ${
                    i === safeIndex
                      ? "w-5 bg-primary"
                      : "w-2 bg-outline-variant hover:bg-outline"
                  }`}
                />
              ))}
            </div>

            <button
              onClick={next}
              className="rounded-full p-1 text-on-surface-variant hover:bg-accent transition-colors"
              aria-label="다음 공지"
            >
              <MdChevronRight size={22} />
            </button>
          </div>
        )}

        {/* 푸터 */}
        <footer className="flex items-center justify-between border-t border-outline-variant px-5 py-3">
          <button
            onClick={hideFor24Hours}
            className="text-xs text-on-surface-variant underline hover:text-foreground transition-colors cursor-pointer"
          >
            24시간 동안 보지 않기
          </button>
          {total > 1 && (
            <span className="text-xs text-on-surface-variant">
              {safeIndex + 1} / {total}
            </span>
          )}
        </footer>
      </div>
    </>
  );
}
