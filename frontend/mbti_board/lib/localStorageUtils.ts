import { useSyncExternalStore } from "react";

export function useLocalStorageSnapshot<T>(getter: () => T, serverFallback: T): T {
  return useSyncExternalStore(() => () => {}, getter, () => serverFallback);
}
