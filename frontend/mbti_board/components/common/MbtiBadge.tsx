import type { MbtiVerified } from "@/types/user";

const VERIFIED_META: Record<MbtiVerified, { color: string; title: string; showMbti: boolean }> = {
  TESTED:  { color: "#2E5C40", title: "검사인증", showMbti: true },
  SELF:    { color: "#7DB27A", title: "자기입력", showMbti: true },
  DEFAULT: { color: "#A8A29E", title: "미인증",   showMbti: false },
};

interface MbtiBadgeProps {
  mbti: string;
  verified: MbtiVerified;
  className?: string;
}

export default function MbtiBadge({ mbti, verified, className = "" }: MbtiBadgeProps) {
  const { color, title, showMbti } = VERIFIED_META[verified];

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium text-white ${className}`}
      style={{ backgroundColor: color }}
      title={title}
    >
      {showMbti ? mbti : title}
    </span>
  );
}
