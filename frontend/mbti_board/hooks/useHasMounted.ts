"use client";

import { useSyncExternalStore } from "react";

function subscribe() {
  return () => {};
}

function getSnapshot() {
  return true;
}

function getServerSnapshot() {
  return false;
}

// 서버 렌더·하이드레이션 시점엔 항상 false, 커밋 이후 클라이언트 렌더부터 true
// (setState-in-effect 없이 hydration-safe하게 "마운트 여부" 판별)
export function useHasMounted(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
}
