package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.mbti.MbtiMetadataSupport;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 신규 계정 생성 — 약관 동의 완료 API(POST /api/auth/oauth2/signup)에서만 호출됨
 * 주의: 이메일 필수 여부는 CustomOAuth2UserService.loadUser()에서 먼저 검증되지만,
 *       loadUser() 검증 시점과 실제 생성(여기) 사이 최대 signupToken 만료시간(10분) 간극이 있어
 *       그 사이 동일 이메일로 다른 경로가 먼저 가입할 수 있음 — DB UNIQUE 제약 위반을 DUPLICATE_EMAIL로 매핑해 방어
 */
@Service
@RequiredArgsConstructor
public class OAuth2AccountService {

    private final UserRepository userRepository;
    private final MbtiMetadataSupport mbtiMetadataSupport;

    @Transactional
    public User createAccount(final String provider, final String providerId, final String email,
            final String nickname, final String profileImageUrl) {
        final MbtiMetadata defaultMbti = mbtiMetadataSupport.getNoneMbti();

        final User user = User.builder()
                .email(email)
                .socialProvider(provider)
                .providerId(providerId)
                .nickname(resolveUniqueNickname(nickname))
                .profileImageUrl(profileImageUrl)
                .mbtiVerified(MbtiVerified.DEFAULT)
                .role(UserRole.USER)
                .state(UserState.NORMAL)
                .mbti(defaultMbti)
                .build();

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
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
