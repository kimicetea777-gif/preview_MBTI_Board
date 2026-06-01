package com.board.kimicetea.global.common;

public final class ValidationConstants {

    private ValidationConstants() {}

    /** 비밀번호 정책: 영문+숫자 포함 10자 이상 */
    public static final String PASSWORD_REGEXP = "^(?=.*[a-zA-Z])(?=.*\\d).{10,}$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 영문+숫자 포함 10자 이상이어야 합니다.";
}
