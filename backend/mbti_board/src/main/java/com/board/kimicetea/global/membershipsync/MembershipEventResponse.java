package com.board.kimicetea.global.membershipsync;

import java.time.Instant;
import java.util.UUID;

/**
 * AS {@code GET /api/internal/membership-events} 응답 항목 — {@code deleted-users} 폴링의
 * {@code DeletedUserEventResponse}와 같은 패턴. changedAt은 AS 쪽 이벤트 발생 시각 그대로이며,
 * MembershipWithdrawalFinalizeService의 유예기간 계산 기준으로 그대로 사용된다(로컬 처리 시각 아님).
 */
public record MembershipEventResponse(String type, Long id, UUID userId, String clientId, Instant changedAt) {

    public static final String TYPE_CLIENT_ACTIVATED = "CLIENT_ACTIVATED";
    public static final String TYPE_CLIENT_WITHDRAWN = "CLIENT_WITHDRAWN";
    public static final String TYPE_CLIENT_BANNED = "CLIENT_BANNED";
}
