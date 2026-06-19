package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.entity.MbtiTestHistory;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.user.dto.ChangePasswordRequest;
import com.board.kimicetea.domain.user.dto.MyProfileResponse;
import com.board.kimicetea.domain.user.dto.UpdatePhoneRequest;
import com.board.kimicetea.domain.user.dto.UpdateProfileRequest;
import com.board.kimicetea.domain.user.dto.UserActivityStatsResponse;
import com.board.kimicetea.domain.user.dto.UserProfileResponse;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.crypto.PhoneHashService;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 유저 도메인 비즈니스 로직 (생성·조회·수정·탈퇴·비밀번호 변경)
 * 주의: 이메일·닉네임 중복 검증은 이 서비스가 단일 책임으로 담당
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MbtiMetadataRepository mbtiMetadataRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MbtiTestHistoryRepository mbtiTestHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneHashService phoneHashService;
    private final TokenRedisService tokenRedisService;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    /**
     * 어드민 유저 직접 생성 — 이메일 인증 없이 role·state 직접 지정
     * 주의: phone은 저장하지 않음 (어드민 생성 계정은 이메일 찾기 불필요)
     */
    @Transactional
    public void adminCreateUser(final String email, final String rawPassword,
            final String nickname, final String mbtiId, final UserRole role, final UserState state) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        validateNicknameNotDuplicate(nickname);

        final MbtiMetadata mbti = findMbtiOrThrow(mbtiId);
        final MbtiVerified mbtiVerified = mbti.isNone() ? MbtiVerified.DEFAULT : MbtiVerified.SELF;

        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .mbtiVerified(mbtiVerified)
                .role(role)
                .state(state)
                .mbti(mbti)
                .build());
    }

    /** 회원가입 — AuthService에서 위임받아 유저 엔티티 생성. phone은 null 허용 (이메일 찾기 전용) */
    @Transactional
    public void registerUser(final String email, final String rawPassword,
            final String nickname, final String mbtiId, final String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        validateNicknameNotDuplicate(nickname);

        String phoneHash = null;
        if (phone != null && !phone.isBlank()) {
            phoneHash = phoneHashService.hash(phone);
            if (userRepository.existsByPhoneHash(phoneHash)) {
                throw new CustomException(ErrorCode.DUPLICATE_PHONE);
            }
        }

        final MbtiMetadata mbti = findMbtiOrThrow(mbtiId);
        final MbtiVerified mbtiVerified = mbti.isNone() ? MbtiVerified.DEFAULT : MbtiVerified.SELF;

        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .phoneHash(phoneHash)
                .mbtiVerified(mbtiVerified)
                .role(UserRole.USER)
                .state(UserState.NORMAL)
                .mbti(mbti)
                .build());
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
     * 탈퇴 OTP 발송 — 6자리 코드를 생성해 Redis(WD:{userId}, TTL 3분)에 저장 후 이메일 발송
     */
    public void sendWithdrawalOtp(final UUID userId) {
        final User user = findUserOrThrow(userId);
        final String otp = generateOtp();
        tokenRedisService.saveWithdrawalOtp(userId.toString(), otp);
        emailService.sendWithdrawalOtp(user.getEmail(), otp);
    }

    /** 6자리 숫자 OTP 생성 — SecureRandom 사용으로 예측 불가 */
    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    /**
     * 회원 탈퇴 — OTP 검증 후 상태 WITHDRAWN 변경 + PII 즉시 익명화 + 전체 기기 토큰 무효화
     * 주의: AT 블랙리스트·RT 삭제는 @Transactional 외부에서 실행 (Redis는 JPA 트랜잭션 미참여)
     */
    @Transactional
    public void withdraw(final UUID userId, final String accessToken, final String otpCode) {
        final String savedOtp = tokenRedisService.getWithdrawalOtp(userId.toString());
        if (savedOtp == null) {
            throw new CustomException(ErrorCode.WITHDRAWAL_OTP_EXPIRED);
        }

        if (!savedOtp.equals(otpCode)) {
            // 오답 횟수 누적 — 3회 초과 시 OTP·카운터 모두 삭제
            final int attempts = tokenRedisService.incrementWithdrawalOtpAttempts(userId.toString());
            if (attempts >= TokenRedisService.WITHDRAWAL_OTP_MAX_ATTEMPTS) {
                tokenRedisService.deleteWithdrawalOtp(userId.toString());
                throw new CustomException(ErrorCode.WITHDRAWAL_OTP_MAX_ATTEMPTS);
            }
            throw new CustomException(ErrorCode.WITHDRAWAL_OTP_INVALID);
        }

        tokenRedisService.deleteWithdrawalOtp(userId.toString());

        final User user = findUserOrThrow(userId);
        user.changeState(UserState.WITHDRAWN);
        user.anonymize();

        tokenRedisService.deleteAllRefreshTokens(userId.toString());
        tokenRedisService.addToBlacklist(accessToken, jwtProvider.getRemainingExpiry(accessToken));
    }

    /**
     * 비밀번호 변경
     * 주의: 소셜 전용 계정(socialProvider != null)은 로컬 비밀번호가 없으므로 차단
     */
    @Transactional
    public void changePassword(final UUID userId, final ChangePasswordRequest request) {
        final User user = findUserOrThrow(userId);

        if (user.getSocialProvider() != null) {
            throw new CustomException(ErrorCode.SOCIAL_ONLY_ACCOUNT);
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 전화번호 등록/변경 — 중복 검증 후 HMAC 해시로 저장
     * 주의: 원본 전화번호는 저장하지 않으므로 현재 번호 조회 불가
     */
    @Transactional
    public void updatePhone(final UUID userId, final UpdatePhoneRequest request) {
        final String phoneHash = phoneHashService.hash(request.phone());
        if (userRepository.existsByPhoneHashAndIdNot(phoneHash, userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_PHONE);
        }
        final User user = findUserOrThrow(userId);
        user.changePhone(phoneHash);
    }

    /** 전화번호 삭제 */
    @Transactional
    public void removePhone(final UUID userId) {
        final User user = findUserOrThrow(userId);
        user.changePhone(null);
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
