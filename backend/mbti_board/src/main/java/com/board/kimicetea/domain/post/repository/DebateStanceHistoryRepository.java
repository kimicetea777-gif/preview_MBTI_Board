package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.DebateStanceHistory;
import com.board.kimicetea.domain.post.entity.DebateStanceHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DebateStanceHistoryRepository extends JpaRepository<DebateStanceHistory, DebateStanceHistoryId> {

    @Query("SELECT d.stance FROM DebateStanceHistory d WHERE d.id.userId = :userId AND d.id.postId = :postId")
    Optional<Short> findStanceByUserIdAndPostId(@Param("userId") UUID userId, @Param("postId") Long postId);

    Optional<DebateStanceHistory> findByIdUserIdAndIdPostId(UUID userId, Long postId);

    void deleteByIdUserIdAndIdPostId(UUID userId, Long postId);
}
