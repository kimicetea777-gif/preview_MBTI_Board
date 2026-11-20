package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MembershipEventApplierTest {

    @Mock
    private UserService userService;

    private MembershipEventApplier applier;

    private final UUID authServerId = UUID.randomUUID();
    private final Instant changedAt = Instant.parse("2026-08-01T04:00:03.120Z");

    @BeforeEach
    void setUp() {
        applier = new MembershipEventApplier(userService);
    }

    @Test
    @DisplayName("CLIENT_WITHDRAWN은 handleMembershipWithdrawn(authServerId, changedAt)으로 위임한다")
    void dispatchesWithdrawn() {
        applier.apply(MembershipEventResponse.TYPE_CLIENT_WITHDRAWN, 1L, authServerId, changedAt);

        verify(userService).handleMembershipWithdrawn(authServerId, changedAt);
    }

    @Test
    @DisplayName("CLIENT_BANNED는 handleMembershipBanned(authServerId)로 위임한다")
    void dispatchesBanned() {
        applier.apply(MembershipEventResponse.TYPE_CLIENT_BANNED, 2L, authServerId, changedAt);

        verify(userService).handleMembershipBanned(authServerId);
    }

    @Test
    @DisplayName("CLIENT_ACTIVATED는 handleMembershipActivated(authServerId)로 위임한다")
    void dispatchesActivated() {
        applier.apply(MembershipEventResponse.TYPE_CLIENT_ACTIVATED, 3L, authServerId, changedAt);

        verify(userService).handleMembershipActivated(authServerId);
    }

    @Test
    @DisplayName("알 수 없는 타입은 아무것도 호출하지 않고 예외도 없다")
    void ignoresUnknownType() {
        assertThatCode(() -> applier.apply("UNKNOWN_TYPE", 4L, authServerId, changedAt)).doesNotThrowAnyException();

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("type이 null이면 NPE 없이 무시한다")
    void ignoresNullType() {
        assertThatCode(() -> applier.apply(null, 5L, authServerId, changedAt)).doesNotThrowAnyException();

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("핸들러 예외는 삼키지 않고 그대로 던진다 — 호출 측이 실패로 판단해야 한다")
    void propagatesHandlerFailure() {
        doThrow(new IllegalStateException("redis down")).when(userService).handleMembershipBanned(authServerId);

        assertThatThrownBy(() -> applier.apply(MembershipEventResponse.TYPE_CLIENT_BANNED, 6L, authServerId, changedAt))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("isKnownType: 세 타입만 true")
    void knownTypes() {
        assertThat(MembershipEventApplier.isKnownType(MembershipEventResponse.TYPE_CLIENT_WITHDRAWN)).isTrue();
        assertThat(MembershipEventApplier.isKnownType(MembershipEventResponse.TYPE_CLIENT_BANNED)).isTrue();
        assertThat(MembershipEventApplier.isKnownType(MembershipEventResponse.TYPE_CLIENT_ACTIVATED)).isTrue();
        assertThat(MembershipEventApplier.isKnownType("UNKNOWN_TYPE")).isFalse();
        assertThat(MembershipEventApplier.isKnownType(null)).isFalse();
    }
}
