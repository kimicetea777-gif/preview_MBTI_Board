package com.board.kimicetea.global.alert;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/** SLF4J Marker 상수 — log.error(AlertMarkers.JWT_TAMPER, ...) 형태로 사용 */
public class AlertMarkers {

    public static final Marker JWT_TAMPER           = MarkerFactory.getMarker(AlertType.JWT_TAMPER.name());
    public static final Marker UNHANDLED_ERROR      = MarkerFactory.getMarker(AlertType.UNHANDLED_ERROR.name());
    public static final Marker DB_QUERY_SLOW        = MarkerFactory.getMarker(AlertType.DB_QUERY_SLOW.name());
    public static final Marker BRUTE_FORCE_LOGIN    = MarkerFactory.getMarker(AlertType.BRUTE_FORCE_LOGIN.name());
    public static final Marker PASSWORD_RESET_ABUSE = MarkerFactory.getMarker(AlertType.PASSWORD_RESET_ABUSE.name());
    public static final Marker ACCOUNT_LOCKED       = MarkerFactory.getMarker(AlertType.ACCOUNT_LOCKED.name());
    public static final Marker RATE_LIMIT_EXCEEDED  = MarkerFactory.getMarker(AlertType.RATE_LIMIT_EXCEEDED.name());
    public static final Marker REDIS_CONNECTION_FAIL = MarkerFactory.getMarker(AlertType.REDIS_CONNECTION_FAIL.name());

    private AlertMarkers() {}
}
