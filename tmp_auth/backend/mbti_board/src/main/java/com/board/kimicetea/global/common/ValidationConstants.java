package com.board.kimicetea.global.common;

public final class ValidationConstants {

    private ValidationConstants() {}

    /** 비밀번호 정책: 영문+숫자 포함 10자 이상 */
    public static final String PASSWORD_REGEXP = "^(?=.*[a-zA-Z])(?=.*\\d).{10,}$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 영문+숫자 포함 10자 이상이어야 합니다.";

    /** 별명 정책: 한글·영문·숫자·언더바(_)만 허용, 2~12자 */
    public static final String NICKNAME_REGEXP = "^[가-힣a-zA-Z0-9_]{2,12}$";
    public static final String NICKNAME_MESSAGE = "별명은 한글, 영문, 숫자, 언더바(_)만 사용할 수 있습니다.";

    /** MBTI 16가지 타입만 허용 — FunResponseSubmitRequest 등 정확한 MBTI 타입이 필요한 곳 */
    public static final String MBTI_REGEXP = "^(E|I)(S|N)(T|F)(J|P)$";

    /** MBTI 16가지 타입 + "NONE"(미선택) 허용 — 회원가입·프로필 수정 등 */
    public static final String MBTI_OR_NONE_REGEXP = "^(NONE|(E|I)(S|N)(T|F)(J|P))$";

    public static final String MBTI_MESSAGE = "올바른 MBTI 유형이 아닙니다";
}
