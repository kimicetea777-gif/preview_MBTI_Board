"use client";

import { useState, useEffect, useRef } from "react";
import { FiInfo } from "react-icons/fi";
import {
  USER_STATE_LABELS,
  USER_STATE_COLORS,
  USER_STATE_DESCRIPTIONS,
} from "@/types/admin";
import type { UserState } from "@/types/user";

const STATE_OPTIONS: UserState[] = [
  "NORMAL", "PENDING", "INACTIVE", "WITHDRAWN", "LOCKED", "SHADOW_BANNED", "PERMANENT_BAN",
];

export default function StateInfoPopover() {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  return (
    <div ref={ref} className="relative inline-flex">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex items-center text-on-surface-variant/60 hover:text-on-surface-variant"
      >
        <FiInfo size={13} />
      </button>

      {open && (
        <div className="absolute left-0 top-5 z-50 w-72 rounded-lg border border-outline-variant bg-surface-container-lowest p-3 shadow-lg">
          <ul className="space-y-2">
            {STATE_OPTIONS.map((s) => (
              <li key={s} className="flex gap-2">
                <span
                  className="mt-0.5 shrink-0 rounded px-1.5 py-0.5 text-xs font-medium text-white"
                  style={{ backgroundColor: USER_STATE_COLORS[s] }}
                >
                  {USER_STATE_LABELS[s]}
                </span>
                <span className="text-xs text-on-surface-variant">{USER_STATE_DESCRIPTIONS[s]}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
