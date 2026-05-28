package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.DebatePost;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 토론 게시글 메타 레포지토리
 * 주의: proCount/conCount는 DB 트리거가 관리하므로 JPA로 직접 수정 금지
 */
public interface DebatePostRepository extends JpaRepository<DebatePost, Long> {
}
