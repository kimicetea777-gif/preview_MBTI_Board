"use client";

import { MdNotificationsActive, MdNotificationsOff, MdInfo } from "react-icons/md";
import { ALERT_TYPE, type AlertType } from "@/types/admin";
import { useAlertStatus, useMuteAlert, useUnmuteAlert } from "@/hooks/useAdmin";

const ALERT_META: Record<AlertType, { label: string; description: string }> = {
  [ALERT_TYPE.JWT_TAMPER]:           { label: "JWT 위조 감지",        description: "토큰 서명 위조·형식 오류 감지 시 알림" },
  [ALERT_TYPE.UNHANDLED_ERROR]:      { label: "미처리 예외",           description: "처리되지 않은 예외 (500) 발생 시 알림" },
  [ALERT_TYPE.DB_QUERY_SLOW]:        { label: "슬로우 쿼리",           description: "DB 쿼리 실행 시간 임계치 초과 시 알림" },
  [ALERT_TYPE.BRUTE_FORCE_LOGIN]:    { label: "로그인 브루트 포스",    description: "로그인 요청 제한 초과 시 알림" },
  [ALERT_TYPE.PASSWORD_RESET_ABUSE]: { label: "비밀번호 재설정 남용",  description: "비밀번호 재설정 요청 제한 초과 시 알림" },
  [ALERT_TYPE.ACCOUNT_LOCKED]:       { label: "잠긴 계정 접근",        description: "잠긴 계정으로 로그인 시도 감지 시 알림" },
  [ALERT_TYPE.RATE_LIMIT_EXCEEDED]:  { label: "API 요청 제한 초과",    description: "기타 API Rate Limit 초과 시 알림" },
  [ALERT_TYPE.REDIS_CONNECTION_FAIL]:{ label: "Redis 연결 실패",       description: "Redis 연결 실패로 토큰 검증 불가 시 알림" },
};

export default function AdminAlertContent() {
  const { data: status } = useAlertStatus();
  const { mutate: mute, isPending: isMuting, variables: mutingType } = useMuteAlert();
  const { mutate: unmute, isPending: isUnmuting, variables: unmutingType } = useUnmuteAlert();

  return (
    <div className="p-6 max-w-3xl">
      <h1 className="text-2xl font-bold text-foreground mb-2">이메일 알림 설정</h1>

      <div className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-950/30 px-4 py-3 mb-6 text-sm text-amber-800 dark:text-amber-300">
        <MdInfo size={18} className="shrink-0 mt-0.5" />
        <span>
          음소거 상태는 서버 재시작 시 자동으로 초기화됩니다.
          첫 알림 발송 후 자동으로 음소거되며, 수동으로 재활성화해야 합니다.
        </span>
      </div>

      <section className="rounded-2xl border border-outline-variant bg-card divide-y divide-outline-variant">
        {(Object.values(ALERT_TYPE) as AlertType[]).map((type) => {
          const enabled = status[type] ?? true;
          const meta = ALERT_META[type];
          const isPendingThis = (isMuting && mutingType === type) || (isUnmuting && unmutingType === type);

          return (
            <div key={type} className="flex items-center justify-between gap-4 px-5 py-4">
              <div className="flex items-center gap-3 min-w-0">
                <div className={`shrink-0 rounded-full p-2 ${
                  enabled
                    ? "bg-emerald-50 text-emerald-600 dark:bg-emerald-950"
                    : "bg-surface-variant text-on-surface-variant"
                }`}>
                  {enabled
                    ? <MdNotificationsActive size={18} />
                    : <MdNotificationsOff size={18} />
                  }
                </div>
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-medium text-foreground">{meta.label}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                      enabled
                        ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900 dark:text-emerald-300"
                        : "bg-surface-variant text-on-surface-variant"
                    }`}>
                      {enabled ? "활성화" : "음소거"}
                    </span>
                  </div>
                  <p className="text-xs text-on-surface-variant">{meta.description}</p>
                </div>
              </div>
              <button
                disabled={isPendingThis}
                onClick={() => enabled ? mute(type) : unmute(type)}
                className={`shrink-0 text-sm px-4 py-1.5 rounded-xl font-medium border transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${
                  enabled
                    ? "border-outline-variant text-on-surface-variant hover:bg-accent"
                    : "border-emerald-300 text-emerald-700 hover:bg-emerald-50 dark:border-emerald-700 dark:text-emerald-400 dark:hover:bg-emerald-950"
                }`}
              >
                {isPendingThis ? "처리 중..." : enabled ? "음소거" : "활성화"}
              </button>
            </div>
          );
        })}
      </section>
    </div>
  );
}
