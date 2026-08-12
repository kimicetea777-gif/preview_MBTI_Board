package com.board.kimicetea.global.deletionsync;

import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * AS(AUTH_serverProject)가 하드 삭제한 계정을 커서 기반으로 폴링해 로컬 프로필을 정리한다
 * (memo.md 1~5번, connect_manual.md §8-3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeletedUserSyncService {

    static final String POLLER_NAME = "deleted-users";
    private static final int PAGE_LIMIT = 500;

    private final DeletedUserPollClient pollClient;
    private final PollingCursorRepository cursorRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public void sync() {
        boolean mayHaveMore = true;
        while (mayHaveMore) {
            mayHaveMore = syncOnePage();
        }
    }

    /**
     * 항목마다 로컬 처리와 커서 전진을 별개 트랜잭션으로 즉시 커밋한다(각 호출이 서로 다른 빈의
     * 프록시를 거치므로 자기 자신의 @Transactional 경계를 각각 가짐) — 배치 끝까지 기다렸다가 한
     * 번에 커서를 갱신하지 않는다. 처리 중 실패하면 그 지점에서 멈추고, 그 이전까지 이미 커밋된
     * 항목들의 커서 전진은 그대로 유지한다(memo.md 5번: 로컬 처리 성공 후에만 커서 전진).
     *
     * @return 더 있을 수 있으면(응답 개수가 PAGE_LIMIT과 같으면) true — 즉시 다음 페이지를 마저 처리
     */
    private boolean syncOnePage() {
        final PollingCursor cursor = loadCursor();

        final DeletedUsersPollResponse response;
        try {
            response = pollClient.poll(cursor.getSinceTimestamp(), cursor.getSinceId(), PAGE_LIMIT);
        } catch (final RestClientException | IllegalStateException e) {
            log.warn("AS 계정 하드 삭제 폴링 호출 실패 — 이번 실행 스킵, 커서 유지", e);
            return false;
        }

        for (final DeletedUserEventResponse item : response.items()) {
            try {
                processItem(item);
            } catch (final RuntimeException e) {
                log.error("계정 하드 삭제 로컬 처리 실패 (logId={}, userId={}) — 그 지점에서 중단, 다음 실행에 재시도",
                        item.id(), item.userId(), e);
                return false;
            }
            cursor.advance(item.deletedAt(), item.id());
            cursorRepository.save(cursor);
        }

        return response.items().size() == PAGE_LIMIT;
    }

    /** 로컬에 없으면(원래 이 서비스 유저가 아니었던 경우) skip — forceWithdrawFromAsDeletion 자체도 이미 WITHDRAWN이면 skip(idempotent) */
    private void processItem(final DeletedUserEventResponse item) {
        userRepository.findByAuthServerId(item.userId())
                .ifPresent(user -> userService.forceWithdrawFromAsDeletion(user.getId()));
    }

    private PollingCursor loadCursor() {
        return cursorRepository.findById(POLLER_NAME).orElseGet(() -> PollingCursor.initial(POLLER_NAME));
    }
}
