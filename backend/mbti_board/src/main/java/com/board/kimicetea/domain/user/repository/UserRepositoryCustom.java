package com.board.kimicetea.domain.user.repository;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 유저 목록 QueryDSL 커스텀 조회 인터페이스 (어드민 전용) */
public interface UserRepositoryCustom {

    /**
     * 이메일·상태·역할 조건으로 유저 목록을 페이지네이션 조회
     * 주의: 세 필터 모두 null 허용 — null이면 해당 조건 무시
     */
    Page<User> searchUsers(String email, UserState state, UserRole role, Pageable pageable);
}
