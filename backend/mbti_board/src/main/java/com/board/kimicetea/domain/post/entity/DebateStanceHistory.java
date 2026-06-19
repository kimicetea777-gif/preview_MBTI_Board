package com.board.kimicetea.domain.post.entity;

import com.board.kimicetea.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 토론 게시글 진영 투표 내역 — 회원 1인당 게시글 1표, 변경 가능
 * 주의: pro_count / con_count는 DB 트리거가 관리 — JPA 직접 수정 금지
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "debate_stance_history")
public class DebateStanceHistory {

    @EmbeddedId
    private DebateStanceHistoryId id;

    @Column(name = "stance", nullable = false)
    private Short stance;

    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /*-----------------------------------연관관계-------------------------------------------*/

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private DebatePost debatePost;

    /*-----------------------------------팩토리/편의 메소드----------------------------------*/

    public static DebateStanceHistory create(final User user, final DebatePost debatePost, final short stance) {
        final DebateStanceHistory history = new DebateStanceHistory();
        history.id = new DebateStanceHistoryId(user.getId(), debatePost.getId());
        history.user = user;
        history.debatePost = debatePost;
        history.stance = stance;
        return history;
    }

    public void changeStance(final short newStance) {
        this.stance = newStance;
    }
}
