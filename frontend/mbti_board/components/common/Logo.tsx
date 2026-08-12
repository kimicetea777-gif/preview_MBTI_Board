import { memo } from "react";
import Link from "next/link";
import Image from "next/image";
import { APP_NAME } from "@/constants/site";

interface Props {
  size?: "sm" | "md" | "lg" | "xl" | "icon";
  asLink?: boolean;
}

// logo_674_122.png의 실제 비율(674:122 ≈ 5.52:1)에 맞춘 width. height가 실제 렌더 크기를 결정한다.
// icon은 정사각형(logo_256_256.png) — 사이드바 접힘 등 좁은 공간용.
const SIZE = {
  sm: { width: 166, height: 30 },
  md: { width: 210, height: 38 },
  lg: { width: 287, height: 52 },
  xl: { width: 387, height: 70 },
  icon: { width: 32, height: 32 },
} as const;

export default memo(function Logo({ size = "md", asLink = true }: Props) {
  const s = SIZE[size];

  const content = (
    <Image
      src={size === "icon" ? "/images/logo_256_256.png" : "/images/logo_674_122.png"}
      alt={APP_NAME}
      width={s.width}
      height={s.height}
      style={{ width: "auto" }}
      priority
    />
  );

  if (!asLink) return content;

  return (
    <Link href="/" className="inline-flex items-center">
      {content}
    </Link>
  );
});
