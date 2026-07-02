package com.board.kimicetea.domain.user;

public final class TermsPolicy {

    private TermsPolicy() {}

    /** 이용약관 현재 버전 — 내용이 바뀌면 증가시키고 기존 유저 재동의 흐름에서 기준값으로 사용 */
    public static final int CURRENT_TERMS_VERSION = 1;

    /** 개인정보처리방침 현재 버전 */
    public static final int CURRENT_PRIVACY_VERSION = 1;
}
