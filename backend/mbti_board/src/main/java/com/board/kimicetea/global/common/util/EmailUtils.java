package com.board.kimicetea.global.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 이메일 관련 유틸
 * 주의: @앞 2자만 노출하고 나머지 로컬 파트는 *** 처리 (개인정보 보호)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmailUtils {

    /** 예) abcdef@gmail.com → ab***@gmail.com */
    public static String mask(final String email) {
        final int atIndex = email.indexOf('@');
        final String local = email.substring(0, atIndex);
        final String domain = email.substring(atIndex);
        final String visiblePart = local.length() > 2 ? local.substring(0, 2) : local;
        return visiblePart + "***" + domain;
    }
}
