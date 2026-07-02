package com.board.kimicetea.global.alert;

/** 이메일 알림 성격 분류 — 타입별 독립 flag 관리 및 Marker 키로 사용 */
public enum AlertType {
    JWT_TAMPER,            // 토큰 서명 위조·형식 오류
    UNHANDLED_ERROR,       // 처리되지 않은 예외 (500)
    DB_QUERY_SLOW,         // Repository 메서드 실행 시간 임계치 초과
    BRUTE_FORCE_LOGIN,     // 로그인 Rate Limit 초과 — 계정 탈취 시도 의심
    PASSWORD_RESET_ABUSE,  // 비밀번호 재설정 Rate Limit 초과 — 스팸·공격 의심
    ACCOUNT_LOCKED,        // UserState.LOCKED 상태로 접근 시도
    RATE_LIMIT_EXCEEDED,   // 기타 API Rate Limit 초과
    REDIS_CONNECTION_FAIL  // Redis 연결 실패 — 토큰 검증 불가 상태
}
