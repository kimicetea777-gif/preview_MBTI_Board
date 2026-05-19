package com.board.kimicetea.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MbtiVerified {
    TESTED("#2E5C40"),   // 사이트 내 검사 완료(회원)
    SELF("#7DB27A"),     // 직접 입력 (회원)
    DEFAULT("#A8A29E");  // 미설정 회원 / 비회원

    private final String colorCode;
}
