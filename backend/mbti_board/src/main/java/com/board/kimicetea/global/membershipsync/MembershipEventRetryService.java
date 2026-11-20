package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.global.alert.AlertMarkers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * [역할] membership_event_failures의 줄 선 멤버십 이벤트를 유저별 순서(seq)대로 다시 적용한다.
 *        성공하면 행을 지우고 같은 유저의 다음 행을 이어서 처리하며, 실패하면 백오프로 다음 시도를 예약한다.
 *        자동으로 포기하지 않는다 — 밴·탈퇴 반영이 빠지면 상태가 어긋난 채로 남기 때문이다.
 * [주의사항] 트랜잭션을 걸지 않는다 — 각 단계는 핸들러의 @Transactional과 리포지토리의 문장 단위 트랜잭션으로 커밋된다.
 *           적용과 삭제가 별개 트랜잭션이라 그 사이에 죽어도 다음 실행이 같은 행을 다시 적용할 뿐이다(핸들러 멱등).
 *           폴러·확정 배치와 겹치지 않도록 MembershipEventRetryScheduler가 MembershipSyncLock 안에서 부른다.
 * 설계: Document/membership_event_retry_plan.md "재시도 작업"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipEventRetryService {

    /** 한 번 실행(틱)에서 시도할 최대 행 수 — 같은 유저를 이어서 처리한 행도 센다 */
    static final int BATCH_LIMIT = 50;
    /** 이 횟수째 실패에 알림 로그(첫 실패 후 약 7분) — 짧은 DB·Redis 끊김에는 메일이 가지 않게 */
    static final int ALERT_FAIL_COUNT = 5;
    /** n번째 실패 후 다음 시도까지의 간격(인덱스 n-1). 표를 넘으면 마지막 값(상한 1시간) */
    static final List<Duration> BACKOFF = List.of(
            Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofMinutes(1),
            Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30),
            Duration.ofHours(1));
    /** membership_event_failures.last_error 길이(V15) */
    static final int LAST_ERROR_MAX_LENGTH = 500;

    private final MembershipEventFailureRepository failureRepository;
    private final MembershipEventApplier applier;

    private enum Outcome { SUCCEEDED, FAILED, INFRA_FAILED }

    /** 시도할 차례가 된 행을 처리한다 — MembershipEventRetryScheduler가 주기적으로 호출 */
    public void retryDue() {
        final List<MembershipEventFailure> heads = failureRepository.findDueHeads(Instant.now(), BATCH_LIMIT);
        int attempted = 0;
        for (final MembershipEventFailure head : heads) {
            Optional<MembershipEventFailure> row = Optional.of(head);
            while (row.isPresent() && attempted < BATCH_LIMIT) {
                final MembershipEventFailure current = row.get();
                if (current.getNextAttemptAt().isAfter(Instant.now())) {
                    break;
                }
                attempted++;
                final Outcome outcome = attempt(current);
                if (outcome == Outcome.INFRA_FAILED) {
                    // DB·Redis 연결 문제면 남은 행도 같은 이유로 실패한다 — 각자 타임아웃을 기다리며 락을 오래 잡지 않게 끝낸다
                    return;
                }
                if (outcome == Outcome.FAILED) {
                    break; // 이 유저는 이번 실행에서 더 진행하지 않는다(순서 유지). 다른 유저는 계속
                }
                row = failureRepository.findFirstByAuthServerIdOrderBySeqAsc(current.getAuthServerId());
            }
            if (attempted >= BATCH_LIMIT) {
                return;
            }
        }
    }

    private Outcome attempt(final MembershipEventFailure row) {
        try {
            applier.apply(row.getEventType(), row.getEventId(), row.getAuthServerId(), row.getChangedAt());
        } catch (final RuntimeException e) {
            recordFailure(row, e);
            return isInfraFailure(e) ? Outcome.INFRA_FAILED : Outcome.FAILED;
        }
        failureRepository.deleteResolved(row.getEventId());
        log.info("[membership-retry] 멤버십 이벤트 반영 성공 (logId={}, userId={}, type={}, 이전 실패 {}회)",
                row.getEventId(), row.getAuthServerId(), row.getEventType(), row.getFailCount());
        return Outcome.SUCCEEDED;
    }

    private void recordFailure(final MembershipEventFailure row, final RuntimeException cause) {
        final int attempts = row.getFailCount() + 1;
        final Duration delay = retryDelay(attempts);
        failureRepository.scheduleRetry(row.getEventId(), Instant.now().plus(delay), describeError(cause));
        if (attempts == ALERT_FAIL_COUNT) {
            log.error(AlertMarkers.MEMBERSHIP_SYNC_FAIL,
                    "[membership-retry] 멤버십 이벤트 {}회 실패 — 계속 재시도 (logId={}, userId={}, type={})",
                    attempts, row.getEventId(), row.getAuthServerId(), row.getEventType(), cause);
        } else {
            log.warn("[membership-retry] 멤버십 이벤트 {}회 실패, {} 뒤 재시도 (logId={}, userId={}, type={}): {}",
                    attempts, delay, row.getEventId(), row.getAuthServerId(), row.getEventType(), cause.toString());
        }
    }

    /** failCount번째 실패 후 다음 시도까지의 간격 — 표를 넘으면 마지막 값(1시간) */
    static Duration retryDelay(final int failCount) {
        final int index = Math.min(Math.max(failCount, 1), BACKOFF.size()) - 1;
        return BACKOFF.get(index);
    }

    /**
     * DB·Redis 연결 문제인가 — 그렇다면 이번 틱의 남은 행도 같은 이유로 실패하므로 틱을 끝낸다.
     * RedisConnectionFailureException은 DataAccessResourceFailureException, Redis 명령 타임아웃(QueryTimeoutException)은
     * TransientDataAccessException의 하위 타입이다. 트랜잭션 시작 시 DB 커넥션을 못 얻으면 CannotCreateTransactionException.
     */
    static boolean isInfraFailure(final Throwable e) {
        Throwable current = e;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (current instanceof DataAccessResourceFailureException
                    || current instanceof TransientDataAccessException
                    || current instanceof CannotCreateTransactionException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /** last_error에 남길 문구 — 예외 종류와 메시지만, 컬럼 길이로 자른다 */
    static String describeError(final Throwable e) {
        final String message = e.getMessage();
        final String text = message == null ? e.getClass().getSimpleName() : e.getClass().getSimpleName() + ": " + message;
        return text.length() <= LAST_ERROR_MAX_LENGTH ? text : text.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
