package com.board.kimicetea.domain.mbti.repository;

import com.board.kimicetea.domain.mbti.entity.MbtiQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * MBTI 검사 문항 레포지토리
 * 주의: UNION ALL 네이티브 쿼리로 카테고리당 8개씩 균등 랜덤 추출 (DB 요청 1회)
 */
public interface MbtiQuestionRepository extends JpaRepository<MbtiQuestion, Long> {

    @Query(value = """
            (SELECT * FROM mbti_questions WHERE category = 'EI' AND test_version = :version ORDER BY RANDOM() LIMIT 8)
            UNION ALL
            (SELECT * FROM mbti_questions WHERE category = 'SN' AND test_version = :version ORDER BY RANDOM() LIMIT 8)
            UNION ALL
            (SELECT * FROM mbti_questions WHERE category = 'TF' AND test_version = :version ORDER BY RANDOM() LIMIT 8)
            UNION ALL
            (SELECT * FROM mbti_questions WHERE category = 'JP' AND test_version = :version ORDER BY RANDOM() LIMIT 8)
            """, nativeQuery = true)
    List<MbtiQuestion> findRandomQuestionsByVersion(@Param("version") String version);
}
