package com.board.kimicetea.domain.post.entity;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.post.enums.PostHiddenBy;
import com.board.kimicetea.domain.user.entity.User;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Column(name = "content_preview", length = 300)
    private String contentPreview;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "image_urls", columnDefinition = "text[]")
    private String[] imageUrls;

    @Column(name = "guest_password")
    private String guestPassword;

    @Column(name = "author_ip", length = 45)
    private String authorIp;

    // PostRepository.increaseViewCount(@Modifying 쿼리)로 원자적 증가 — JPA 직접 수정 금지
    @Builder.Default
    @Column(name = "view_count", nullable = false, insertable = false, updatable = false)
    private Integer viewCount = 0;

    // DB 트리거가 관리 — JPA 직접 수정 금지
    @Builder.Default
    @Column(name = "comment_count", nullable = false, insertable = false, updatable = false)
    private Integer commentCount = 0;

    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "hidden_by", length = 10)
    private PostHiddenBy hiddenBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /*-----------------------------------연관관계-------------------------------------------*/

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_mbti", nullable = false)
    private MbtiMetadata authorMbti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_mbti")
    private MbtiMetadata targetMbti;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType;

    @OneToOne(mappedBy = "post", fetch = FetchType.LAZY)
    private DebatePost debatePost;

    @OneToOne(mappedBy = "post", fetch = FetchType.LAZY)
    private VotePost votePost;

    /*-----------------------------------편의 메소드----------------------------------------*/

    public void hide() {
        this.isVisible = false;
        this.hiddenBy = PostHiddenBy.USER;
    }

    /** 어드민 숨김/복구 전용 — 일반 사용자 플로우에서는 hide() 사용 */
    public void changeVisibility(final boolean visible) {
        this.isVisible = visible;
        this.hiddenBy = visible ? null : PostHiddenBy.ADMIN;
    }

    /** 어드민 고정/고정 해제 — 목록 조회 시 최상단 노출 여부 */
    public void changePinned(final boolean pinned) {
        this.isPinned = pinned;
    }

    public void update(final String title, final String content, final String contentPreview, final String[] imageUrls) {
        this.title = title;
        this.content = content;
        this.contentPreview = contentPreview;
        this.imageUrls = imageUrls;
    }
}
