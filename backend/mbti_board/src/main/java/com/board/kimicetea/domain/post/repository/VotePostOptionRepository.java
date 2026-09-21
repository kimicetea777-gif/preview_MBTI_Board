package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.dto.VoteMbtiStatRow;
import com.board.kimicetea.domain.post.entity.VotePostOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 투표 선택지 레포지토리
 * 주의: vote_count는 DB 트리거가 관리 — JPA로 직접 수정 금지
 */
public interface VotePostOptionRepository extends JpaRepository<VotePostOption, Long> {

    /** 선택지별 MBTI 분포 플랫 조회 — vote_option_mbti_counts는 트리거가 유지하는 사전 집계 테이블 */
    @Query(value = """
            SELECT
                vpo.id         AS optionId,
                vmc.mbti_id    AS mbtiId,
                vmc.vote_count AS count
            FROM vote_post_options vpo
            LEFT JOIN vote_option_mbti_counts vmc ON vpo.id = vmc.option_id
            WHERE vpo.post_id = :postId
            ORDER BY vpo.display_order, vmc.mbti_id
            """, nativeQuery = true)
    List<VoteMbtiStatRow> findMbtiStatsByPostId(@Param("postId") Long postId);
}
