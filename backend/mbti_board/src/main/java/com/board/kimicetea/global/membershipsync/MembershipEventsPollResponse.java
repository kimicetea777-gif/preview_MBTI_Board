package com.board.kimicetea.global.membershipsync;

import java.util.List;

/** hasMore 없이 items.size() < limit으로 마지막 페이지 판단 — deleted-users 폴링과 동일 계약 */
public record MembershipEventsPollResponse(List<MembershipEventResponse> items) {}
