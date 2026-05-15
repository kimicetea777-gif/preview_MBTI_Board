package com.board.kimicetea.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ADMIN(100),
    TESTER(50),
    USER(10),
    GUEST(1);

    private final int weight;
}
