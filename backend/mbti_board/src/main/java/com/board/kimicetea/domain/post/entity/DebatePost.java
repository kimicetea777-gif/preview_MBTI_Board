package com.board.kimicetea.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "debate_posts")
public class DebatePost {

    @Id
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "pro_label", nullable = false)
    private String proLabel = "찬성";

    @Builder.Default
    @Column(name = "con_label", nullable = false)
    private String conLabel = "반대";

    // DB 트리거가 관리 — JPA 직접 수정 금지
    @Builder.Default
    @Column(name = "pro_count", nullable = false, insertable = false, updatable = false)
    private Integer proCount = 0;

    // DB 트리거가 관리 — JPA 직접 수정 금지
    @Builder.Default
    @Column(name = "con_count", nullable = false, insertable = false, updatable = false)
    private Integer conCount = 0;

    @Column(name = "ends_at")
    private Instant endsAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /*-----------------------------------연관관계-------------------------------------------*/

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id")
    private Post post;
}
