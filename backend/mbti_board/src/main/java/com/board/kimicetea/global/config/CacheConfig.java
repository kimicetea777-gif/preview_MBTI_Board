package com.board.kimicetea.global.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인메모리 캐시 설정 — 변경이 없는 참조 테이블(postTypes 등) 전용
 */
@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("postTypes", "mbtiList");
    }
}
