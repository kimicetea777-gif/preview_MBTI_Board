package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 게시판 유형 조회 레포지토리 (code 기준 단건 조회 전용)
 */
public interface PostTypeRepository extends JpaRepository<PostType, Short> {

    Optional<PostType> findByCode(String code);
}
