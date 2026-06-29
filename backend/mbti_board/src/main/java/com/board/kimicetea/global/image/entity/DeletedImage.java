package com.board.kimicetea.global.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 삭제 예약된 R2 이미지 레코드 — 배치 정리 대기 목록
 * PENDING: 업로드 직후 등록, 게시글 저장 성공 시 제거
 * DELETE_REQUESTED: 즉시 삭제 실패 시 전환, 스케줄러 재시도 대상
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "deleted_images")
public class DeletedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "url", nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeletedImageStatus status;

    @CreationTimestamp
    @Column(name = "marked_at", nullable = false, updatable = false)
    private Instant markedAt;

    /*---------------------------------편의--------------------------------- */
    /** 업로드 직후 PENDING 레코드 생성 */
    public static @NonNull DeletedImage pending(final String url) {
        final DeletedImage record = new DeletedImage();
        record.url = url;
        record.status = DeletedImageStatus.PENDING;
        return record;
    }

    /** 게시글 수정/삭제로 제거된 이미지 — 24h 후 스케줄러 삭제 */
    public static @NonNull DeletedImage markedForCleanup(final String url) {
        final DeletedImage record = new DeletedImage();
        record.url = url;
        record.status = DeletedImageStatus.PENDING;
        return record;
    }

    /** 즉시 삭제 실패 시 호출 — 스케줄러 재시도 대상으로 전환 */
    public void markDeleteRequested() {
        this.status = DeletedImageStatus.DELETE_REQUESTED;
    }
}
