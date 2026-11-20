package com.board.kimicetea.global.image.sync;

import com.board.kimicetea.domain.post.entity.PostImage;
import com.board.kimicetea.domain.post.enums.PostImageStatus;
import com.board.kimicetea.domain.post.repository.PostImageRepository;
import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.image.client.ImageServerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * post_images의 PENDING_CONFIRM / PENDING_RELEASE 행을 Image Server와 동기화하는 워커(outbox 처리).
 * 글 저장 트랜잭션은 상태만 남기고, 실제 confirm/release 호출은 여기서 트랜잭션 밖에서 한다.
 * <p>
 * 주의: 이 클래스의 메서드에는 트랜잭션을 걸지 않는다 — Image Server 응답을 기다리는 동안 DB 커넥션을
 *       잡지 않기 위해서다. 상태 갱신은 리포지토리의 조건부 UPDATE(문장 단위 트랜잭션)로 한다.
 * 안전성은 롤백이 아니라 (1) 조건부 갱신 (2) confirm·release의 멱등성에서 나온다 — 갱신 전에 죽거나 UPDATE가
 * 실패하면 행이 그대로 남아 다음 주기에 같은 호출을 다시 보낸다.
 * 설계: Document/image_async_confirm_plan.md §4.5~4.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageSyncService {

    /** 한 번 실행에서 처리할 최대 행 수 */
    static final int BATCH_LIMIT = 100;
    /** Image Server가 한 요청에 받는 id 개수 상한 */
    static final int CHUNK_SIZE = 20;
    /** confirm은 이 횟수만큼 실패하면 CONFIRM_FAILED로 결론 — 아래 간격의 누적(약 7시간 52분)이 GC 한도 24시간 안 */
    static final int MAX_CONFIRM_ATTEMPTS = 10;
    /** release는 포기하지 않고, 이 횟수째 실패에 메일만 보낸다 */
    static final int RELEASE_ALERT_ATTEMPT = 10;
    /** release 재시도 간격 상한 */
    static final Duration RELEASE_MAX_INTERVAL = Duration.ofHours(1);
    /** n번째 실패 후 다음 시도까지의 간격(인덱스 n-1). 표를 넘으면 마지막 값 */
    static final List<Duration> BACKOFF = List.of(
            Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofMinutes(1),
            Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30),
            Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(4));

    private final PostImageRepository postImageRepository;
    private final ImageServerClient imageServerClient;

    /** 처리할 차례인 행을 confirm·release한다 — PostImageSyncScheduler가 주기적으로 호출 */
    public void syncDue() {
        final List<PostImage> due = postImageRepository.findDue(Instant.now(), BATCH_LIMIT);
        if (due.isEmpty()) {
            return;
        }
        final Map<PostImageStatus, List<PostImage>> byStatus = due.stream()
                .collect(Collectors.groupingBy(PostImage::getStatus));
        chunks(byStatus.getOrDefault(PostImageStatus.PENDING_CONFIRM, List.of())).forEach(this::confirmChunk);
        chunks(byStatus.getOrDefault(PostImageStatus.PENDING_RELEASE, List.of())).forEach(this::releaseChunk);
    }

    private void confirmChunk(final List<PostImage> rows) {
        final List<String> ids = idStrings(rows);
        final List<String> missing;
        try {
            missing = imageServerClient.confirm(ids);
        } catch (RestClientException e) {
            if (isRetryable(e)) {
                scheduleRetry(rows, PostImageStatus.PENDING_CONFIRM, e);
            } else {
                // 400 등 — 다시 보내도 같은 결과라 재시도하지 않는다(코드 버그 신호)
                postImageRepository.markConfirmFailed(uuids(ids));
                log.error(AlertMarkers.IMAGE_SYNC_FAIL,
                        "[image-sync] confirm 즉시 실패(재시도 불가) — 확정 실패 처리: {}", describe(rows), e);
            }
            return;
        }

        final Set<String> missingSet = new HashSet<>(missing);
        final List<String> confirmed = ids.stream().filter(id -> !missingSet.contains(id)).toList();
        if (!confirmed.isEmpty()) {
            postImageRepository.markConfirmed(uuids(confirmed));
        }
        if (!missing.isEmpty()) {
            postImageRepository.markConfirmFailed(uuids(missing));
            // 만료·삭제된 이미지는 콘텐츠 문제라 메일 없이 WARN만 — 배포 직후 백필로 기존 깨진 이미지가 몰려도 메일 폭주 없음
            log.warn("[image-sync] 이미 만료·삭제된 이미지 — 확정 실패 처리: {}",
                    describe(rows.stream().filter(row -> missingSet.contains(row.getImageId().toString())).toList()));
        }
    }

    private void releaseChunk(final List<PostImage> rows) {
        final List<String> ids = idStrings(rows);
        try {
            imageServerClient.release(ids);
        } catch (RestClientException e) {
            // release는 오류 종류와 무관하게 포기하지 않는다 — 늦어져도 게시글에는 영향이 없다
            scheduleRetry(rows, PostImageStatus.PENDING_RELEASE, e);
            return;
        }
        postImageRepository.deleteIfPendingRelease(uuids(ids));
    }

    /** 실패 횟수가 같은 행끼리 묶어(다음 간격이 같음) 재시도를 예약하거나, confirm 한도를 넘으면 결론 낸다 */
    private void scheduleRetry(final List<PostImage> rows, final PostImageStatus expected, final RestClientException cause) {
        final Map<Integer, List<PostImage>> byFailCount = rows.stream()
                .collect(Collectors.groupingBy(PostImage::getFailCount));
        byFailCount.forEach((failCount, group) -> {
            final int attempts = failCount + 1;
            final List<UUID> ids = group.stream().map(PostImage::getImageId).toList();

            if (expected == PostImageStatus.PENDING_CONFIRM && attempts >= MAX_CONFIRM_ATTEMPTS) {
                postImageRepository.markConfirmFailed(ids);
                log.error(AlertMarkers.IMAGE_SYNC_FAIL,
                        "[image-sync] confirm {}회 실패 — 확정 실패 처리: {}", attempts, describe(group), cause);
                return;
            }

            postImageRepository.scheduleRetry(ids, expected.name(), Instant.now().plus(retryDelay(attempts, expected)));
            if (expected == PostImageStatus.PENDING_RELEASE && attempts == RELEASE_ALERT_ATTEMPT) {
                log.error(AlertMarkers.IMAGE_SYNC_FAIL,
                        "[image-sync] release {}회 실패 — 계속 재시도: {}", attempts, describe(group), cause);
            } else {
                log.warn("[image-sync] {} {}회 실패, 재시도 예약: {} ({})",
                        expected, attempts, describe(group), cause.getMessage());
            }
        });
    }

    /** attempts번째 실패 후 다음 시도까지의 간격 — release는 상한 1시간 */
    static Duration retryDelay(final int attempts, final PostImageStatus status) {
        final Duration base = BACKOFF.get(Math.min(attempts, BACKOFF.size()) - 1);
        if (status == PostImageStatus.PENDING_RELEASE && base.compareTo(RELEASE_MAX_INTERVAL) > 0) {
            return RELEASE_MAX_INTERVAL;
        }
        return base;
    }

    /**
     * 재시도하면 회복될 수 있는 오류인가.
     * 응답 코드가 있는 오류: 5xx, 429(rate limit), 401(API 키 설정 — 사람이 고치면 회복)만 재시도.
     * 응답 코드가 없는 오류(연결 실패·타임아웃·본문 해석 실패)는 재시도.
     */
    static boolean isRetryable(final RestClientException e) {
        if (e instanceof RestClientResponseException response) {
            final int status = response.getStatusCode().value();
            return response.getStatusCode().is5xxServerError()
                    || status == HttpStatus.TOO_MANY_REQUESTS.value()
                    || status == HttpStatus.UNAUTHORIZED.value();
        }
        return true;
    }

    private static List<List<PostImage>> chunks(final List<PostImage> rows) {
        final List<List<PostImage>> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += CHUNK_SIZE) {
            result.add(rows.subList(i, Math.min(i + CHUNK_SIZE, rows.size())));
        }
        return result;
    }

    private static List<String> idStrings(final List<PostImage> rows) {
        return rows.stream().map(row -> row.getImageId().toString()).toList();
    }

    private static List<UUID> uuids(final List<String> ids) {
        return ids.stream().map(UUID::fromString).toList();
    }

    /** 로그용 — 어느 글의 어떤 이미지인지 */
    private static String describe(final List<PostImage> rows) {
        return rows.stream()
                .map(row -> "post " + row.getPostId() + "/" + row.getImageId())
                .collect(Collectors.joining(", "));
    }
}
