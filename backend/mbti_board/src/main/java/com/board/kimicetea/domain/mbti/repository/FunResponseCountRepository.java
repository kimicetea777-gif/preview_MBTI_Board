package com.board.kimicetea.domain.mbti.repository;

import com.board.kimicetea.domain.mbti.entity.FunResponseCount;
import com.board.kimicetea.domain.mbti.entity.FunResponseCountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * FN 응답 카운트 레포지토리
 * 주의: upsertCount 외 직접 save 사용 금지 — ON CONFLICT로 원자적 카운트 증가
 */
public interface FunResponseCountRepository extends JpaRepository<FunResponseCount, FunResponseCountId> {

    @Modifying
    @Query(value = """
            INSERT INTO fun_response_counts (option_id, mbti_id, total_count)
            VALUES (:optionId, :mbtiId, 1)
            ON CONFLICT (option_id, mbti_id) DO UPDATE
                SET total_count = fun_response_counts.total_count + 1
            """, nativeQuery = true)
    void upsertCount(@Param("optionId") Long optionId, @Param("mbtiId") String mbtiId);
}
