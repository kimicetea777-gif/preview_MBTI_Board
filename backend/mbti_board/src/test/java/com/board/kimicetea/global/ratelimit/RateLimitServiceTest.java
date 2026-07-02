package com.board.kimicetea.global.ratelimit;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "ipSalt", "test-salt");
    }

    @Nested
    @DisplayName("checkByEmail — 이메일 기반 Rate Limit")
    class CheckByEmail {

        @Test
        @DisplayName("요청 횟수가 한도 이하이면 예외를 던지지 않는다")
        void underLimit_noException() {
            when(redisTemplate.execute(any(), any(), any())).thenReturn(1L);

            assertThatNoException().isThrownBy(() ->
                    rateLimitService.checkByEmail("email-verify", "user@example.com", 5, 60));
        }

        @Test
        @DisplayName("요청 횟수가 한도를 초과하면 RATE_LIMIT_EXCEEDED 예외를 던진다")
        void overLimit_throwsRateLimitExceeded() {
            when(redisTemplate.execute(any(), any(), any())).thenReturn(6L);

            assertThatThrownBy(() ->
                    rateLimitService.checkByEmail("email-verify", "user@example.com", 5, 60))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("플러스 주소(user+tag@domain)는 기본 주소(user@domain)와 같은 Redis 키를 사용한다")
        @SuppressWarnings("unchecked")
        void plusAddress_normalizedToSameKey() {
            when(redisTemplate.execute(any(), any(), any())).thenReturn(1L);
            final ArgumentCaptor<List<String>> keyCaptor = ArgumentCaptor.forClass(List.class);

            rateLimitService.checkByEmail("email-verify", "user+tag@example.com", 5, 60);
            rateLimitService.checkByEmail("email-verify", "user@example.com", 5, 60);

            verify(redisTemplate, times(2)).execute(any(), keyCaptor.capture(), any());
            final List<List<String>> allKeys = keyCaptor.getAllValues();
            assertThat(allKeys.get(0)).isEqualTo(allKeys.get(1));
        }

        @Test
        @DisplayName("이메일은 대소문자를 구분하지 않는다")
        @SuppressWarnings("unchecked")
        void email_isCaseInsensitive() {
            when(redisTemplate.execute(any(), any(), any())).thenReturn(1L);
            final ArgumentCaptor<List<String>> keyCaptor = ArgumentCaptor.forClass(List.class);

            rateLimitService.checkByEmail("email-verify", "User@Example.COM", 5, 60);
            rateLimitService.checkByEmail("email-verify", "user@example.com", 5, 60);

            verify(redisTemplate, times(2)).execute(any(), keyCaptor.capture(), any());
            final List<List<String>> allKeys = keyCaptor.getAllValues();
            assertThat(allKeys.get(0)).isEqualTo(allKeys.get(1));
        }
    }
}
