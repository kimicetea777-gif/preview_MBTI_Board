package com.board.kimicetea;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 통합 테스트 베이스 클래스.
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {
}
