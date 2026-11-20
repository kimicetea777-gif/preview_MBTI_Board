package com.board.kimicetea.global.membershipsync;

import java.util.List;

/**
 * [역할] AS {@code GET /api/internal/membership-events} 응답(connect_manual.md §8-4).
 * [주의사항] {@code hasNext}가 {@link Boolean}(래퍼)인 이유와 {@code null} 처리는
 * {@link com.board.kimicetea.global.deletionsync.DeletedUsersPollResponse}와 같다 — 옛 AS 응답에는 필드가 없다.
 *
 * @param items   이번 페이지의 이벤트(최대 요청한 limit건)
 * @param hasNext 다음 페이지가 있는가. 옛 AS 응답이면 {@code null}
 */
public record MembershipEventsPollResponse(List<MembershipEventResponse> items, Boolean hasNext) {}
