import type { IconType } from "react-icons";

interface IconDividerProps {
  icon: IconType;
  iconSize?: "sm" | "md" | "lg";
  className?: string;
}

const ICON_SIZE = {
  sm: "h-5 w-5",
  md: "h-7 w-7",
  lg: "h-9 w-9",
} as const;

export default function IconDivider({ icon: Icon, iconSize = "md", className = "my-12" }: IconDividerProps) {
  return (
    <div className={`flex items-center justify-center ${className}`}>
      <div className="h-px flex-1 bg-ring/20" />
      <Icon className={`mx-4 ${ICON_SIZE[iconSize]} text-ring/50`} />
      <div className="h-px flex-1 bg-ring/20" />
    </div>
  );
}
