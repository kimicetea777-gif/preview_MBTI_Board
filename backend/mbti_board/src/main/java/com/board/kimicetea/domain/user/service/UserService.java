package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.user.dto.ChangePasswordRequest;
import com.board.kimicetea.domain.user.dto.MyProfileResponse;
import com.board.kimicetea.domain.user.dto.UpdateProfileRequest;
import com.board.kimicetea.domain.user.dto.UserProfileResponse;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
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
    private final PasswordEncoder passwordEncoder;
    private final TokenRedisService tokenRedisService;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    /** 회원가입 — AuthService에서 위임받아 유저 엔티티 생성 */
    @Transactional
    public void registerUser(final String email, final String rawPassword,
            final String nickname, final String mbtiId) {
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
