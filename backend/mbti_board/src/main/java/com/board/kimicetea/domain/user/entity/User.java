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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "social_provider")
    private String socialProvider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "nickname", unique = true, nullable = false, length = 50)
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

    public void changePassword(final String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeRole(final UserRole role) {
        this.role = role;
    }

    /** 탈퇴 시 PII 즉시 제거 — email·nickname은 UNIQUE NOT NULL이므로 고유 플레이스홀더로 대체 */
    public void anonymize() {
        this.email = "withdrawn_" + this.id + "@deleted.invalid";
        this.nickname = "withdrawn_" + this.id.toString().substring(0, 8);
        this.password = null;
        this.profileImageUrl = null;
        this.socialProvider = null;
        this.providerId = null;
    }
}
