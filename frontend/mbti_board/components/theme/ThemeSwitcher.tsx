"use client";

import { useTheme } from "next-themes";
import { useSyncExternalStore } from "react";
import { cn } from "@/lib/utils";

const useHasMounted = () =>
  useSyncExternalStore(() => () => {}, () => true, () => false);

const THEMES = [
  { id: "theme-earthen", label: "어스",   bg: "#fbf9f5", accent: "#31170a" },
  { id: "light",         label: "라이트", bg: "#ffffff",  accent: "#111827" },
  { id: "dark",          label: "다크",   bg: "#0a0a0a",  accent: "#f5f5f5" },
  { id: "theme-navy",    label: "네이비", bg: "#0A0F1E",  accent: "#E4F0F6" },
  { id: "theme-rose",    label: "로즈",   bg: "#1A0A0A",  accent: "#E8729A" },
] as const;

export default function ThemeSwitcher() {
  const { theme, setTheme } = useTheme();
  const mounted = useHasMounted();

  if (!mounted) return null;

  return (
    <div className="flex items-center gap-2">
      {THEMES.map((t) => (
        <button
          key={t.id}
          title={t.label}
          onClick={() => setTheme(t.id)}
          className={cn(
            "w-7 h-7 rounded-full border-2 transition-all",
            theme === t.id ? "border-ring scale-110" : "border-transparent hover:scale-105"
          )}
          style={{ background: `linear-gradient(135deg, ${t.bg} 50%, ${t.accent} 50%)` }}
        />
      ))}
    </div>
  );
}
