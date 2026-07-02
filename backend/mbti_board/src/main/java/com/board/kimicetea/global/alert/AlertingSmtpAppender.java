package com.board.kimicetea.global.alert;

import ch.qos.logback.classic.net.SMTPAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.helpers.CyclicBuffer;
import org.slf4j.Marker;

import java.util.List;

/**
 * SMTPAppender 확장 — AlertType별 flag 확인 후 이메일 발송, 발송 후 auto-mute
 * 주의: Marker 없는 ERROR는 UNHANDLED_ERROR로 처리
 */
public class AlertingSmtpAppender extends SMTPAppender {

    @Override
    protected void sendBuffer(final CyclicBuffer<ILoggingEvent> cb, final ILoggingEvent lastEventEvaluated) {
        final AlertType type = resolveAlertType(lastEventEvaluated);
        if (!AlertFlagRegistry.isEnabled(type)) {
            return;
        }
        super.sendBuffer(cb, lastEventEvaluated);
        AlertFlagRegistry.mute(type);
    }

    private AlertType resolveAlertType(final ILoggingEvent event) {
        final List<Marker> markers = event.getMarkerList();
        if (markers != null) {
            for (final Marker marker : markers) {
                try {
                    return AlertType.valueOf(marker.getName());
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return AlertType.UNHANDLED_ERROR;
    }
}
