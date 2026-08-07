package com.board.kimicetea.domain.user.enums;

public enum UserState {

    // length : 50
    NORMAL, // 정상 상태
    PENDING, // 가입 대기: 이메일 인증이 필요하거나 가입 절차가 마무리되지 않은 상태
    INACTIVE, // 휴면 상태: 장기간 접속하지 않아 데이터가 아카이브된 상태
    WITHDRAWN, // 탈퇴 상태: 사용자가 탈퇴를 요청하여 데이터 삭제 대기 중인 상태
    LOCKED, // 일시 잠금: 비정상적인 접근 감지 시 본인 확인을 위해 일시적으로 잠긴 상태
    SHADOW_BANNED, // 쉐도우 밴: 유저는 플레이 가능하지만, 로그 감시
    PERMANENT_BAN; // 영구 정지: 운영 정책 위반으로 영구적으로 접근이 차단된 상태

    /**
     * 요청 자체를 막아야 하는 상태인지 — CustomOAuth2UserService.validateAccountState의 switch와
     * 동일한 판정을 세션 캐시(role/blocked)에서도 재사용하기 위한 단일 출처.
     * 주의: SHADOW_BANNED는 콘텐츠 노출 제한일 뿐 본인 접근은 막지 않으므로 포함하지 않음
     */
    public boolean isBlocked() {
        return this == LOCKED || this == PERMANENT_BAN || this == INACTIVE || this == WITHDRAWN;
    }
}
