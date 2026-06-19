package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.EmailVerifyConfirmRequest;
import com.board.kimicetea.domain.auth.dto.EmailVerifySendRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailResponse;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.PasswordResetConfirmRequest;
import com.board.kimicetea.domain.auth.dto.PasswordResetRequestBody;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.global.crypto.PhoneHashService;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.service.UserService;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.TokenIssuerService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.board.kimicetea.global.security.util.CookieUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * 로컬 인증 비즈니스 로직 (회원가입·로그인·재발급·로그아웃)
 * 주의: 유저 생성·닉네임 중복 검증은 UserService에 위임 — AuthService는 토큰 발급/폐기만 담당
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PhoneHashService phoneHashService;
    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;
    private final PasswordEncoder passwordEncoder;
    private final CookieUtils cookieUtils;
    private final TokenIssuerService tokenIssuerService;
    private final EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * 전화번호로 이메일 찾기 — WITHDRAWN 계정은 존재하지 않는 것으로 처리
     * 주의: 이메일은 @앞 2자만 노출하고 나머지 *** 처리 (개인정보 보호)
     */
    @Transactional(readOnly = true)
    public FindEmailResponse findEmail(final FindEmailRequest request) {
        final String phoneHash = phoneHashService.hash(request.phone());
        final User user = userRepository.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getState() == UserState.WITHDRAWN) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return new FindEmailResponse(maskEmail(user.getEmail()), user.getSocialProvider() != null);
    }

    /**
     * 이메일 마스킹 — @앞 2자 노출, 나머지 로컬 파트는 *** 처리
     * 예) abcdef@gmail.com → ab***@gmail.com
     */
    private String maskEmail(final String email) {
        final int atIndex = email.indexOf('@');
        final String local = email.substring(0, atIndex);
        final String domain = email.substring(atIndex);
        final String visiblePart = local.length() > 2 ? local.substring(0, 2) : local;
        return visiblePart + "***" + domain;
    }

    /**
     * 비밀번호 재설정 이메일 발송 — 항상 동일 응답 반환 (이메일 열거 방지)
     * 주의: 로컬 계정이고 NORMAL/LOCKED 상태인 경우에만 실제 발송. 기존 토큰 있으면 먼저 무효화
     */
    @Transactional(readOnly = true)
    public void requestPasswordReset(final PasswordResetRequestBody request) {
        final Optional<User> optionalUser = userRepository.findByEmail(request.email());
        if (optionalUser.isEmpty()) return;

        final User user = optionalUser.get();
        if (user.getSocialProvider() != null) return;

        final UserState state = user.getState();
        if (state != UserState.NORMAL && state != UserState.LOCKED) return;

        final String oldToken = tokenRedisService.getPasswordResetTokenByEmail(request.email());
        if (oldToken != null) {
            tokenRedisService.deletePasswordResetToken(oldToken, request.email());
        }

        final String newToken = UUID.randomUUID().toString();
        tokenRedisService.savePasswordResetToken(newToken, request.email());
        emailService.sendPasswordResetEmail(
                request.email(),
                frontendUrl + "/account/reset-password?token=" + newToken);
    }

    /**
     * 비밀번호 재설정 확인 — 토큰 검증 후 비밀번호 변경 + 전 기기 강제 로그아웃
     * 주의: 토큰 사용 즉시 두 키 모두 삭제 (재사용 방지). 전 기기 RT도 삭제
     */
    @Transactional
    public void confirmPasswordReset(final PasswordResetConfirmRequest request) {
        final String email = tokenRedisService.getEmailByPasswordResetToken(request.token());
        if (email == null) {
            throw new CustomException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(request.newPassword()));

        tokenRedisService.deletePasswordResetToken(request.token(), email);
        tokenRedisService.deleteAllRefreshTokens(user.getId().toString());
    }

    /**
     * 이메일 인증 링크 발송 — Key: EV:TOKEN:{uuid}=email (10분)
     * 주의: 링크는 프론트엔드 /signup/verify?token={uuid} 로 연결되며 프론트가 confirm API 호출
     * 주의: 이미 가입된 이메일이면 DUPLICATE_EMAIL 예외 발생
     */
    public void sendEmailVerification(final EmailVerifySendRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        final String token = UUID.randomUUID().toString();
        tokenRedisService.saveEmailVerifyToken(token, request.email());
        emailService.sendEmailVerification(request.email(), frontendUrl + "/signup/verify?token=" + token);
    }

    /**
     * 이메일 인증 토큰 확인 — 토큰 유효 시 EV:DONE:{email}=1 (30분) 마킹
     * 주의: 토큰은 1회 사용 후 즉시 삭제 (재사용 방지)
     * 
     * @return 인증된 이메일 주소 (프론트가 회원가입 폼 단계로 이동할 때 사용)
     */
    public String confirmEmailVerification(final EmailVerifyConfirmRequest request) {
        final String email = tokenRedisService.getEmailByVerifyToken(request.token());
        if (email == null) {
            throw new CustomException(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID);
        }
        tokenRedisService.deleteEmailVerifyToken(request.token());
        tokenRedisService.markEmailVerified(email);
        return email;
    }

    /**
     * 회원가입 — 이메일 인증 완료 여부 검증 후 UserService에 유저 생성 위임
     * 주의: 가입 완료 후 EV:DONE 키 즉시 제거 (동일 이메일 중복 가입 방지)
     */
    @Transactional
    public void signup(final SignupRequest request) {
        if (!tokenRedisService.isEmailVerified(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        userService.registerUser(request.email(), request.password(), request.nickname(), request.mbtiId(), request.phone());
        tokenRedisService.clearEmailVerified(request.email());
    }

    @Transactional
    public LoginResponse login(final LoginRequest request, final HttpServletResponse response) {
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getSocialProvider() != null) {
            throw new CustomException(ErrorCode.SOCIAL_ONLY_ACCOUNT);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        validateAccountState(user);

        final String deviceId = UUID.randomUUID().toString();
        final String accessToken = tokenIssuerService.issue(user, deviceId, response);

        user.updateLastLoginAt();

        return new LoginResponse(accessToken, user.getEmail(), user.getNickname(), user.getRole().name());
    }

    @Transactional
    public LoginResponse reissue(final HttpServletRequest request, final HttpServletResponse response) {
        final String oldRefreshToken = extractRefreshTokenFromCookie(request);

        final Claims claims = parseRefreshTokenOrThrow(oldRefreshToken);
        final String userId = claims.getSubject();
        final String deviceId = claims.get("deviceId", String.class);

        final String stored = tokenRedisService.getRefreshToken(userId, deviceId);

        if (stored == null) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        if (!stored.equals(oldRefreshToken)) {
            tokenRedisService.deleteAllRefreshTokens(userId);
            throw new CustomException(ErrorCode.REUSE_DETECTED);
        }

        final User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // RT Rotation: 기존 RT 삭제 후 새 AT/RT 발급
        tokenRedisService.deleteRefreshToken(userId, deviceId);

        final String newAccessToken = tokenIssuerService.issue(user, deviceId, response);

        return new LoginResponse(newAccessToken, user.getEmail(), user.getNickname(), user.getRole().name());
    }

    /** AT 블랙리스트 등록 + Redis RT 삭제 + 클라이언트 Cookie 만료 처리 */
    public void logout(final String accessToken, final String userId,
            final String deviceId, final HttpServletResponse response) {
        tokenRedisService.addToBlacklist(accessToken, jwtProvider.getRemainingExpiry(accessToken));
        tokenRedisService.deleteRefreshToken(userId, deviceId);
        cookieUtils.clearRefreshTokenCookie(response);
    }

    /**
     * 계정 상태 검증 — 로그인 불가 상태 차단
     * 주의: SHADOW_BANNED는 콘텐츠 노출 제한일 뿐, 로그인 자체는 허용
     */
    private void validateAccountState(final User user) {
        switch (user.getState()) {
            case LOCKED -> throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
            case PERMANENT_BAN -> throw new CustomException(ErrorCode.ACCOUNT_BANNED);
            case INACTIVE -> throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
            case WITHDRAWN -> throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
            default -> {
                /* NORMAL, PENDING, SHADOW_BANNED 는 통과 */ }
        }
    }

    private String extractRefreshTokenFromCookie(final HttpServletRequest request) {
        if (request.getCookies() == null)
            throw new CustomException(ErrorCode.MISSING_REFRESH_TOKEN);
        return Arrays.stream(request.getCookies())
                .filter(c -> CookieUtils.REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MISSING_REFRESH_TOKEN));
    }

    private Claims parseRefreshTokenOrThrow(final String token) {
        return jwtProvider.validateAndParseClaims(token)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
    }
}
