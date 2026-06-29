package com.board.kimicetea.global.alert;

import ch.qos.logback.classic.net.SMTPAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.helpers.CyclicBuffer;

/** SMTPAppender 확장 — EmailAlertFlag가 mute 상태이면 이메일 전송을 건너뜀 */
public class AlertingSmtpAppender extends SMTPAppender {

    @Override
    protected void sendBuffer(final CyclicBuffer<ILoggingEvent> cb, final ILoggingEvent lastEventEvaluated) {
        if (!EmailAlertFlag.isEnabled()) {
            return;
        }

        super.sendBuffer(cb, lastEventEvaluated);
    }
}
