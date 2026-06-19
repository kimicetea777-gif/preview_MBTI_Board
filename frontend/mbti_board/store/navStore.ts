import { create } from "zustand";

interface NavStore {
  drawerOpen: boolean;
  openDrawer: () => void;
  closeDrawer: () => void;
}

export const useNavStore = create<NavStore>((set) => ({
  drawerOpen: false,
  openDrawer: () => set({ drawerOpen: true }),
  closeDrawer: () => set({ drawerOpen: false }),
}));
