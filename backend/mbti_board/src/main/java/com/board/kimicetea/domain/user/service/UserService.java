package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.entity.MbtiTestHistory;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.user.dto.MyProfileResponse;
import com.board.kimicetea.domain.user.dto.UpdateProfileRequest;
import com.board.kimicetea.domain.user.dto.UserActivityStatsResponse;
import com.board.kimicetea.domain.user.dto.UserProfileResponse;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.entity.UserRetentionLog;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.repository.UserRetentionLogRepository;
import com.board.kimicetea.global.crypto.RetentionEncryptionService;
import com.board.kimicetea.global.audit.AuditEvent;
import com.board.kimicetea.global.audit.AuditLogService;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 유저 도메인 비즈니스 로직 (생성·조회·수정·탈퇴·비밀번호 변경)
 * 주의: 이메일·닉네임 중복 검증은 이 서비스가 단일 책임으로 담당
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRetentionLogRepository userRetentionLogRepository;
    private final MbtiMetadataRepository mbtiMetadataRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MbtiTestHistoryRepository mbtiTestHistoryRepository;
    private final RetentionEncryptionService retentionEncryptionService;
    private final SessionService sessionService;
    private final AuditLogService auditLogService;

    /** 닉네임 실시간 중복 체크 — 회원가입 폼에서 제출 전 확인용 */
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(final String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(final UUID userId) {
        final User user = findUserOrThrow(userId);
        return MyProfileResponse.from(user);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(final UUID userId, final UpdateProfileRequest request) {
        final User user = findUserOrThrow(userId);

        if (request.nickname() != null) {
            final String trimmed = request.nickname().strip();
            if (!trimmed.equals(user.getNickname())) {
                validateNicknameNotDuplicate(trimmed);
                user.changeNickname(trimmed);
            }
        }

        if (request.profileImageUrl() != null) {
            user.changeProfileImageUrl(request.profileImageUrl());
        }

        if (request.mbtiId() != null) {
            user.changeMbti(findMbtiOrThrow(request.mbtiId()), MbtiVerified.SELF);
        }

        return MyProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(final UUID userId) {
        final User user = findUserOrThrow(userId);
        return UserProfileResponse.from(user);
    }

    /** 내 활동 통계 조회 — 게시글 수·댓글 수·최근 검사 MBTI·검사 일시 */
    @Transactional(readOnly = true)
    public UserActivityStatsResponse getMyActivityStats(final UUID userId) {
        final long postCount = postRepository.countByUserId(userId);
        final long commentCount = commentRepository.countByUserId(userId);
        final List<MbtiTestHistory> latest = mbtiTestHistoryRepository.findLatestByUserId(
                userId, PageRequest.of(0, 1));
        final MbtiTestHistory latestHistory = latest.isEmpty() ? null : latest.get(0);
        final String latestTestMbtiId = latestHistory == null ? null : latestHistory.getMbti().getId();
        final Instant latestTestDate  = latestHistory == null ? null : latestHistory.getCreatedAt();
        return UserActivityStatsResponse.of(postCount, commentCount, latestTestMbtiId, latestTestDate);
    }

    /**
     * AS 계정 자체가 하드 삭제됐을 때(DeletedUserSyncService, connect_manual.md §8-3) 호출 — AS
     * 마스터 계정이 이미 영구 삭제된 상태라 로컬이 이미 WITHDRAWN이면 아무것도 하지 않음 — 폴링
     * 재시도/중복 이벤트에 대해 idempotent해야 함.
     */
    @Transactional
    public void forceWithdrawFromAsDeletion(final UUID userId) {
        final User user = findUserOrThrow(userId);
        if (user.getState() == UserState.WITHDRAWN) {
            return;
        }
        performWithdrawal(user, "AS_DELETION_SYNC");
    }

    /**
     * AS 멤버십 이벤트 CLIENT_WITHDRAWN 수신 시 호출 (MembershipEventSyncService) — 로컬 유저가 없으면
     * (아직 JIT 계정 생성 전) 조용히 skip. 이미 WITHDRAWN(확정 완료) 또는 이미
     * MEMBERSHIP_WITHDRAWAL_PENDING이면 idempotent no-op — 최초 이벤트의 changedAt만 유지한다.
     */
    @Transactional
    public void handleMembershipWithdrawn(final UUID authServerId, final Instant changedAt) {
        userRepository.findByAuthServerId(authServerId).ifPresent(user -> {
            if (user.getState() == UserState.WITHDRAWN || user.getState() == UserState.MEMBERSHIP_WITHDRAWAL_PENDING) {
                return;
            }
            user.markMembershipWithdrawalPending(changedAt);
            sessionService.deleteAllSessions(user.getAuthServerId());
            auditLogService.record(AuditEvent.WITHDRAWAL, user.getId(), user.getRole().name(), "AS_MEMBERSHIP_WITHDRAWN");
        });
    }

    /**
     * AS 멤버십 이벤트 CLIENT_BANNED 수신 시 호출 — 로컬 유저가 없으면 skip. 이미 PERMANENT_BAN이면
     * idempotent no-op. 밴은 "떠남"이 아니므로 PII는 건드리지 않는다.
     */
    @Transactional
    public void handleMembershipBanned(final UUID authServerId) {
        userRepository.findByAuthServerId(authServerId).ifPresent(user -> {
            if (user.getState() == UserState.PERMANENT_BAN) {
                return;
            }
            user.changeState(UserState.PERMANENT_BAN);
            sessionService.deleteAllSessions(user.getAuthServerId());
            auditLogService.record(AuditEvent.MEMBERSHIP_BANNED, user.getId(), user.getRole().name(), "AS_MEMBERSHIP_BANNED");
        });
    }

    /**
     * AS 멤버십 이벤트 CLIENT_ACTIVATED 수신 시 호출 — 재연결이든 밴 해제든 동일 로직으로 NORMAL 복구.
     * 로컬 유저가 아직 없으면(신규 가입 동의만 하고 로그인 전인 경우) 조용히 skip — 실제 로그인 시
     * JIT으로 생성되며 별도 조치가 필요 없다. NORMAL 등 그 외 상태면 무시.
     */
    @Transactional
    public void handleMembershipActivated(final UUID authServerId) {
        userRepository.findByAuthServerId(authServerId).ifPresent(user -> {
            if (user.getState() != UserState.MEMBERSHIP_WITHDRAWAL_PENDING && user.getState() != UserState.PERMANENT_BAN) {
                return;
            }
            user.restoreToNormal();
            auditLogService.record(AuditEvent.MEMBERSHIP_REACTIVATED, user.getId(), user.getRole().name(), "AS_MEMBERSHIP_ACTIVATED");
        });
    }

    /**
     * MembershipWithdrawalFinalizeService(30일 유예 경과 확정 배치)에서 호출 — 그 사이 재연결됐다면
     * (레이스로 인해) 이미 NORMAL이므로 상태 가드로 걸러낸다.
     */
    @Transactional
    public void finalizeMembershipWithdrawal(final UUID userId) {
        final User user = findUserOrThrow(userId);
        if (user.getState() != UserState.MEMBERSHIP_WITHDRAWAL_PENDING) {
            return;
        }
        performWithdrawal(user, "MEMBERSHIP_WITHDRAWAL_GRACE_EXPIRED");
    }

    /**
     * 수사 협조용 보존 로그 저장 → PII 즉시 익명화 → 전체 기기 세션 무효화 → 감사 로그.
     * 주의: 보존 로그는 반드시 anonymize() 호출 전에 저장 — 익명화 후에는 원본 email 소실
     * 주의: 세션 삭제는 @Transactional 외부에서 실행 (Redis는 JPA 트랜잭션 미참여)
     */
    private void performWithdrawal(final User user, final String ipOrMarker) {
        saveRetentionLog(user, ipOrMarker);

        user.changeState(UserState.WITHDRAWN);
        user.anonymize();

        sessionService.deleteAllSessions(user.getAuthServerId());

        auditLogService.record(AuditEvent.WITHDRAWAL, user.getId(), user.getRole().name(), ipOrMarker);
    }

    /** anonymize() 이전에 호출 — 원본 email이 필요하므로 순서 변경 금지 */
    private void saveRetentionLog(final User user, final String ip) {
        userRetentionLogRepository.save(Objects.requireNonNull(UserRetentionLog.of(
                user.getId(),
                retentionEncryptionService.encrypt(user.getEmail()),
                ip
        )));
    }

    /** 닉네임 중복 검증 **/
    private void validateNicknameNotDuplicate(final String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private MbtiMetadata findMbtiOrThrow(final String mbtiId) {
        return mbtiMetadataRepository.findById(mbtiId)
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));
    }

    private User findUserOrThrow(final UUID userId) {
        return userRepository.findWithMbtiById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
