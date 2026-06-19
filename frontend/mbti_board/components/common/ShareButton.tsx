"use client";

import { useState } from "react";
import { LuShare2, LuCheck, LuCopy } from "react-icons/lu";

interface ShareButtonProps {
  /** Relative path (e.g. /mbti/INTJ) — full URL is computed at click time */
  path: string;
  label?: string;
  className?: string;
}

export function ShareButton({
  path,
  label = "공유하기",
  className = "",
}: ShareButtonProps) {
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [url, setUrl] = useState("");

  function handleClick() {
    setUrl(`${window.location.origin}${path}`);
    setOpen((prev) => !prev);
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => {
        setCopied(false);
        setOpen(false);
      }, 1500);
    } catch { }
  }

  const displayUrl = url.replace(/^https?:\/\//, "");

  return (
    <div className="relative">
      <button
        onClick={handleClick}
        className={`flex items-center gap-2 rounded-xl border border-border px-5 py-3.5 text-sm font-semibold text-foreground hover:bg-accent transition-colors ${className}`}
      >
        <LuShare2 className="text-base" />
        {label}
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute top-full left-1/2 -translate-x-1/2 mt-2 z-50 bg-card border border-border rounded-xl shadow-lg p-3 w-72">
            <div className="flex items-center gap-2">
              <span className="flex-1 text-xs text-muted-foreground bg-muted rounded-lg px-3 py-2 truncate min-w-0">
                {displayUrl}
              </span>
              <button
                onClick={handleCopy}
                className="shrink-0 flex items-center gap-1.5 rounded-lg bg-primary/10 hover:bg-primary/20 text-primary px-3 py-2 text-xs font-semibold transition-colors"
              >
                {copied ? (
                  <>
                    <LuCheck className="text-sm" />
                    복사됨
                  </>
                ) : (
                  <>
                    <LuCopy className="text-sm" />
                    복사
                  </>
                )}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
