import type { MbtiVerified } from "@/types/user";

const VERIFIED_COLOR: Record<MbtiVerified, string> = {
  TESTED: "#2E5C40",
  SELF: "#7DB27A",
  DEFAULT: "#A8A29E",
};

const VERIFIED_LABEL: Record<MbtiVerified, string> = {
  TESTED: "검사인증",
  SELF: "자기입력",
  DEFAULT: "미인증",
};

interface MbtiBadgeProps {
  mbti: string;
  verified: MbtiVerified;
  className?: string;
}

export default function MbtiBadge({ mbti, verified, className = "" }: MbtiBadgeProps) {
  const color = VERIFIED_COLOR[verified];
  const label = (verified === "DEFAULT") ? "미인증" : mbti;

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium text-white ${className}`}
      style={{ backgroundColor: color }}
      title={VERIFIED_LABEL[verified]}
    >
      {label}
    </span>
  );
}
