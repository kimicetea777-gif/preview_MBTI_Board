package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.oauth2.userinfo.GoogleOAuth2UserInfo;
import com.board.kimicetea.global.security.oauth2.userinfo.KakaoOAuth2UserInfo;
import com.board.kimicetea.global.security.oauth2.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * OAuth2 로그인 후 사용자 정보를 DB에 동기화하는 서비스
 * 주의: social_provider + provider_id 조합으로 기존 회원 여부 판단. 신규 회원은 MBTI=NONE으로 자동 가입
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String DEFAULT_MBTI_ID = "NONE";

    private final UserRepository userRepository;
    private final MbtiMetadataRepository mbtiMetadataRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(final OAuth2UserRequest userRequest) {
        final OAuth2User oAuth2User = super.loadUser(userRequest);
        final String provider = userRequest.getClientRegistration().getRegistrationId();
        final OAuth2UserInfo userInfo = resolveUserInfo(provider, oAuth2User.getAttributes());
        final User user = findOrCreateUser(provider, userInfo);
        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private OAuth2UserInfo resolveUserInfo(final String provider, final Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(new OAuth2Error("UNSUPPORTED_PROVIDER"));
        };
    }

    private User findOrCreateUser(final String provider, final OAuth2UserInfo userInfo) {
        return userRepository
                .findBySocialProviderAndProviderId(provider, userInfo.getProviderId())
                .orElseGet(() -> createUser(provider, userInfo));
    }

    private User createUser(final String provider, final OAuth2UserInfo userInfo) {
        // 이메일 미동의 또는 제공 불가 시 가입 차단 — users.email NOT NULL 제약 보호
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("OAUTH2_EMAIL_REQUIRED"));
        }
        // 같은 이메일로 이미 로컬 계정이 존재하면 소셜 가입 차단
        if (userRepository.existsByEmail(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("DUPLICATE_EMAIL"));
        }

        final MbtiMetadata defaultMbti = mbtiMetadataRepository.findById(DEFAULT_MBTI_ID)
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));

        final User user = User.builder()
                .email(userInfo.getEmail())
                .socialProvider(provider)
                .providerId(userInfo.getProviderId())
                .nickname(resolveUniqueNickname(userInfo.getNickname()))
                .profileImageUrl(userInfo.getProfileImageUrl())
                .mbtiVerified(MbtiVerified.DEFAULT)
                .role(UserRole.USER)
                .state(UserState.NORMAL)
                .mbti(defaultMbti)
                .build();

        return userRepository.save(user);
    }

    /** 닉네임 중복 시 숫자 suffix를 붙여 고유 닉네임 생성 */
    private String resolveUniqueNickname(final String base) {
        final String safeName = (base != null && !base.isBlank()) ? base : "user";
        String candidate = safeName;
        int attempt = 0;
        while (userRepository.existsByNickname(candidate)) {
            candidate = safeName + "_" + (++attempt);
        }
        return candidate;
    }
}
