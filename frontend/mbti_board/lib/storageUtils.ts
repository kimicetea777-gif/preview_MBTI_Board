import { isBrowser } from "@/lib/utils";

export const sessionStorage = {
  get: (key: string): string | null => {
    if (!isBrowser()) return null;
    return window.sessionStorage.getItem(key);
  },
  set: (key: string, value: string): void => {
    if (!isBrowser()) return;
    window.sessionStorage.setItem(key, value);
  },
  remove: (key: string): void => {
    if (!isBrowser()) return;
    window.sessionStorage.removeItem(key);
  },
  has: (key: string): boolean => {
    if (!isBrowser()) return false;
    return window.sessionStorage.getItem(key) !== null;
  },
  getJson: <T>(key: string): T | null => {
    if (!isBrowser()) return null;
    const raw = window.sessionStorage.getItem(key);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as T;
    } catch { return null; }
  },
};
