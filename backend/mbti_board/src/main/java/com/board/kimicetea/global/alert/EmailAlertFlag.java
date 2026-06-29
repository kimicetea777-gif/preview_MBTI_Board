package com.board.kimicetea.global.alert;

import java.util.concurrent.atomic.AtomicBoolean;

/** 이메일 에러 알림 전송 여부 플래그 — Logback은 Spring 컨텍스트 밖이므로 static으로 관리 */
public class EmailAlertFlag {

    private static final AtomicBoolean enabled = new AtomicBoolean(true);

    private EmailAlertFlag() {}

    public static boolean isEnabled() {
        return enabled.get();
    }

    public static void mute() {
        enabled.set(false);
    }

    public static void unmute() {
        enabled.set(true);
    }
}
