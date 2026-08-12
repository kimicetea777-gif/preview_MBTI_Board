package com.board.kimicetea.global.audit;

public enum AuditEvent {
    SIGNUP,
    OAUTH2_SIGNUP,
    LOGIN,
    OAUTH2_LOGIN,
    PASSWORD_CHANGE,
    RECOVERY_EMAIL_CHANGE,
    WITHDRAWAL,
    REUSE_DETECTED,
    MEMBERSHIP_BANNED,
    MEMBERSHIP_REACTIVATED
}
