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
    protected static final String SIGNUP_URL               = "/api/auth/signup";
    protected static final String LOGIN_URL                = "/api/auth/login";
    protected static final String LOGOUT_URL               = "/api/auth/logout";
    protected static final String REISSUE_URL              = "/api/auth/reissue";
    protected static final String EMAIL_VERIFY_SEND_URL    = "/api/auth/email-verify/send";
    protected static final String EMAIL_VERIFY_CONFIRM_URL = "/api/auth/email-verify/confirm";
    protected static final String FIND_EMAIL_URL           = "/api/auth/find-email";
    protected static final String PW_RESET_REQUEST_URL     = "/api/auth/password-reset/request";
    protected static final String PW_RESET_CONFIRM_URL     = "/api/auth/password-reset/confirm";

    // User
    protected static final String ME_URL               = "/api/users/me";
    protected static final String MY_POSTS_URL         = "/api/users/me/posts";
    protected static final String WITHDRAWAL_VERIFY_URL = "/api/users/me/withdrawal-verify";
    protected static final String PASSWORD_URL         = "/api/users/me/password";
    protected static final String USER_URL             = "/api/users/";

    // Admin - User
    protected static final String ADMIN_USERS_URL       = "/api/admin/users";
    protected static final String ADMIN_USER_STATE_URL  = "/api/admin/users/%s/state";
    protected static final String ADMIN_USER_ROLE_URL   = "/api/admin/users/%s/role";

    // Admin - Post
    protected static final String ADMIN_POSTS_URL              = "/api/admin/posts";
    protected static final String ADMIN_POST_VISIBILITY_URL    = "/api/admin/posts/%d/visibility";

    // Admin - Comment
    protected static final String ADMIN_COMMENTS_URL           = "/api/admin/comments";
    protected static final String ADMIN_COMMENT_VISIBILITY_URL = "/api/admin/comments/%d/visibility";

    // Post
    protected static final String POSTS_URL        = "/api/posts";
    protected static final String VOTE_POSTS_URL   = "/api/posts/vote";
    protected static final String DEBATE_POSTS_URL = "/api/posts/debate";

    // Comment reactions — use as: REACTIONS_BASE + commentId + "/reactions"
    protected static final String REACTIONS_BASE = "/api/comments/";

    // Image
    protected static final String IMAGE_PENDING_URL = "/api/images/pending";
    protected static final String IMAGE_CANCEL_URL  = "/api/images";

    // MBTI 검사 / 메타데이터
    protected static final String MBTI_TEST_QUESTIONS_URL = "/api/mbti/test/questions";
    protected static final String MBTI_SUBMIT_URL          = "/api/mbti/test/submit";
    protected static final String MBTI_LIST_URL            = "/api/mbti";
    protected static final String MBTI_HISTORY_URL         = "/api/users/me/mbti/history";
}
