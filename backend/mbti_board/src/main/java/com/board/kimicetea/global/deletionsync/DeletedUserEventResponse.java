package com.board.kimicetea.global.deletionsync;

import java.time.Instant;
import java.util.UUID;

/**
 * AS(AUTH_serverProject)의 {@code GET /api/internal/deleted-users} 응답 항목을 그대로 미러링.
 * AS 쪽 {@code DeletedUserEventResponse}와 동일 shape — {@code type}은 지금 "ACCOUNT_DELETED" 하나뿐.
 */
public record DeletedUserEventResponse(String type, Long id, UUID userId, Instant deletedAt) {
}
