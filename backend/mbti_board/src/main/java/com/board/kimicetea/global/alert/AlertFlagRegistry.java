package com.board.kimicetea.global.alert;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AlertType별 이메일 알림 활성화 플래그 레지스트리 — Logback은 Spring 컨텍스트 밖이므로 static으로 관리
 * 주의: 서버 재시작 시 모든 플래그 true로 초기화
 */
public class AlertFlagRegistry {

    private static final Map<AlertType, AtomicBoolean> FLAGS = new EnumMap<>(AlertType.class);

    static {
        for (final AlertType type : AlertType.values()) {
            FLAGS.put(type, new AtomicBoolean(true));
        }
    }

    private AlertFlagRegistry() {}

    public static boolean isEnabled(final AlertType type) {
        return FLAGS.get(type).get();
    }

    public static void mute(final AlertType type) {
        FLAGS.get(type).set(false);
    }

    public static void unmute(final AlertType type) {
        FLAGS.get(type).set(true);
    }

    /** 전체 타입 활성화 상태 반환 — AlertController 상태 조회용 */
    public static Map<AlertType, Boolean> getStatus() {
        final Map<AlertType, Boolean> status = new EnumMap<>(AlertType.class);
        FLAGS.forEach((type, flag) -> status.put(type, flag.get()));
        return status;
    }
}
