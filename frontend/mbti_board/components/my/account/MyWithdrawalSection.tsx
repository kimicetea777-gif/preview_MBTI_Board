"use client";

import InfoBanner from "@/components/common/InfoBanner";
import { AUTH_SERVER_URL } from "@/constants/site";

const AS_ACCOUNT_URL = `${AUTH_SERVER_URL}/account`;

export default function MyWithdrawalSection() {
  return (
    <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-4 space-y-3">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-foreground">회원 탈퇴</p>
          <p className="text-xs text-muted-foreground mt-0.5">
            탈퇴는 계정 관리 페이지에서 연동 해제를 통해 진행됩니다.
          </p>
        </div>
        <a
          href={AS_ACCOUNT_URL}
          className="shrink-0 rounded-lg border border-destructive/40 px-3 py-1.5 text-xs font-medium text-destructive hover:bg-destructive/10 transition-colors"
        >
          탈퇴하기
        </a>
      </div>
      <InfoBanner items={[
        { variant: "info", text: "탈퇴는 계정 관리 페이지의 연동 서비스 해제를 통해 진행됩니다." },
        { variant: "info", text: "연동 해제 후 30일 안에 다시 로그인하면 계정이 그대로 복구됩니다." },
        { variant: "error", text: "30일이 지나면 개인 정보가 자동으로 삭제되며 복구할 수 없습니다." },
      ]} />
    </div>
  );
}
