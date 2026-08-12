package com.board.kimicetea.domain.user.entity;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DynamicUpdate
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    /** AS(Authorization Server, client_id=TypeButter)가 발급한 사용자 고유 식별자(sub) */
    @Column(name = "auth_server_id", unique = true, nullable = false)
    private UUID authServerId;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "nickname", unique = true, nullable = false, length = 12)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "mbti_verified", nullable = false)
    private MbtiVerified mbtiVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private UserState state;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * AS CLIENT_WITHDRAWN 이벤트 발생 시각(로컬 처리 시각 아님) — MembershipEventResponse.changedAt을
     * 그대로 저장. MEMBERSHIP_WITHDRAWAL_PENDING 상태일 때만 값이 있고, 이 값 기준으로 30일 유예 후 확정.
     */
    @Column(name = "membership_withdrawn_at")
    private Instant membershipWithdrawnAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /*-----------------------------------연관관계-------------------------------------------*/

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mbti_id", nullable = false)
    private MbtiMetadata mbti;

    /*-----------------------------------편의 메소드----------------------------------------*/

    public void updateLastLoginAt() {
        this.lastLoginAt = Instant.now();
    }

    public void changeState(final UserState state) {
        this.state = state;
    }

    public void changeMbti(final MbtiMetadata mbti, final MbtiVerified mbtiVerified) {
        this.mbti = mbti;
        this.mbtiVerified = mbtiVerified;
    }

    public void changeNickname(final String nickname) {
        this.nickname = nickname;
    }

    public void changeProfileImageUrl(final String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void changeRole(final UserRole role) {
        this.role = role;
    }

    /** AS CLIENT_WITHDRAWN 이벤트 수신 시 호출 — PII는 그대로 두고 로그인만 막는다 */
    public void markMembershipWithdrawalPending(final Instant membershipWithdrawnAt) {
        this.state = UserState.MEMBERSHIP_WITHDRAWAL_PENDING;
        this.membershipWithdrawnAt = membershipWithdrawnAt;
    }

    /** AS CLIENT_ACTIVATED(재연결/밴 해제) 수신 시 호출 — NORMAL로 복구 */
    public void restoreToNormal() {
        this.state = UserState.NORMAL;
        this.membershipWithdrawnAt = null;
    }

    /** 탈퇴 시 PII 즉시 제거 — email·nickname은 UNIQUE NOT NULL이므로 고유 플레이스홀더로 대체 */
    public void anonymize() {
        this.email = "withdrawn_" + this.id + "@deleted.invalid";
        this.nickname = "wd_" + this.id.toString().replace("-", "").substring(0, 9);
        this.profileImageUrl = null;
        this.lastLoginAt = null;
    }
}
