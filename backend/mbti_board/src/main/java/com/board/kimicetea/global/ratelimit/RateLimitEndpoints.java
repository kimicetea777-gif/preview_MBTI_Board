package com.board.kimicetea.global.ratelimit;

/**
 * Rate Limit 키 접두사로 쓰이는 endpoint 식별자 모음
 * 주의: 같은 기능이라도 IP 기준(check)과 이메일 기준(checkByEmail)을 서로 다른 문자열로 호출하면
 *       Redis에 별도 버킷(RL:{endpoint}:...)으로 쌓여 의도한 합산 방어가 되지 않으므로 반드시 이 상수를 공유해서 사용
 */
public final class RateLimitEndpoints {

    private RateLimitEndpoints() {
    }

    public static final String NICKNAME_CHECK = "nickname-check";
}
