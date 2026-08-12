package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.entity.UserRetentionLog;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.repository.UserRetentionLogRepository;
import com.board.kimicetea.global.audit.AuditEvent;
import com.board.kimicetea.global.audit.AuditLogService;
import com.board.kimicetea.global.crypto.RetentionEncryptionService;
import com.board.kimicetea.global.security.session.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRetentionLogRepository userRetentionLogRepository;

    @Mock
    private MbtiMetadataRepository mbtiMetadataRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MbtiTestHistoryRepository mbtiTestHistoryRepository;

    @Mock
    private RetentionEncryptionService retentionEncryptionService;

    @Mock
    private SessionService sessionService;

    @Mock
    private AuditLogService auditLogService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, userRetentionLogRepository, mbtiMetadataRepository, postRepository,
                commentRepository, mbtiTestHistoryRepository, retentionEncryptionService,
                sessionService, auditLogService);
    }

    @Nested
    @DisplayName("forceWithdrawFromAsDeletion — AS 계정 하드 삭제 폴링에서 호출")
    class ForceWithdrawFromAsDeletion {

        @Test
        @DisplayName("NORMAL 상태 유저는 PII 익명화 + WITHDRAWN 전환 + 전체 세션 삭제 + 감사 로그까지 수행한다")
        void anonymizesAndMarksWithdrawn() {
            final UUID userId = UUID.randomUUID();
            final UUID authServerId = UUID.randomUUID();
            final User user = User.builder()
                    .id(userId)
                    .authServerId(authServerId)
                    .email("someone@example.com")
                    .nickname("nick1234")
                    .role(UserRole.USER)
                    .state(UserState.NORMAL)
                    .build();
            when(userRepository.findWithMbtiById(userId)).thenReturn(Optional.of(user));
            when(retentionEncryptionService.encrypt(any())).thenReturn("encrypted");

            userService.forceWithdrawFromAsDeletion(userId);

            assertThat(user.getState()).isEqualTo(UserState.WITHDRAWN);
            assertThat(user.getEmail()).startsWith("withdrawn_");
            verify(userRetentionLogRepository).save(any(UserRetentionLog.class));
            verify(sessionService).deleteAllSessions(authServerId);
            verify(auditLogService).record(eq(AuditEvent.WITHDRAWAL), eq(userId), eq("USER"), eq("AS_DELETION_SYNC"));
        }

        @Test
        @DisplayName("이미 WITHDRAWN이면 아무것도 하지 않는다 — 폴링 재시도/중복 이벤트에 대해 idempotent")
        void noOpWhenAlreadyWithdrawn() {
            final UUID userId = UUID.randomUUID();
            final User user = User.builder()
                    .id(userId)
                    .authServerId(UUID.randomUUID())
                    .email("withdrawn_x@deleted.invalid")
                    .nickname("withdrawn_")
                    .role(UserRole.USER)
                    .state(UserState.WITHDRAWN)
                    .build();
            when(userRepository.findWithMbtiById(userId)).thenReturn(Optional.of(user));

            userService.forceWithdrawFromAsDeletion(userId);

            verify(userRetentionLogRepository, never()).save(any());
            verifyNoInteractions(sessionService, auditLogService);
        }
    }

    @Nested
    @DisplayName("handleMembershipWithdrawn — AS CLIENT_WITHDRAWN 이벤트 수신")
    class HandleMembershipWithdrawn {

        @Test
        @DisplayName("NORMAL 유저는 MEMBERSHIP_WITHDRAWAL_PENDING으로 전환하고 이벤트 시각을 기록, PII는 보존한다")
        void marksPendingWithoutAnonymizing() {
            final UUID userId = UUID.randomUUID();
            final UUID authServerId = UUID.randomUUID();
            final Instant changedAt = Instant.parse("2026-01-01T00:00:00Z");
            final User user = User.builder()
                    .id(userId).authServerId(authServerId).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.NORMAL).build();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(user));

            userService.handleMembershipWithdrawn(authServerId, changedAt);

            assertThat(user.getState()).isEqualTo(UserState.MEMBERSHIP_WITHDRAWAL_PENDING);
            assertThat(user.getMembershipWithdrawnAt()).isEqualTo(changedAt);
            assertThat(user.getEmail()).isEqualTo("someone@example.com");
            verify(sessionService).deleteAllSessions(authServerId);
        }

        @Test
        @DisplayName("이미 MEMBERSHIP_WITHDRAWAL_PENDING이면 아무것도 하지 않는다 — idempotent")
        void noOpWhenAlreadyPending() {
            final UUID authServerId = UUID.randomUUID();
            final User user = User.builder()
                    .id(UUID.randomUUID()).authServerId(authServerId).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.MEMBERSHIP_WITHDRAWAL_PENDING).build();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(user));

            userService.handleMembershipWithdrawn(authServerId, Instant.now());

            verifyNoInteractions(sessionService, auditLogService);
        }

        @Test
        @DisplayName("로컬 유저가 없으면 조용히 skip한다")
        void skipsWhenUserMissing() {
            final UUID authServerId = UUID.randomUUID();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.empty());

            userService.handleMembershipWithdrawn(authServerId, Instant.now());

            verifyNoInteractions(sessionService, auditLogService);
        }
    }

    @Nested
    @DisplayName("handleMembershipBanned — AS CLIENT_BANNED 이벤트 수신")
    class HandleMembershipBanned {

        @Test
        @DisplayName("NORMAL 유저는 PERMANENT_BAN으로 즉시 전환하고 세션을 삭제한다 — PII는 보존")
        void bansImmediately() {
            final UUID userId = UUID.randomUUID();
            final UUID authServerId = UUID.randomUUID();
            final User user = User.builder()
                    .id(userId).authServerId(authServerId).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.NORMAL).build();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(user));

            userService.handleMembershipBanned(authServerId);

            assertThat(user.getState()).isEqualTo(UserState.PERMANENT_BAN);
            assertThat(user.getEmail()).isEqualTo("someone@example.com");
            verify(sessionService).deleteAllSessions(authServerId);
            verify(auditLogService).record(eq(AuditEvent.MEMBERSHIP_BANNED), eq(userId), eq("USER"), any());
        }

        @Test
        @DisplayName("이미 PERMANENT_BAN이면 아무것도 하지 않는다 — idempotent")
        void noOpWhenAlreadyBanned() {
            final UUID authServerId = UUID.randomUUID();
            final User user = User.builder()
                    .id(UUID.randomUUID()).authServerId(authServerId).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.PERMANENT_BAN).build();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(user));

            userService.handleMembershipBanned(authServerId);

            verifyNoInteractions(sessionService, auditLogService);
        }
    }

    @Nested
    @DisplayName("handleMembershipActivated — AS CLIENT_ACTIVATED 이벤트 수신")
    class HandleMembershipActivated {

        @Test
        @DisplayName("MEMBERSHIP_WITHDRAWAL_PENDING 유저는 NORMAL로 복구되고 withdrawnAt이 초기화된다")
        void restoresFromPending() {
            final UUID userId = UUID.randomUUID();
            final UUID authServerId = UUID.randomUUID();
            final User user = User.builder()
                    .id(userId).authServerId(authServerId).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.MEMBERSHIP_WITHDRAWAL_PENDING).build();
            user.markMembershipWithdrawalPending(Instant.now());
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(user));

            userService.handleMembershipActivated(authServerId);

            assertThat(user.getState()).isEqualTo(UserState.NORMAL);
            assertThat(user.getMembershipWithdrawnAt()).isNull();
            verify(auditLogService).record(eq(AuditEvent.MEMBERSHIP_REACTIVATED), eq(userId), eq("USER"), any());
        }

        @Test
        @DisplayName("PERMANENT_BAN 유저도 동일 로직으로 NORMAL로 복구된다 — 밴 해제")
        void restoresFromBan() {
            final UUID authServerId = UUID.randomUUID();
            final User user = User.builder()
                    .id(UUID.randomUUID()).authServerId(authServerId).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.PERMANENT_BAN).build();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(user));

            userService.handleMembershipActivated(authServerId);

            assertThat(user.getState()).isEqualTo(UserState.NORMAL);
        }

        @Test
        @DisplayName("NORMAL 유저는 무시한다 — 신규 가입 동의 등 다른 이유로 발생한 이벤트")
        void ignoresNormalUser() {
            final UUID authServerId = UUID.randomUUID();
            final User user = User.builder()
                    .id(UUID.randomUUID()).authServerId(authServerId).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.NORMAL).build();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(user));

            userService.handleMembershipActivated(authServerId);

            assertThat(user.getState()).isEqualTo(UserState.NORMAL);
            verifyNoInteractions(auditLogService);
        }

        @Test
        @DisplayName("로컬 유저가 없으면(JIT 생성 전) 예외 없이 skip한다")
        void skipsWhenUserMissing() {
            final UUID authServerId = UUID.randomUUID();
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.empty());

            userService.handleMembershipActivated(authServerId);

            verifyNoInteractions(auditLogService);
        }
    }

    @Nested
    @DisplayName("finalizeMembershipWithdrawal — 30일 유예 경과 확정 배치에서 호출")
    class FinalizeMembershipWithdrawal {

        @Test
        @DisplayName("MEMBERSHIP_WITHDRAWAL_PENDING 유저는 익명화 + WITHDRAWN 확정 처리된다")
        void finalizesWithdrawal() {
            final UUID userId = UUID.randomUUID();
            final User user = User.builder()
                    .id(userId).authServerId(UUID.randomUUID()).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.MEMBERSHIP_WITHDRAWAL_PENDING).build();
            when(userRepository.findWithMbtiById(userId)).thenReturn(Optional.of(user));
            when(retentionEncryptionService.encrypt(any())).thenReturn("encrypted");

            userService.finalizeMembershipWithdrawal(userId);

            assertThat(user.getState()).isEqualTo(UserState.WITHDRAWN);
            assertThat(user.getEmail()).startsWith("withdrawn_");
        }

        @Test
        @DisplayName("그 사이 재연결되어 NORMAL이면 아무것도 하지 않는다 — 확정 배치와 재연결 사이 레이스 방지")
        void noOpWhenAlreadyReconnected() {
            final UUID userId = UUID.randomUUID();
            final User user = User.builder()
                    .id(userId).authServerId(UUID.randomUUID()).email("someone@example.com")
                    .nickname("nick1234").role(UserRole.USER).state(UserState.NORMAL).build();
            when(userRepository.findWithMbtiById(userId)).thenReturn(Optional.of(user));

            userService.finalizeMembershipWithdrawal(userId);

            assertThat(user.getState()).isEqualTo(UserState.NORMAL);
            verify(userRetentionLogRepository, never()).save(any());
        }
    }
}
