package com.board.kimicetea.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MbtiVerified {
    TESTED("#2E5C40"),
    SELF("#7DB27A"),
    DEFAULT("#A8A29E");

    private final String colorCode;
}
