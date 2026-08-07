package com.board.kimicetea.global.security.oauth2;

/**
 * AS(Authorization Server)와의 통신 자체가 실패했을 때(네트워크/타임아웃 등) 던진다.
 * AS가 명시적으로 거부한 것과 달리 세션이 잘못된 게 아니므로, 호출부는 세션을 지우지 말고
 * 이번 요청만 실패시켜야 한다(RedisConnectionFailureException과 동일한 취급).
 */
public class AuthorizedClientRefreshUnavailableException extends RuntimeException {

    public AuthorizedClientRefreshUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
