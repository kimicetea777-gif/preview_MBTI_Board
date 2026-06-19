import { HiExclamationCircle } from "react-icons/hi";

interface Props {
  message: string | null | undefined;
  className?: string;
}

export default function ErrorMessage({ message, className }: Props) {
  if (!message) return null;
  return (
    <p
      role="alert"
      className={`text-xs text-destructive border border-destructive/40 bg-destructive/5 rounded-md px-3 py-2 flex items-center gap-1.5 ${className ?? ""}`}
    >
      <HiExclamationCircle className="text-base shrink-0" />
      {message}
    </p>
  );
}
