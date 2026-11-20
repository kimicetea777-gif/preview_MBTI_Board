package com.board.kimicetea.global.membershipsync;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.board.kimicetea.global.alert.AlertFlagRegistry;
import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.alert.AlertType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 주의: AlertFlagRegistry는 JVM 전체가 공유하는 static이라, 매 테스트 뒤에 두 타입을 다시 켜서
 *       다른 테스트(알림 메일 동작)에 음소거가 새지 않게 한다.
 */
@ExtendWith(MockitoExtension.class)
class MembershipSyncAlertServiceTest {

    @Mock
    private MembershipEventFailureRepository failureRepository;

    private MembershipSyncAlertService service;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new MembershipSyncAlertService(failureRepository);
        logger = (Logger) LoggerFactory.getLogger(MembershipSyncAlertService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        AlertFlagRegistry.unmute(AlertType.MEMBERSHIP_SYNC_FAIL);
        AlertFlagRegistry.unmute(AlertType.MEMBERSHIP_SYNC_STALE);
    }

    private long staleErrors() {
        return logs.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .filter(event -> event.getMarkerList() != null
                        && event.getMarkerList().contains(AlertMarkers.MEMBERSHIP_SYNC_STALE))
                .count();
    }

    @Nested
    @DisplayName("reenableIfDrained")
    class ReenableIfDrained {

        @Test
        @DisplayName("FAIL이 음소거 상태이고 큐가 비었으면 다시 켠다")
        void unmutesWhenDrained() {
            AlertFlagRegistry.mute(AlertType.MEMBERSHIP_SYNC_FAIL);
            when(failureRepository.count()).thenReturn(0L);

            service.reenableIfDrained();

            assertThat(AlertFlagRegistry.isEnabled(AlertType.MEMBERSHIP_SYNC_FAIL)).isTrue();
        }

        @Test
        @DisplayName("음소거 상태여도 행이 남아 있으면 그대로 둔다")
        void staysMutedWhileRowsRemain() {
            AlertFlagRegistry.mute(AlertType.MEMBERSHIP_SYNC_FAIL);
            when(failureRepository.count()).thenReturn(2L);

            service.reenableIfDrained();

            assertThat(AlertFlagRegistry.isEnabled(AlertType.MEMBERSHIP_SYNC_FAIL)).isFalse();
        }

        @Test
        @DisplayName("음소거 상태가 아니면 쿼리하지 않는다(평상시 10초마다 쿼리가 늘지 않음)")
        void skipsQueryWhenEnabled() {
            service.reenableIfDrained();

            verify(failureRepository, never()).count();
        }
    }

    @Nested
    @DisplayName("checkDaily")
    class CheckDaily {

        @Test
        @DisplayName("폴링이 실패한 날은 MEMBERSHIP_SYNC_STALE ERROR를 남긴다")
        void alertsWhenSyncFailed() {
            when(failureRepository.countByCreatedAtBefore(any())).thenReturn(0L);

            service.checkDaily(false);

            assertThat(staleErrors()).isEqualTo(1);
        }

        @Test
        @DisplayName("24시간보다 오래된 행이 있으면 MEMBERSHIP_SYNC_STALE ERROR를 남긴다")
        void alertsWhenStaleRowsExist() {
            when(failureRepository.countByCreatedAtBefore(any())).thenReturn(3L);

            service.checkDaily(true);

            assertThat(staleErrors()).isEqualTo(1);
        }

        @Test
        @DisplayName("둘 다 아니면 ERROR 없이, 음소거돼 있던 STALE을 다시 켠다")
        void unmutesWhenHealthy() {
            AlertFlagRegistry.mute(AlertType.MEMBERSHIP_SYNC_STALE);
            when(failureRepository.countByCreatedAtBefore(any())).thenReturn(0L);

            service.checkDaily(true);

            assertThat(staleErrors()).isZero();
            assertThat(AlertFlagRegistry.isEnabled(AlertType.MEMBERSHIP_SYNC_STALE)).isTrue();
        }

        @Test
        @DisplayName("점검 쿼리가 실패해도 예외를 던지지 않고 WARN만 남긴다")
        void swallowsQueryFailure() {
            when(failureRepository.countByCreatedAtBefore(any()))
                    .thenThrow(new DataAccessResourceFailureException("db down"));

            assertThatCode(() -> service.checkDaily(true)).doesNotThrowAnyException();

            assertThat(logs.list).anySatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
            assertThat(staleErrors()).isZero();
        }
    }
}
