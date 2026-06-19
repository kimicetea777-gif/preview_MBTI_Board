package com.board.kimicetea.domain.mbti.repository;

import com.board.kimicetea.domain.mbti.dto.FunStatRow;
import com.board.kimicetea.domain.mbti.entity.FunQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * FN 질문 레포지토리
 * 주의: JOIN FETCH로 options N+1 방지
 */
public interface FunQuestionRepository extends JpaRepository<FunQuestion, Long> {

    /** DB에서 랜덤 ID limit개 추출 — is_visible=true인 문항만, Java 셔플 없이 DB가 직접 랜덤 선택 */
    @Query(value = "SELECT id FROM fun_questions WHERE test_version = :version AND is_visible = true ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<Long> findRandomIdsByVersion(@Param("version") String version, @Param("limit") int limit);

    /** ID 목록으로 options JOIN FETCH — findRandomIdsByVersion 결과로 호출 */
    @Query("SELECT DISTINCT q FROM FunQuestion q JOIN FETCH q.options WHERE q.id IN :ids")
    List<FunQuestion> findAllWithOptionsByIds(@Param("ids") List<Long> ids);

    /** 어드민 전체 조회 — is_visible 무관, options JOIN FETCH, ID 오름차순 */
    @Query("SELECT DISTINCT q FROM FunQuestion q JOIN FETCH q.options WHERE q.testVersion = :version ORDER BY q.id ASC")
    List<FunQuestion> findAllWithOptionsByTestVersionOrderByIdAsc(@Param("version") String version);

    /** 단건 조회 + options JOIN FETCH — 어드민 수정 시 사용 */
    @Query("SELECT DISTINCT q FROM FunQuestion q JOIN FETCH q.options WHERE q.id = :id")
    java.util.Optional<FunQuestion> findByIdWithOptions(@Param("id") Long id);

    /** 전체 통계 플랫 조회 — 질문·선택지·카운트 한 번에 */
    @Query(value = """
            SELECT
                fq.id            AS questionId,
                fq.title         AS questionTitle,
                fqo.id           AS optionId,
                fqo.option_text  AS optionText,
                fqo.display_order AS displayOrder,
                frc.mbti_id      AS mbtiId,
                frc.count        AS count
            FROM fun_questions fq
            JOIN fun_question_options fqo ON fq.id = fqo.question_id
            LEFT JOIN fun_response_counts frc ON fqo.id = frc.option_id
            WHERE fq.test_version = :version
            ORDER BY fq.id, fqo.display_order, frc.mbti_id
            """, nativeQuery = true)
    List<FunStatRow> findAllStats(@Param("version") String version);
}
