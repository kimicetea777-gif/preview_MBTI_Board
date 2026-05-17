package com.board.kimicetea.domain.user.enums;

public enum UserState {

    // length : 50
    NORMAL, // 정상 상태
    PENDING, // 가입 대기: 이메일 인증이 필요하거나 가입 절차가 마무리되지 않은 상태
    INACTIVE, // 휴면 상태: 장기간 접속하지 않아 데이터가 아카이브된 상태
    WITHDRAWN, // 탈퇴 상태: 사용자가 탈퇴를 요청하여 데이터 삭제 대기 중인 상태
    LOCKED, // 일시 잠금: 비정상적인 접근 감지 시 본인 확인을 위해 일시적으로 잠긴 상태
    SHADOW_BANNED, // 쉐도우 밴: 유저는 플레이 가능하지만, 로그 감시
    PERMANENT_BAN // 영구 정지: 운영 정책 위반으로 영구적으로 접근이 차단된 상태
}
