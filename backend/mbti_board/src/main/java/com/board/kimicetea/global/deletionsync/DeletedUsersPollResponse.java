package com.board.kimicetea.global.deletionsync;

import java.util.List;

/**
 * {@code hasMore} 필드는 없다 — {@code items.size() < limit}이면 더 없다는 뜻(AS 쪽 응답과 동일 규칙,
 * connect_manual.md §8-3 참고).
 */
public record DeletedUsersPollResponse(List<DeletedUserEventResponse> items) {
}
