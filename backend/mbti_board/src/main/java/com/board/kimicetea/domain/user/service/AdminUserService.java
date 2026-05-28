package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.user.dto.AdminUserSummaryResponse;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 어드민 유저 관리 비즈니스 로직 (목록 조회·상태 변경·역할 변경)
 * 주의: 본 서비스 메서드는 SecurityConfig에서 ROLE_ADMIN 인가를 전제로 호출됨
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    /** 이메일·상태·역할 필터로 유저 목록 페이지 조회 */
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> getUsers(final String email, final UserState state,
            final UserRole role, final Pageable pageable) {
        return userRepository.searchUsers(email, state, role, pageable)
                .map(AdminUserSummaryResponse::from);
    }

    /**
     * 유저 상태 변경
     * 주의: 어드민 자기 자신의 상태 변경 금지
     */
    @Transactional
    public void changeUserState(final UUID adminId, final UUID targetId, final UserState state) {
        if (adminId.equals(targetId)) {
            throw new CustomException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF);
        }
        final User target = findUserOrThrow(targetId);
        target.changeState(state);
    }

    /**
     * 유저 역할 변경
     * 주의: 어드민 자기 자신의 역할 변경 금지
     */
    @Transactional
    public void changeUserRole(final UUID adminId, final UUID targetId, final UserRole role) {
        if (adminId.equals(targetId)) {
            throw new CustomException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF);
        }
        final User target = findUserOrThrow(targetId);
        target.changeRole(role);
    }

    private User findUserOrThrow(final UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
