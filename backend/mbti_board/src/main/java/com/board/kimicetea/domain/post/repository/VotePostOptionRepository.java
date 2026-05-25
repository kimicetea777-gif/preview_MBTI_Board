package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.VotePostOption;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 투표 선택지 레포지토리
 * 주의: vote_count는 DB 트리거가 관리 — JPA로 직접 수정 금지
 */
public interface VotePostOptionRepository extends JpaRepository<VotePostOption, Long> {
}
