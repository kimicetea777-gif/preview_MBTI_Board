import { AUTH_SERVER_URL } from "@/constants/site";
import { LuKeyRound } from "react-icons/lu";

export default function MyPasswordSection() {
  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm">
      <h2 className="font-heading text-base font-bold text-foreground mb-3 flex items-center gap-1.5">
        <LuKeyRound className="text-lg" />
        비밀번호 재설정
      </h2>
      <div className="flex items-center justify-between gap-4 rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3">
        <span className="text-xs text-muted-foreground">인증 메일을 받아 비밀번호를 재설정할 수 있습니다.</span>
        <a
          href={`${AUTH_SERVER_URL}/account/reset-password`}
          target="_blank"
          rel="noopener noreferrer"
          className="shrink-0 rounded-lg border border-outline-variant px-3 py-1.5 text-xs font-medium text-foreground hover:bg-accent transition-colors"
        >
          재설정
        </a>
      </div>
    </div>
  );
}
