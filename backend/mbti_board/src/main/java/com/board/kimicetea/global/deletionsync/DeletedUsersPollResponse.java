package com.board.kimicetea.global.deletionsync;

import java.util.List;

/**
 * [역할] AS {@code GET /api/internal/deleted-users} 응답(connect_manual.md §8-3).
 * [주의사항] {@code hasNext}는 래퍼 타입({@link Boolean})이다 — AS가 이 필드를 추가하기 전 버전이면 JSON에 없어서
 * {@code null}로 들어온다. 원시 {@code boolean}이면 그 경우 {@code false}가 돼 첫 페이지에서 멈추므로 바꾸지 말 것.
 * {@code null}일 때의 판단은 {@link DeletedUserSyncService}가 예전 규칙(개수 == limit)으로 대신한다.
 *
 * @param items   이번 페이지의 기록(최대 요청한 limit건)
 * @param hasNext 다음 페이지가 있는가. AS가 limit+1건을 읽어 정확히 판단한 값. 옛 AS 응답이면 {@code null}
 */
public record DeletedUsersPollResponse(List<DeletedUserEventResponse> items, Boolean hasNext) {
}
