import { POST_TYPE_META } from "@/types/post";
import type { PostType } from "@/types/post";
import { cn } from "@/lib/utils";

interface Props {
  type: PostType;
  className?: string;
}

export default function PostTypeBadge({ type, className }: Props) {
  const { label, color } = POST_TYPE_META[type];
  return (
    <span className={cn("shrink-0 px-2 py-1 rounded-sm text-xs font-medium", color, className)}>
      {label}
    </span>
  );
}
