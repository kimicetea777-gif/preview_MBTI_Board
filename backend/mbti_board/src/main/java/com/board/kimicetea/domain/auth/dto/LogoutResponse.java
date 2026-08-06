package com.board.kimicetea.domain.auth.dto;

/**
 * authServerLogoutUrl이 있으면 프론트가 그 주소로 이동시켜 AS 세션(RP-Initiated Logout)까지 끝내야 한다.
 * null이면 AS ID Token을 못 찾은 것(예: 소셜 로그인 이력이 없는 계정)이라 로컬 로그아웃만으로 끝.
 */
public record LogoutResponse(
        String authServerLogoutUrl
) {}
