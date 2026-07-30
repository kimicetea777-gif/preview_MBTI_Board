import { memo } from "react";
import Link from "next/link";
import Image from "next/image";
import { APP_NAME } from "@/constants/site";

interface Props {
  size?: "sm" | "md" | "lg" | "xl";
  asLink?: boolean;
}

const SIZE = {
  sm: { width: 60,  height: 30 },
  md: { width: 110, height: 38 },
  lg: { width: 150, height: 52 },
  xl: { width: 200, height: 70 },
} as const;

export default memo(function Logo({ size = "md", asLink = true }: Props) {
  const s = SIZE[size];

  const content = (
    <Image
      src="/images/logo.png"
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
