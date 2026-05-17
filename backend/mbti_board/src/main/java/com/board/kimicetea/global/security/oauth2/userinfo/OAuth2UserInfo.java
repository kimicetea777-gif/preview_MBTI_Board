package com.board.kimicetea.global.security.oauth2.userinfo;

/**
 * Provider별 OAuth2 사용자 정보 추출 인터페이스
 * 주의: 각 Provider의 attributes 구조가 다르므로 구현체에서 Provider 규격에 맞게 파싱해야 함
 */
public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
}
