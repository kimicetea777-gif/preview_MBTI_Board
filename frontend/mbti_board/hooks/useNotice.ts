"use client";

import { useState, useSyncExternalStore } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchActiveNotices } from "@/lib/api/notice";
import { NOTICE_HIDDEN_KEY } from "@/types/notice";
import type { NoticeResponse } from "@/types/notice";

function checkHidden(): boolean {
  try {
    const raw = localStorage.getItem(NOTICE_HIDDEN_KEY);
    if (!raw) return false;
    return Date.now() < Number(raw);
  } catch {
    return false;
  }
}

export function hideNoticeFor24Hours() {
  try {
    localStorage.setItem(NOTICE_HIDDEN_KEY, String(Date.now() + 24 * 60 * 60 * 1000));
  } catch {
    // localStorage 접근 불가 환경에서는 무시
  }
}

// subscribe는 모듈 레벨 상수 — 렌더마다 새 참조 생성 방지
const noopSubscribe = () => () => {};

// useSyncExternalStore: SSR은 항상 true(hidden), 클라이언트는 localStorage 판별
function useIsHiddenByUser(): boolean {
  return useSyncExternalStore(
    noopSubscribe,
    () => checkHidden(),  // 클라이언트 스냅샷
    () => true,           // 서버 스냅샷 — hydration 전에는 숨김
  );
}

interface UseNoticeResult {
  notices: NoticeResponse[];
  isVisible: boolean;
  close: () => void;
  hideFor24Hours: () => void;
}

export function useNotice(): UseNoticeResult {
  const hiddenByUser = useIsHiddenByUser();
  const [closed, setClosed] = useState(false);

  const { data: notices = [] } = useQuery({
    queryKey: ["notices", "active"],
    queryFn: fetchActiveNotices,
    staleTime: 5 * 60 * 1000,
    enabled: !hiddenByUser,
  });

  const isVisible = !hiddenByUser && !closed && notices.length > 0;

  function close() {
    setClosed(true);
  }

  function hideFor24Hours() {
    hideNoticeFor24Hours();
    setClosed(true);
  }

  return { notices, isVisible, close, hideFor24Hours };
}
