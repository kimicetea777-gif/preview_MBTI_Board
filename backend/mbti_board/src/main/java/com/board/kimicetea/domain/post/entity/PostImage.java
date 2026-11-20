package com.board.kimicetea.domain.post.entity;

import com.board.kimicetea.domain.post.enums.PostImageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 게시글이 소유한 Image Server 이미지 + 확정·정리 작업 상태(outbox).
 * 주의: 등록은 PostImageRepository#claim(ON CONFLICT DO NOTHING)으로만 하고(먼저 등록한 글이 소유자),
 *       상태 변경은 전부 리포지토리의 조건부 UPDATE로 한다 — 이 엔티티는 조회 전용으로 쓴다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post_images")
public class PostImage {

    @Id
    @Column(name = "image_id")
    private UUID imageId;

    /** 관리자 영구 삭제 후에는 NULL(FK ON DELETE SET NULL) — 그때 상태는 항상 PENDING_RELEASE */
    @Column(name = "post_id")
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostImageStatus status;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
}
