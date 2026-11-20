package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.deletionsync.PollingCursor;
import com.board.kimicetea.global.deletionsync.PollingCursorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * AS의 연동 상태 변경(연결 해제/밴/재연결)을 커서 기반으로 폴링해 로컬 상태에 반영한다.
 * DeletedUserSyncService(계정 하드 삭제 폴링)와 같은 poller 인프라(PollingCursor)를 poller_name만
 * 다르게 해서 공유한다.
 * <p>
 * 반영에 실패한 이벤트는 membership_event_failures에 줄 세우고 커서는 넘긴다 — 한 건의 실패가 다른 유저의
 * 이벤트를 막지 않게 하기 위해서다. 줄 선 이벤트는 MembershipEventRetryService가 유저별 순서대로 다시 적용한다.
 * 같은 유저의 순서를 지키려고, 미해결 이벤트가 있는 유저의 이후 이벤트는 적용하지 않고 뒤에 줄 세운다.
 * 주의: 트랜잭션을 걸지 않는다 — 각 단계는 핸들러의 @Transactional과 리포지토리의 문장 단위 트랜잭션으로 커밋된다.
 * 설계: Document/membership_event_retry_plan.md "폴러 규칙"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipEventSyncService {

    static final String POLLER_NAME = "membership-events";
    private static final int PAGE_LIMIT = 500;

    private final MembershipEventPollClient pollClient;
    private final PollingCursorRepository cursorRepository;
    private final MembershipEventApplier applier;
    private final MembershipEventFailureRepository failureRepository;
    private final MembershipSyncLock lock;

    /** 한 페이지를 처리한 결과 */
    private enum PageResult { HAS_NEXT, DONE, STOPPED }

    /**
     * [역할] 다음 페이지가 없을 때까지 폴링해 반영한다.
     * [주의사항] 재시도 작업과 겹치지 않게 MembershipSyncLock 안에서 돈다.
     *
     * @return 마지막 페이지까지 모든 이벤트를 적용하거나 줄 세웠으면 true. 폴링 호출 실패, 줄 세우기 실패,
     *         커서 키(id·changedAt) 누락으로 중간에 멈췄으면 false — 이날은 확정 배치를 돌리면 안 된다
     *         (받지 못한 재연결 이벤트가 있을 수 있음, MembershipSyncScheduler 참고)
     */
    public boolean sync() {
        return lock.callLocked(() -> {
            PageResult result;
            do {
                result = syncOnePage();
            } while (result == PageResult.HAS_NEXT);
            return result == PageResult.DONE;
        });
    }

    /**
     * 항목마다 "적용 또는 줄 세우기"와 커서 전진을 별개 트랜잭션으로 즉시 커밋한다. 커서는 항상 그 뒤에 저장하므로
     * 중간에 죽어도 다음 실행이 같은 이벤트를 다시 받을 뿐이다(핸들러 멱등, 줄 세우기는 ON CONFLICT DO NOTHING).
     */
    private PageResult syncOnePage() {
        final PollingCursor cursor = loadCursor();

        final MembershipEventsPollResponse response;
        try {
            response = pollClient.poll(cursor.getSinceTimestamp(), cursor.getSinceId(), PAGE_LIMIT);
        } catch (final RestClientException | IllegalStateException e) {
            log.warn("[membership-sync] AS 멤버십 이벤트 폴링 호출 실패 — 이번 실행 스킵, 커서 유지", e);
            return PageResult.STOPPED;
        }

        // 락을 잡고 있으므로 페이지를 처리하는 동안 다른 스레드가 이 집합을 바꾸지 않는다
        final Set<UUID> blockedUsers = new HashSet<>(failureRepository.findAllAuthServerIds());

        for (final MembershipEventResponse item : response.items()) {
            if (item.id() == null || item.changedAt() == null) {
                // 커서를 이 값으로 옮길 수 없다 — AS가 커서 정렬 키로 쓰는 값이라 정상이면 오지 않는다
                log.error(AlertMarkers.MEMBERSHIP_SYNC_FAIL,
                        "[membership-sync] 커서 키가 없는 멤버십 이벤트 — 그 지점에서 중단 (logId={}, changedAt={})",
                        item.id(), item.changedAt());
                return PageResult.STOPPED;
            }
            if (!handle(item, blockedUsers)) {
                return PageResult.STOPPED;
            }
            cursor.advance(item.changedAt(), item.id());
            cursorRepository.save(cursor);
        }

        return hasNextPage(response) ? PageResult.HAS_NEXT : PageResult.DONE;
    }

    /**
     * 이벤트 한 건을 검증 → 적용 또는 줄 세우기 한다.
     *
     * @return 커서를 이 이벤트 뒤로 넘겨도 되면 true, 줄 세우기에 실패해 멈춰야 하면 false
     */
    private boolean handle(final MembershipEventResponse item, final Set<UUID> blockedUsers) {
        // 검증은 blockedUsers 확인보다 먼저 — 이상한 이벤트가 줄에 들어가 테이블 제약에 걸리면 폴러가 영구히 멈춘다
        if (item.userId() == null || !MembershipEventApplier.isKnownType(item.type())) {
            log.warn("[membership-sync] 처리할 수 없는 멤버십 이벤트 건너뜀 (logId={}, type={}, userId={})",
                    item.id(), item.type(), item.userId());
            return true;
        }

        if (blockedUsers.contains(item.userId())) {
            return enqueue(item, 0, Instant.now(), null);
        }

        try {
            applier.apply(item.type(), item.id(), item.userId(), item.changedAt());
            return true;
        } catch (final RuntimeException e) {
            // 알림은 재시도 5회째에 보낸다 — 여기서 ERROR를 남기면 짧은 장애에도 메일이 나가고 그 타입이 음소거된다
            log.warn("[membership-sync] 멤버십 이벤트 반영 실패 — 재시도 큐에 줄 세움 (logId={}, userId={}, type={}): {}",
                    item.id(), item.userId(), item.type(), e.toString());
            final boolean queued = enqueue(item, 1,
                    Instant.now().plus(MembershipEventRetryService.retryDelay(1)),
                    MembershipEventRetryService.describeError(e));
            if (queued) {
                blockedUsers.add(item.userId());
            }
            return queued;
        }
    }

    /** 줄 세우기 — 실패하면(DB 장애) 이벤트를 넘기면 유실되므로 false를 돌려 그 지점에서 멈추게 한다 */
    private boolean enqueue(final MembershipEventResponse item, final int failCount, final Instant nextAttemptAt,
            final String lastError) {
        try {
            failureRepository.enqueue(item.id(), item.userId(), item.type(), item.changedAt(),
                    failCount, nextAttemptAt, lastError);
            return true;
        } catch (final RuntimeException e) {
            log.error(AlertMarkers.MEMBERSHIP_SYNC_FAIL,
                    "[membership-sync] 재시도 큐 저장 실패 — 그 지점에서 중단, 커서 유지 (logId={}, userId={}, type={})",
                    item.id(), item.userId(), item.type(), e);
            return false;
        }
    }

    /**
     * [역할] 다음 페이지를 바로 이어서 요청할지 정한다 — DeletedUserSyncService와 같은 규칙.
     * [주의사항] AS의 {@code hasNext}를 따르고, 없으면({@code null}, 옛 AS) 개수 == PAGE_LIMIT으로 판단한다.
     * 항목이 비어 있으면 커서가 전진하지 않아 같은 요청이 반복되므로 {@code hasNext}가 true여도 멈춘다.
     */
    private boolean hasNextPage(final MembershipEventsPollResponse response) {
        if (response.items().isEmpty()) {
            return false;
        }
        if (response.hasNext() != null) {
            return response.hasNext();
        }
        return response.items().size() == PAGE_LIMIT;
    }

    private PollingCursor loadCursor() {
        return cursorRepository.findById(POLLER_NAME).orElseGet(() -> PollingCursor.initial(POLLER_NAME));
    }
}
