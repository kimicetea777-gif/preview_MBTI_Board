package com.board.kimicetea.global.image.repository;

import com.board.kimicetea.global.image.entity.DeletedImage;
import com.board.kimicetea.global.image.entity.DeletedImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/** 삭제 예약 이미지 레포지토리 — status·marked_at 기준 배치 조회 및 삭제 */
public interface DeletedImageRepository extends JpaRepository<DeletedImage, Long> {

    /** 특정 status이면서 marked_at이 cutoff 이전인 레코드 조회 (스케줄러용) */
    List<DeletedImage> findAllByStatusAndMarkedAtBefore(DeletedImageStatus status, Instant cutoff);

    /** DELETE_REQUESTED 전체 조회 (즉시 삭제 실패 재시도용) */
    List<DeletedImage> findAllByStatus(DeletedImageStatus status);

    /** 주어진 URL 목록 중 PENDING 레코드를 한 번에 삭제 (게시글 저장 성공 시 호출) */
    @Modifying
    @Query("DELETE FROM DeletedImage d WHERE d.url IN :urls AND d.status = :status")
    void deleteByUrlsAndStatus(@Param("urls") List<String> urls, @Param("status") DeletedImageStatus status);

    /** URL 목록으로 레코드 조회 (취소 시 R2 즉시 삭제 대상 확인) */
    List<DeletedImage> findAllByUrlIn(List<String> urls);
}
