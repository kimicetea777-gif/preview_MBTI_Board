package com.board.kimicetea.global.image.sync;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.board.kimicetea.domain.post.entity.PostImage;
import com.board.kimicetea.domain.post.enums.PostImageStatus;
import com.board.kimicetea.domain.post.repository.PostImageRepository;
import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.image.client.ImageServerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 이미지 확정·정리 워커 — 오류 분류, 재시도 간격, 묶음 갱신, 메일 알림 marker.
 * 조건부 갱신(WHERE status)의 SQL 동작은 PostImageRepositoryTest(통합)에서 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class PostImageSyncServiceTest {

    @Mock private PostImageRepository postImageRepository;
    @Mock private ImageServerClient imageServerClient;

    private PostImageSyncService service;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new PostImageSyncService(postImageRepository, imageServerClient);
        logger = (Logger) LoggerFactory.getLogger(PostImageSyncService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("성공하면 묶음 한 번으로 CONFIRMED 처리")
        void success() {
            final PostImage a = row(PostImageStatus.PENDING_CONFIRM, 0);
            final PostImage b = row(PostImageStatus.PENDING_CONFIRM, 0);
            due(a, b);
            when(imageServerClient.confirm(ids(a, b))).thenReturn(List.of());

            service.syncDue();

            verify(postImageRepository).markConfirmed(uuids(a, b));
            verify(postImageRepository, never()).markConfirmFailed(anyCollection());
            assertNoAlert();
        }

        @Test
        @DisplayName("응답에서 빠진 id(만료된 이미지)만 CONFIRM_FAILED, 메일 없음")
        void missingIdsArePermanentFailures() {
            final PostImage a = row(PostImageStatus.PENDING_CONFIRM, 0);
            final PostImage expired = row(PostImageStatus.PENDING_CONFIRM, 0);
            due(a, expired);
            final List<String> missing = ids(expired); // thenReturn 안에서 mock을 호출하면 스텁이 꼬이므로 미리 계산
            when(imageServerClient.confirm(ids(a, expired))).thenReturn(missing);

            service.syncDue();

            verify(postImageRepository).markConfirmed(uuids(a));
            verify(postImageRepository).markConfirmFailed(uuids(expired));
            verify(postImageRepository, never()).scheduleRetry(anyCollection(), anyString(), any());
            assertNoAlert();
        }

        @Test
        @DisplayName("재시도 대상 오류면 실패 횟수가 같은 행끼리 묶어 간격표대로 재시도 예약")
        void retryableErrorSchedulesRetryGroupedByFailCount() {
            final PostImage first = row(PostImageStatus.PENDING_CONFIRM, 0);
            final PostImage fourth = row(PostImageStatus.PENDING_CONFIRM, 3);
            due(first, fourth);
            when(imageServerClient.confirm(anyList())).thenThrow(new ResourceAccessException("timeout"));
            final Instant before = Instant.now();

            service.syncDue();

            assertRetryScheduled(uuids(first), "PENDING_CONFIRM", before, Duration.ofSeconds(10));
            assertRetryScheduled(uuids(fourth), "PENDING_CONFIRM", before, Duration.ofMinutes(5));
            verify(postImageRepository, never()).markConfirmFailed(anyCollection());
            assertNoAlert();
        }

        @Test
        @DisplayName("400 같은 즉시 실패 4xx는 재시도 없이 CONFIRM_FAILED + 메일")
        void nonRetryable4xxFailsImmediately() {
            final PostImage a = row(PostImageStatus.PENDING_CONFIRM, 0);
            due(a);
            when(imageServerClient.confirm(anyList())).thenThrow(clientError(HttpStatus.BAD_REQUEST));

            service.syncDue();

            verify(postImageRepository).markConfirmFailed(uuids(a));
            verify(postImageRepository, never()).scheduleRetry(anyCollection(), anyString(), any());
            assertAlert();
        }

        @Test
        @DisplayName("10번째 실패면 재시도하지 않고 CONFIRM_FAILED + 메일")
        void tenthFailureConcludes() {
            final PostImage a = row(PostImageStatus.PENDING_CONFIRM, 9);
            due(a);
            when(imageServerClient.confirm(anyList())).thenThrow(new ResourceAccessException("down"));

            service.syncDue();

            verify(postImageRepository).markConfirmFailed(uuids(a));
            verify(postImageRepository, never()).scheduleRetry(anyCollection(), anyString(), any());
            assertAlert();
        }

        @Test
        @DisplayName("21개 이상이면 Image Server 상한(20)에 맞춰 나눠 요청")
        void chunksBy20() {
            final PostImage[] rows = IntStream.range(0, 25)
                    .mapToObj(i -> row(PostImageStatus.PENDING_CONFIRM, 0))
                    .toArray(PostImage[]::new);
            due(rows);
            when(imageServerClient.confirm(anyList())).thenReturn(List.of());

            service.syncDue();

            @SuppressWarnings("unchecked")
            final ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
            verify(imageServerClient, times(2)).confirm(captor.capture());
            assertThat(captor.getAllValues()).extracting(List::size).containsExactly(20, 5);
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("성공하면 묶음 한 번으로 행 삭제")
        void success() {
            final PostImage a = row(PostImageStatus.PENDING_RELEASE, 0);
            final PostImage b = row(PostImageStatus.PENDING_RELEASE, 0);
            due(a, b);

            service.syncDue();

            verify(imageServerClient).release(ids(a, b));
            verify(postImageRepository).deleteIfPendingRelease(uuids(a, b));
        }

        @Test
        @DisplayName("400이어도 포기하지 않고 재시도 — 늦어져도 게시글에는 영향이 없다")
        void anyErrorIsRetried() {
            final PostImage a = row(PostImageStatus.PENDING_RELEASE, 0);
            due(a);
            doThrow(clientError(HttpStatus.BAD_REQUEST)).when(imageServerClient).release(anyList());
            final Instant before = Instant.now();

            service.syncDue();

            assertRetryScheduled(uuids(a), "PENDING_RELEASE", before, Duration.ofSeconds(10));
            verify(postImageRepository, never()).deleteIfPendingRelease(anyCollection());
        }

        @Test
        @DisplayName("간격은 1시간 상한, 10번째 실패에 메일을 보내고도 계속 재시도")
        void cappedIntervalAndAlertAtTenth() {
            final PostImage tenth = row(PostImageStatus.PENDING_RELEASE, 9);
            due(tenth);
            doThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)).when(imageServerClient).release(anyList());
            final Instant before = Instant.now();

            service.syncDue();

            assertRetryScheduled(uuids(tenth), "PENDING_RELEASE", before, Duration.ofHours(1));
            assertAlert();
        }

        @Test
        @DisplayName("10번째 이전 실패는 메일 없이 재시도만")
        void noAlertBeforeTenth() {
            final PostImage fifth = row(PostImageStatus.PENDING_RELEASE, 4);
            due(fifth);
            doThrow(new ResourceAccessException("down")).when(imageServerClient).release(anyList());

            service.syncDue();

            assertNoAlert();
        }
    }

    @Test
    @DisplayName("처리할 행이 없으면 Image Server를 부르지 않는다")
    void nothingDue() {
        when(postImageRepository.findDue(any(), anyInt())).thenReturn(List.of());

        service.syncDue();

        verifyNoInteractions(imageServerClient);
    }

    @Test
    @DisplayName("confirm 대상과 release 대상이 섞여 있으면 각각 처리한다")
    void mixedStatuses() {
        final PostImage confirm = row(PostImageStatus.PENDING_CONFIRM, 0);
        final PostImage release = row(PostImageStatus.PENDING_RELEASE, 0);
        due(confirm, release);
        when(imageServerClient.confirm(ids(confirm))).thenReturn(List.of());

        service.syncDue();

        verify(postImageRepository).markConfirmed(uuids(confirm));
        verify(postImageRepository).deleteIfPendingRelease(uuids(release));
    }

    @Test
    @DisplayName("재시도 간격표: confirm은 표 그대로, release는 1시간 상한")
    void retryDelayTable() {
        assertThat(PostImageSyncService.retryDelay(1, PostImageStatus.PENDING_CONFIRM)).isEqualTo(Duration.ofSeconds(10));
        assertThat(PostImageSyncService.retryDelay(9, PostImageStatus.PENDING_CONFIRM)).isEqualTo(Duration.ofHours(4));
        assertThat(PostImageSyncService.retryDelay(7, PostImageStatus.PENDING_RELEASE)).isEqualTo(Duration.ofHours(1));
        assertThat(PostImageSyncService.retryDelay(9, PostImageStatus.PENDING_RELEASE)).isEqualTo(Duration.ofHours(1));
        assertThat(PostImageSyncService.retryDelay(50, PostImageStatus.PENDING_RELEASE)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("오류 분류: 연결 실패·5xx·429·401은 재시도, 400·404는 즉시 실패")
    void errorClassification() {
        assertThat(PostImageSyncService.isRetryable(new ResourceAccessException("timeout"))).isTrue();
        assertThat(PostImageSyncService.isRetryable(new RestClientException("unreadable body"))).isTrue();
        assertThat(PostImageSyncService.isRetryable(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))).isTrue();
        assertThat(PostImageSyncService.isRetryable(clientError(HttpStatus.TOO_MANY_REQUESTS))).isTrue();
        assertThat(PostImageSyncService.isRetryable(clientError(HttpStatus.UNAUTHORIZED))).isTrue();
        assertThat(PostImageSyncService.isRetryable(clientError(HttpStatus.BAD_REQUEST))).isFalse();
        assertThat(PostImageSyncService.isRetryable(clientError(HttpStatus.NOT_FOUND))).isFalse();
    }

    @Test
    @DisplayName("워커에는 트랜잭션이 없다 — Image Server 응답을 기다리는 동안 DB 커넥션을 잡지 않는다")
    void workerHasNoTransaction() throws NoSuchMethodException {
        assertThat(PostImageSyncService.class.isAnnotationPresent(Transactional.class)).isFalse();
        final Method syncDue = PostImageSyncService.class.getMethod("syncDue");
        assertThat(syncDue.isAnnotationPresent(Transactional.class)).isFalse();
    }

    // ------------------------------------------------------------------------------------------

    private static PostImage row(final PostImageStatus status, final int failCount) {
        final PostImage image = mock(PostImage.class);
        lenient().when(image.getImageId()).thenReturn(UUID.randomUUID());
        lenient().when(image.getPostId()).thenReturn(1L);
        lenient().when(image.getStatus()).thenReturn(status);
        lenient().when(image.getFailCount()).thenReturn(failCount);
        return image;
    }

    private void due(final PostImage... rows) {
        when(postImageRepository.findDue(any(Instant.class), eq(PostImageSyncService.BATCH_LIMIT)))
                .thenReturn(new ArrayList<>(Arrays.asList(rows)));
    }

    private static List<String> ids(final PostImage... rows) {
        return Arrays.stream(rows).map(row -> row.getImageId().toString()).toList();
    }

    private static List<UUID> uuids(final PostImage... rows) {
        return Arrays.stream(rows).map(PostImage::getImageId).toList();
    }

    private void assertRetryScheduled(final Collection<UUID> ids, final String expectedStatus,
            final Instant before, final Duration delay) {
        final ArgumentCaptor<Instant> next = ArgumentCaptor.forClass(Instant.class);
        verify(postImageRepository).scheduleRetry(eq(ids), eq(expectedStatus), next.capture());
        assertThat(next.getValue()).isBetween(before.plus(delay), Instant.now().plus(delay));
    }

    private static HttpClientErrorException clientError(final HttpStatus status) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(),
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
    }

    private void assertAlert() {
        assertThat(logs.list).anySatisfy(event ->
                assertThat(event.getMarkerList()).contains(AlertMarkers.IMAGE_SYNC_FAIL));
    }

    private void assertNoAlert() {
        assertThat(logs.list).noneSatisfy(event ->
                assertThat(event.getMarkerList()).isNotNull().contains(AlertMarkers.IMAGE_SYNC_FAIL));
    }
}
