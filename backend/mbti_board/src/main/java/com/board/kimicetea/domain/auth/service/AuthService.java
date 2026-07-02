package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.EmailVerifyConfirmRequest;
import com.board.kimicetea.domain.auth.dto.EmailVerifySendRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailResponse;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.OAuth2SignupConfirmRequest;
import com.board.kimicetea.domain.auth.dto.PasswordResetConfirmRequest;
import com.board.kimicetea.domain.auth.dto.PasswordResetRequestBody;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.user.TermsPolicy;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.global.audit.AuditEvent;
import com.board.kimicetea.global.audit.AuditLogService;
import com.board.kimicetea.global.common.util.EmailUtils;
import com.board.kimicetea.global.common.util.RequestUtils;
import com.board.kimicetea.global.crypto.PhoneHashService;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.global.ratelimit.RateLimitService;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.service.UserService;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.TokenIssuerService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.oauth2.OAuth2AccountService;
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

import java.time.Instant;
import java.util.Arrays;
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
    private final RateLimitService rateLimitService;
    private final AuditLogService auditLogService;
    private final OAuth2AccountService oAuth2AccountService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${site.url}")
    private String siteUrl;

    /**
     * 전화번호로 이메일 찾기 — INACTIVE·LOCKED·PERMANENT_BAN·WITHDRAWN 상태는 사유를 알리고 차단
     * 주의: 이메일은 @앞 2자만 노출하고 나머지 *** 처리 (개인정보 보호)
     */
    @Transactional(readOnly = true)
    public FindEmailResponse findEmail(final FindEmailRequest request) {
        final String phoneHash = phoneHashService.hash(request.phone());
        final User user = userRepository.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateAccountAccessible(user);

        return new FindEmailResponse(EmailUtils.mask(user.getEmail()), user.getSocialProvider() != null);
    }

    /**
     * 비밀번호 재설정 이메일 발송 — 항상 동일 응답 반환 (이메일 열거 방지)
     * 주의: 로컬 계정이고 NORMAL/LOCKED 상태인 경우에만 실제 발송. 기존 토큰 있으면 먼저 무효화
     */
    @Transactional(readOnly = true)
    public void requestPasswordReset(final PasswordResetRequestBody request) {
        rateLimitService.checkByEmail("pw-reset", request.email(), 3, 600);
        userRepository.findByEmail(request.email())
                .filter(u -> u.getSocialProvider() == null)
                .filter(u -> u.getState() == UserState.NORMAL || u.getState() == UserState.LOCKED)
                .ifPresent(u -> issuePasswordResetToken(request.email()));
    }

    /** 기존 토큰 무효화 후 새 토큰 발급 및 이메일 발송 */
    private void issuePasswordResetToken(final String email) {
        final String oldToken = tokenRedisService.getPasswordResetTokenByEmail(email);
        if (oldToken != null)
            tokenRedisService.deletePasswordResetToken(oldToken, email);

        final String newToken = UUID.randomUUID().toString();
        tokenRedisService.savePasswordResetToken(newToken, email);
        emailService.sendPasswordResetEmail(email, siteUrl + "/account/reset-password?token=" + newToken);
    }

    /**
     * 비밀번호 재설정 확인 — 토큰 검증 후 비밀번호 변경 + 전 기기 강제 로그아웃
     * 주의: 토큰 사용 즉시 두 키 모두 삭제 (재사용 방지). 전 기기 RT도 삭제
     */
    @Transactional
    public void confirmPasswordReset(final PasswordResetConfirmRequest request,
            final HttpServletRequest httpRequest) {
        final String email = tokenRedisService.getEmailByPasswordResetToken(request.token())
                .orElseThrow(() -> new CustomException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        /* 토큰 정리 */
        tokenRedisService.deletePasswordResetToken(request.token(), email);
        tokenRedisService.deleteAllRefreshTokens(user.getId().toString());
        auditLogService.record(AuditEvent.PASSWORD_CHANGE, user.getId(),
                user.getRole().name(), RequestUtils.extractClientIp(httpRequest));
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
        rateLimitService.checkByEmail("email-verify", request.email(), 3, 600);

        final String token = UUID.randomUUID().toString();
        tokenRedisService.saveEmailVerifyToken(token, request.email());
        emailService.sendEmailVerification(request.email(), siteUrl + "/signup/verify?token=" + token);
    }

    /**
     * 이메일 인증 토큰 확인 — 토큰 유효 시 EV:DONE:{email}=1 (30분) 마킹
     * 주의: 토큰은 1회 사용 후 즉시 삭제 (재사용 방지)
     * 
     * @return 인증된 이메일 주소 (프론트가 회원가입 폼 단계로 이동할 때 사용)
     */
    public String confirmEmailVerification(final EmailVerifyConfirmRequest request) {
        final String email = tokenRedisService.getEmailByVerifyToken(request.token())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID));
        tokenRedisService.deleteEmailVerifyToken(request.token());
        tokenRedisService.markEmailVerified(email);
        return email;
    }

    /**
     * 회원가입 — 이메일 인증 완료 여부 검증 후 UserService에 유저 생성 위임
     * 주의: 가입 완료 후 EV:DONE 키 즉시 제거 (동일 이메일 중복 가입 방지)
     */
    @Transactional
    public void signup(final SignupRequest request, final HttpServletRequest httpRequest) {
        validateTermsAgreement(request.termsAgreed(), request.privacyAgreed());
        if (!tokenRedisService.isEmailVerified(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        final User user = userService.registerUser(
                request.email(), request.password(), request.nickname(), request.mbtiId(), request.phone());
        user.agreeToTerms(Instant.now(), TermsPolicy.CURRENT_TERMS_VERSION, TermsPolicy.CURRENT_PRIVACY_VERSION);
        tokenRedisService.clearEmailVerified(request.email());
        auditLogService.record(AuditEvent.SIGNUP, user.getId(),
                user.getRole().name(), RequestUtils.extractClientIp(httpRequest));
    }

    @Transactional
    public LoginResponse login(final LoginRequest request, final HttpServletRequest httpRequest,
            final HttpServletResponse response) {
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getSocialProvider() != null) {
            throw new CustomException(ErrorCode.SOCIAL_ONLY_ACCOUNT);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        validateAccountAccessible(user);

        final String deviceId = UUID.randomUUID().toString();
        final String accessToken = tokenIssuerService.issue(user, deviceId, response);

        user.updateLastLoginAt();
        auditLogService.record(AuditEvent.LOGIN, user.getId(),
                user.getRole().name(), RequestUtils.extractClientIp(httpRequest));

        return new LoginResponse(accessToken, user.getEmail(), user.getNickname(), user.getRole().name());
    }

    /**
     * 소셜 로그인 신규 가입 약관 동의 완료 — 이 시점에 처음으로 User를 생성
     * 주의: signupToken으로 이미 생성된 계정이 있으면(중복 호출) 새로 만들지 않고 그대로 로그인 처리 —
     *       이 경우 termsAgreedAt 등 최초 동의 이력을 덮어쓰지 않음
     */
    @Transactional
    public LoginResponse confirmOAuth2Signup(final OAuth2SignupConfirmRequest request,
            final HttpServletRequest httpRequest, final HttpServletResponse response) {
        validateTermsAgreement(request.termsAgreed(), request.privacyAgreed());

        final Claims claims = jwtProvider.validateAndParseClaims(request.signupToken())
                .filter(c -> "OAUTH2_SIGNUP".equals(c.get("purpose", String.class)))
                .orElseThrow(() -> new CustomException(ErrorCode.OAUTH2_SIGNUP_TOKEN_INVALID));

        final String provider = claims.get("provider", String.class);
        final String providerId = claims.get("providerId", String.class);

        final User user = userRepository.findBySocialProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    final User created = oAuth2AccountService.createAccount(
                            provider, providerId,
                            claims.get("email", String.class),
                            claims.get("nickname", String.class),
                            claims.get("profileImageUrl", String.class));
                    created.agreeToTerms(Instant.now(),
                            TermsPolicy.CURRENT_TERMS_VERSION, TermsPolicy.CURRENT_PRIVACY_VERSION);
                    auditLogService.record(AuditEvent.OAUTH2_SIGNUP, created.getId(),
                            created.getRole().name(), RequestUtils.extractClientIp(httpRequest));
                    return created;
                });

        final String deviceId = UUID.randomUUID().toString();
        final String accessToken = tokenIssuerService.issue(user, deviceId, response);

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
            auditLogService.record(AuditEvent.REUSE_DETECTED, UUID.fromString(userId),
                    "UNKNOWN", RequestUtils.extractClientIp(request));
            throw new CustomException(ErrorCode.REUSE_DETECTED);
        }

        final User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateAccountAccessible(user);

        // RT Rotation: 기존 RT 삭제 후 새 AT/RT 발급
        tokenRedisService.deleteRefreshToken(userId, deviceId);

        final String newAccessToken = tokenIssuerService.issue(user, deviceId, response);

        return new LoginResponse(newAccessToken, user.getEmail(), user.getNickname(), user.getRole().name());
    }

    /** AT 블랙리스트 등록 + Redis RT 삭제 + 클라이언트 Cookie 만료 처리 */
    public void logout(final String accessToken, final String userId,
            final String deviceId, final HttpServletResponse response) {
        // 만료된 AT는 블랙리스트 등록 불필요 — 필터에서 이미 거부됨
        jwtProvider.validateAndParseClaims(accessToken).ifPresent(claims -> {
            final long remaining = jwtProvider.getRemainingExpiry(claims);
            if (remaining > 0) {
                tokenRedisService.addToBlacklist(claims.getId(), remaining);
            }
        });
        tokenRedisService.deleteRefreshToken(userId, deviceId);
        cookieUtils.clearRefreshTokenCookie(response);
    }

    /** 약관 동의 여부 검증 — 향후 동의 항목이 추가되면 이 메소드만 확장하면 됨 (signup·confirmOAuth2Signup 공유) */
    private void validateTermsAgreement(final boolean termsAgreed, final boolean privacyAgreed) {
        if (!termsAgreed || !privacyAgreed) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }
    }

    /**
     * 계정 접근 가능 여부 검증 — INACTIVE·LOCKED·PERMANENT_BAN·WITHDRAWN 상태는 사용자에게 사유를 알리고 차단
     * 주의: SHADOW_BANNED는 콘텐츠 노출 제한일 뿐, 접근 자체는 허용
     */
    private void validateAccountAccessible(final User user) {
        switch (user.getState()) {
            case LOCKED -> throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
            case PERMANENT_BAN -> throw new CustomException(ErrorCode.ACCOUNT_BANNED);
            case INACTIVE -> throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
            case WITHDRAWN -> throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
            default -> {
            }
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
