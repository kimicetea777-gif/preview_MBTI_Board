package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.VotePostHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 유저 투표 내역 레포지토리 — 1인 1표 UPSERT 패턴 지원
 * 주의: UNIQUE 제약(user_id, post_id) — DB가 중복 투표를 막으므로 서비스에서도 사전 조회 후 UPDATE 처리
 */
public interface VotePostHistoryRepository extends JpaRepository<VotePostHistory, Long> {

    @Query("SELECT h FROM VotePostHistory h JOIN FETCH h.votePostOption JOIN FETCH h.user WHERE h.votePost.id = :postId AND h.user.id = :userId")
    Optional<VotePostHistory> findByVotePost_IdAndUser_Id(@Param("postId") Long postId, @Param("userId") UUID userId);

    /** 댓글 목록 조회 시 투표 선택지 일괄 조회 — userId 목록에 해당하는 투표 내역 반환 */
    @Query("SELECT h FROM VotePostHistory h JOIN FETCH h.votePostOption JOIN FETCH h.user WHERE h.votePost.id = :postId AND h.user.id IN :userIds")
    List<VotePostHistory> findByVotePost_IdAndUser_IdIn(@Param("postId") Long postId, @Param("userIds") List<UUID> userIds);
}
