package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * [역할] AS 멤버십 이벤트 한 건을 로컬 유저 상태에 반영한다 — 폴러(MembershipEventSyncService)와
 *        재시도 작업(MembershipEventRetryService)이 같은 규칙을 쓰도록 한곳에 둔다.
 * [주의사항] UserService를 호출하는 별도 빈이어야 한다 — 같은 클래스 안에서 부르면 프록시를 거치지 않아
 *           핸들러의 @Transactional이 적용되지 않는다. 핸들러 예외는 삼키지 않고 그대로 던진다
 *           (호출 측이 실패로 판단해 줄 세우거나 재시도를 예약해야 하므로). 핸들러는 상태 가드로 멱등이고,
 *           로컬 유저가 없으면(첫 로그인 전) 조용히 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventApplier {

    private final UserService userService;

    /** 실패하면 RuntimeException. type이 null이거나 모르는 값이면 warn 후 정상 종료 */
    public void apply(final String type, final Long eventId, final UUID authServerId, final Instant changedAt) {
        if (type == null) {
            log.warn("[membership-sync] 타입이 없는 멤버십 이벤트 무시 (logId={})", eventId);
            return;
        }
        switch (type) {
            case MembershipEventResponse.TYPE_CLIENT_WITHDRAWN ->
                    userService.handleMembershipWithdrawn(authServerId, changedAt);
            case MembershipEventResponse.TYPE_CLIENT_BANNED ->
                    userService.handleMembershipBanned(authServerId);
            case MembershipEventResponse.TYPE_CLIENT_ACTIVATED ->
                    userService.handleMembershipActivated(authServerId);
            default ->
                    log.warn("[membership-sync] 알 수 없는 멤버십 이벤트 타입 무시 (logId={}, type={})", eventId, type);
        }
    }

    /** 이 클래스가 반영할 줄 아는 타입인가 — 폴러가 줄 세우기 전에 이상한 이벤트를 걸러 낼 때 쓴다 */
    static boolean isKnownType(final String type) {
        return MembershipEventResponse.TYPE_CLIENT_WITHDRAWN.equals(type)
                || MembershipEventResponse.TYPE_CLIENT_BANNED.equals(type)
                || MembershipEventResponse.TYPE_CLIENT_ACTIVATED.equals(type);
    }
}
