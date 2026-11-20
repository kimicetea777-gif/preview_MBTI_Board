package com.board.kimicetea.domain.user.repository;

import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * users.state 컬럼이 UserState의 모든 값을 담을 수 있는지 실제 PostgreSQL 스키마로 확인한다.
 * V1의 VARCHAR(20)에 MEMBERSHIP_WITHDRAWAL_PENDING(29자)이 들어가지 않아 CLIENT_WITHDRAWN 반영이 항상 실패했던
 * 버그(V16에서 수정)의 회귀 방지 — 핸들러 테스트는 Mockito라 컬럼 길이를 잡지 못한다.
 * 주의: 실제 DB가 필요하다 — application-test.yaml의 datasource(기본 localhost:5432).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class UserStateColumnTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("users.state 길이는 가장 긴 UserState 이름 이상이다")
    void stateColumnFitsEveryUserState() {
        final Integer columnLength = jdbc.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'state'
                """, Integer.class);
        final int longest = Arrays.stream(UserState.values()).mapToInt(state -> state.name().length()).max().orElseThrow();

        assertThat(columnLength).isGreaterThanOrEqualTo(longest);
    }

    @Test
    @DisplayName("chk_user_state가 모든 UserState 값을 허용한다")
    void checkConstraintAllowsEveryUserState() {
        final String constraint = jdbc.queryForObject("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                WHERE conrelid = 'users'::regclass AND conname = 'chk_user_state'
                """, String.class);

        assertThat(constraint).contains(Arrays.stream(UserState.values()).map(state -> "'" + state.name() + "'").toList());
    }
}
