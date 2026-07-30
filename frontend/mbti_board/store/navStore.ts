import { create } from "zustand";
import { isBrowser } from "@/lib/utils";

const SIDEBAR_COLLAPSED_KEY = "sidebarCollapsed";

interface NavStore {
  drawerOpen: boolean;
  openDrawer: () => void;
  closeDrawer: () => void;
  // 서버 렌더·첫 클라이언트 렌더는 항상 false로 고정 — 마운트 이후 localStorage 값으로 동기화 (hydration mismatch 방지)
  sidebarCollapsed: boolean;
  setSidebarCollapsed: (collapsed: boolean) => void;
  toggleSidebarCollapsed: () => void;
}

export const useNavStore = create<NavStore>((set, get) => ({
  drawerOpen: false,
  openDrawer: () => set({ drawerOpen: true }),
  closeDrawer: () => set({ drawerOpen: false }),
  sidebarCollapsed: false,
  setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
  toggleSidebarCollapsed: () => {
    const next = !get().sidebarCollapsed;
    if (isBrowser()) localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(next));
    set({ sidebarCollapsed: next });
  },
}));

export function getStoredSidebarCollapsed(): boolean {
  if (!isBrowser()) return false;
  return localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === "true";
}
