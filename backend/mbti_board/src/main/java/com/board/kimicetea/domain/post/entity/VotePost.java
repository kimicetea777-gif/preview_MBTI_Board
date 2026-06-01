package com.board.kimicetea.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "vote_posts")
public class VotePost {

    // posts.id를 PK로 공유
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "ends_at")
    private Instant endsAt; // NULL이면 무기한 투표

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /*-----------------------------------연관관계-------------------------------------------*/

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id")
    private Post post;

    // DB 트리거가 vote_count 관리 — voteCount 직접 수정 금지
    // 항상 2개 이상 존재 (생성 시 서비스에서 보장) — 빈 리스트는 정상 상태가 아님
    @Builder.Default
    @BatchSize(size = 30)
    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "votePost", fetch = FetchType.LAZY)
    private List<VotePostOption> options = new ArrayList<>();

}

