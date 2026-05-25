package com.board.kimicetea;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;

/**
 * 통합 테스트 베이스 클래스.
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 * 주의: ConcurrentMapCacheManager 는 JVM 공유 — @AfterEach 에서 캐시를 초기화하지 않으면
 *       이전 테스트에서 캐시된 detached 엔티티가 다음 트랜잭션에서 사용돼 500 발생.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    private CacheManager cacheManager;

    /** 각 테스트 후 Spring 인메모리 캐시 전체 초기화 — detached 엔티티 재사용 방지 */
    @AfterEach
    void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    // Auth
    protected static final String SIGNUP_URL              = "/api/auth/signup";
    protected static final String LOGIN_URL               = "/api/auth/login";
    protected static final String LOGOUT_URL              = "/api/auth/logout";
    protected static final String REISSUE_URL             = "/api/auth/reissue";
    protected static final String EMAIL_VERIFY_SEND_URL   = "/api/auth/email-verify/send";
    protected static final String EMAIL_VERIFY_CONFIRM_URL = "/api/auth/email-verify/confirm";

    // User
    protected static final String ME_URL               = "/api/users/me";
    protected static final String WITHDRAWAL_VERIFY_URL = "/api/users/me/withdrawal-verify";
    protected static final String PASSWORD_URL         = "/api/users/me/password";
    protected static final String USER_URL             = "/api/users/";

    // Post
    protected static final String POSTS_URL = "/api/posts";

    // Comment reactions — use as: REACTIONS_BASE + commentId + "/reactions"
    protected static final String REACTIONS_BASE = "/api/comments/";
}
