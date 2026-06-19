import Image from "next/image";
import type { MbtiMetadata } from "@/types/mbti";

interface Props {
  mbti: MbtiMetadata | null | undefined;
  size?: "sm" | "lg" | "xl";
  className?: string;
}

const SIZE_MAP = {
  sm: { wh: "w-24 h-24", px: 96 },
  lg: { wh: "h-40 w-40", px: 160 },
  xl: { wh: "h-56 w-56", px: 224 },
} as const;

export default function MbtiAvatar({ mbti, size = "lg", className = "" }: Props) {
  const { wh, px } = SIZE_MAP[size];

  return (
    <div
      className={`${wh} rounded-full bg-surface-container-high ${className}`}
      style={mbti?.colorCode ? { background: `radial-gradient(circle, ${mbti.colorCode}60 0%, transparent 70%)` } : undefined}
    >
      {mbti?.imageUrl && (
        <div
          className="h-full w-full overflow-hidden rounded-full"
          style={{
            maskImage: "radial-gradient(circle, black 55%, transparent 100%)",
            WebkitMaskImage: "radial-gradient(circle, black 55%, transparent 100%)",
          }}
        >
          <Image
            src={mbti.imageUrl}
            alt={mbti.id}
            width={px}
            height={px}
            className="h-full w-full object-cover"
          />
        </div>
      )}
    </div>
  );
}
