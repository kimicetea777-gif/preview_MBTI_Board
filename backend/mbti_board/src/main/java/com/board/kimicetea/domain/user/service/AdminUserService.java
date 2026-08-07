package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.user.dto.AdminUpdateUserRequest;
import com.board.kimicetea.domain.user.dto.AdminUserSummaryResponse;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserSortField;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final SessionService sessionService;

    /** 이메일·상태·역할 필터로 유저 목록 페이지 조회 */
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> getUsers(final String email, final UserState state,
            final UserRole role, final UserSortField sortBy, final Sort.Direction direction,
            final Pageable pageable) {
        return userRepository.searchUsers(email, state, role, sortBy, direction, pageable)
                .map(AdminUserSummaryResponse::from);
    }

    /**
     * 유저 통합 수정 — 닉네임·프로필 사진·역할·상태를 단일 트랜잭션으로 처리
     * 주의: null 필드는 변경하지 않음. 어드민 자기 자신 수정 금지
     */
    @Transactional
    public void updateUser(final UUID adminId, final UUID targetId, final AdminUpdateUserRequest request) {
        if (adminId.equals(targetId)) {
            throw new CustomException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF);
        }
        final User target = findUserOrThrow(targetId);
        if (request.nickname() != null) {
            final String trimmed = request.nickname().strip();
            if (!trimmed.equals(target.getNickname())) {
                if (userRepository.existsByNickname(trimmed)) {
                    throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
                }
            }
            target.changeNickname(trimmed);
        }
        if (Boolean.TRUE.equals(request.clearProfileImage())) {
            target.changeProfileImageUrl(null);
        }
        if (request.role() != null) {
            target.changeRole(request.role());
        }
        if (request.state() != null) {
            target.changeState(request.state());
            // 관리자 강제 탈퇴 시 PII 즉시 익명화 + 전체 기기 세션 폐기
            if (UserState.WITHDRAWN.equals(request.state())) {
                target.anonymize();
                sessionService.deleteAllSessions(target.getAuthServerId());
            }
        }
    }

    private User findUserOrThrow(final UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
