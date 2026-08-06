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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AS(Authorization Server) 로그인 시 처음 보는 계정(authServerId 미존재)을 즉시(JIT) 생성 —
 * CustomOAuth2UserService.loadUser()에서만 호출됨. 약관 동의는 AS가 이미 끝낸 뒤이므로 여기선 다루지 않음.
 * 주의: 닉네임·프로필 이미지는 AS가 준 값을 쓰지 않음 — 닉네임은 무작위로 부여 후 마이페이지에서 변경,
 *       프로필 이미지는 현재 어디에도 노출되지 않아 수집하지 않음(불필요한 개인정보 수집 최소화)
 */
@Service
@RequiredArgsConstructor
public class OAuth2AccountService {

    private final UserRepository userRepository;
    private final MbtiMetadataSupport mbtiMetadataSupport;

    @Transactional
    public User createAccount(final UUID authServerId, final String email) {
        final MbtiMetadata defaultMbti = mbtiMetadataSupport.getNoneMbti();

        final User user = User.builder()
                .email(email)
                .authServerId(authServerId)
                .nickname(generateRandomNickname())
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

    /**
     * "user" + 36진수 랜덤 문자열(8자) 닉네임 생성 — 충돌 시 재추첨
     * 주의: 보안이 걸린 값이 아니므로(예측 가능해도 무방) SecureRandom 대신 가벼운 ThreadLocalRandom 사용
     */
    private String generateRandomNickname() {
        String candidate;
        do {
            // base36 문자열은 값에 따라 길이가 가변적 — 8자 미만이면 substring이 터지므로 앞을 0으로 채운 뒤 뒤 8자만 사용
            final String padded = "00000000" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
            candidate = "user" + padded.substring(padded.length() - 8);
        } while (userRepository.existsByNickname(candidate));
        return candidate;
    }
}
