package com.board.kimicetea.domain.post.entity;

import com.board.kimicetea.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "vote_posts_history")
public class VotePostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // 투표(등록/변경) 시점의 유저 mbti_id 스냅샷 — DB 트리거가 관리, JPA 직접 수정 금지
    // (vote_option_mbti_counts 카운트 동기화 트리거가 users 재조회 없이 이 값만 참조함)
    @Column(name = "mbti_id", length = 6, insertable = false, updatable = false)
    private String mbtiId;

    /*-----------------------------------연관관계-------------------------------------------*/

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private VotePost votePost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private VotePostOption votePostOption;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /*-----------------------------------편의 메소드----------------------------------------*/

    public void changeOption(VotePostOption votePostOption) {
        this.votePostOption = votePostOption;
    }
}
