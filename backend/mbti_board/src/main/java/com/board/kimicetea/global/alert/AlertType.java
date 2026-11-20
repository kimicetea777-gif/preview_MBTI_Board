package com.board.kimicetea.global.alert;

/** 이메일 알림 성격 분류 — 타입별 독립 flag 관리 및 Marker 키로 사용 */
public enum AlertType {
    JWT_TAMPER,            // 토큰 서명 위조·형식 오류
    UNHANDLED_ERROR,       // 처리되지 않은 예외 (500)
    BRUTE_FORCE_LOGIN,     // 로그인 Rate Limit 초과 — 계정 탈취 시도 의심
    PASSWORD_RESET_ABUSE,  // 비밀번호 재설정 Rate Limit 초과 — 스팸·공격 의심
    ACCOUNT_LOCKED,        // UserState.LOCKED 상태로 접근 시도
    RATE_LIMIT_EXCEEDED,   // 기타 API Rate Limit 초과
    REDIS_CONNECTION_FAIL, // Redis 연결 실패 — 토큰 검증 불가 상태
    IMAGE_SYNC_FAIL,       // Image Server 이미지 확정·정리 워커 실패 — 재시도 소진, 즉시 실패 4xx, 워커 실행 오류
    MEMBERSHIP_SYNC_FAIL,  // 멤버십 이벤트 반영 실패가 이어짐(재시도 5회째) 또는 폴러 정지 — 큐가 비면 자동 재활성화
    MEMBERSHIP_SYNC_STALE  // 04:15 점검: 폴링 실패로 확정 배치를 건너뜀, 또는 24시간 넘게 안 풀린 이벤트
}
