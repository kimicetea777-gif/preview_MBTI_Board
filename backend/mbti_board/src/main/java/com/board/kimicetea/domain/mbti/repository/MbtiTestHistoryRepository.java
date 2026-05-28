package com.board.kimicetea.domain.mbti.repository;

import com.board.kimicetea.domain.mbti.entity.MbtiTestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MbtiTestHistoryRepository extends JpaRepository<MbtiTestHistory, Long> {

    @Query("SELECT h FROM MbtiTestHistory h JOIN FETCH h.mbti WHERE h.user.id = :userId ORDER BY h.createdAt DESC")
    List<MbtiTestHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
