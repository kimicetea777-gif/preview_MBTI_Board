package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.domain.user.service.UserService;
import com.board.kimicetea.global.deletionsync.PollingCursor;
import com.board.kimicetea.global.deletionsync.PollingCursorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * AS의 연동 상태 변경(연결 해제/밴/재연결)을 커서 기반으로 폴링해 로컬 상태에 반영한다.
 * DeletedUserSyncService(계정 하드 삭제 폴링)와 같은 poller 인프라(PollingCursor)를 poller_name만
 * 다르게 해서 공유한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipEventSyncService {

    static final String POLLER_NAME = "membership-events";
    private static final int PAGE_LIMIT = 500;

    private final MembershipEventPollClient pollClient;
    private final PollingCursorRepository cursorRepository;
    private final UserService userService;

    public void sync() {
        boolean mayHaveMore = true;
        while (mayHaveMore) {
            mayHaveMore = syncOnePage();
        }
    }

    /**
     * 항목마다 로컬 처리와 커서 전진을 별개 트랜잭션으로 즉시 커밋 — DeletedUserSyncService와 동일 패턴.
     * 처리 중 실패하면 그 지점에서 멈추고, 그 이전까지 이미 커밋된 항목들의 커서 전진은 유지한다.
     *
     * @return 더 있을 수 있으면(응답 개수가 PAGE_LIMIT과 같으면) true — 즉시 다음 페이지를 마저 처리
     */
    private boolean syncOnePage() {
        final PollingCursor cursor = loadCursor();

        final MembershipEventsPollResponse response;
        try {
            response = pollClient.poll(cursor.getSinceTimestamp(), cursor.getSinceId(), PAGE_LIMIT);
        } catch (final RestClientException | IllegalStateException e) {
            log.warn("AS 멤버십 이벤트 폴링 호출 실패 — 이번 실행 스킵, 커서 유지", e);
            return false;
        }

        for (final MembershipEventResponse item : response.items()) {
            try {
                processItem(item);
            } catch (final RuntimeException e) {
                log.error("멤버십 이벤트 로컬 처리 실패 (logId={}, userId={}, type={}) — 그 지점에서 중단, 다음 실행에 재시도",
                        item.id(), item.userId(), item.type(), e);
                return false;
            }
            cursor.advance(item.changedAt(), item.id());
            cursorRepository.save(cursor);
        }

        return response.items().size() == PAGE_LIMIT;
    }

    /** 로컬에 없으면(아직 JIT 계정 생성 전 등) skip — 각 핸들러 자체도 idempotent */
    private void processItem(final MembershipEventResponse item) {
        switch (item.type()) {
            case MembershipEventResponse.TYPE_CLIENT_WITHDRAWN ->
                    userService.handleMembershipWithdrawn(item.userId(), item.changedAt());
            case MembershipEventResponse.TYPE_CLIENT_BANNED ->
                    userService.handleMembershipBanned(item.userId());
            case MembershipEventResponse.TYPE_CLIENT_ACTIVATED ->
                    userService.handleMembershipActivated(item.userId());
            default ->
                    log.warn("알 수 없는 멤버십 이벤트 타입 무시 (logId={}, type={})", item.id(), item.type());
        }
    }

    private PollingCursor loadCursor() {
        return cursorRepository.findById(POLLER_NAME).orElseGet(() -> PollingCursor.initial(POLLER_NAME));
    }
}
