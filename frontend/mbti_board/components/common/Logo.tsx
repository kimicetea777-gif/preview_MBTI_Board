import { memo } from "react";
import Link from "next/link";

interface Props {
  size?: "sm" | "md" | "lg";
  asLink?: boolean;
}

const SIZE = {
  sm: { icon: 24, gap: "gap-1.5", text: "text-base" },
  md: { icon: 32, gap: "gap-2",   text: "text-xl"  },
  lg: { icon: 44, gap: "gap-3",   text: "text-3xl" },
} as const;

function LogoIcon({ size }: { size: number }) {
  const half = size / 2;
  const pad  = size * 0.1;
  const r    = size * 0.15;

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} fill="none" aria-hidden>
      <rect x={pad}        y={pad}        width={half - pad * 1.5} height={half - pad * 1.5} rx={r} fill="#6366f1" />
      <rect x={half + pad * 0.5} y={pad}        width={half - pad * 1.5} height={half - pad * 1.5} rx={r} fill="#ec4899" />
      <rect x={pad}        y={half + pad * 0.5} width={half - pad * 1.5} height={half - pad * 1.5} rx={r} fill="#f59e0b" />
      <rect x={half + pad * 0.5} y={half + pad * 0.5} width={half - pad * 1.5} height={half - pad * 1.5} rx={r} fill="#10b981" />
    </svg>
  );
}

export default memo(function Logo({ size = "md", asLink = true }: Props) {
  const s = SIZE[size];

  const content = (
    <span className={`inline-flex items-center ${s.gap}`}>
      <LogoIcon size={s.icon} />
      <span className={`font-bold tracking-tight text-gray-900 ${s.text}`}>
        MBTI <span className="text-indigo-500">Board</span>
      </span>
    </span>
  );

  if (!asLink) return content;

  return (
    <Link href="/" className="inline-flex items-center">
      {content}
    </Link>
  );
});
