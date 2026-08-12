package com.board.kimicetea.domain.user.repository;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByAuthServerId(UUID authServerId);

    @EntityGraph(attributePaths = { "mbti" })
    Optional<User> findWithMbtiById(UUID id);

    long countByState(UserState state);

    /** MembershipWithdrawalFinalizeService — 유예기간이 지난 MEMBERSHIP_WITHDRAWAL_PENDING 유저 조회 */
    List<User> findByStateAndMembershipWithdrawnAtBefore(UserState state, Instant cutoff);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM User u WHERE u.state = :state")
    void bulkDeleteByState(@Param("state") UserState state);

    // ── 통계 쿼리 ─────────────────────────────────────────────────────────────

    @Query(value = "SELECT state AS name, COUNT(*) AS value FROM users GROUP BY state ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByStateRaw();

    @Query(value = "SELECT role AS name, COUNT(*) AS value FROM users GROUP BY role ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByRoleRaw();

    @Query(value = "SELECT mbti_id AS name, COUNT(*) AS value FROM users GROUP BY mbti_id ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByMbtiRaw();

    @Query(value = "SELECT mbti_verified AS name, COUNT(*) AS value FROM users GROUP BY mbti_verified ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByMbtiVerifiedRaw();

    /** state가 NORMAL이면서 마지막 로그인이 :cutoff 이전이거나 없는 유저 수 */
    @Query("SELECT COUNT(u) FROM User u WHERE u.state = :state AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoff)")
    long countByStateAndLastLoginAtBefore(@Param("state") UserState state, @Param("cutoff") Instant cutoff);

    /** 최근 :since 이후 일별 ADMIN·TESTER 신규 생성 수 */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', created_at), 'MM-DD')          AS date,
                   COUNT(*) FILTER (WHERE role = 'ADMIN')                    AS admin_count,
                   COUNT(*) FILTER (WHERE role = 'TESTER')                   AS tester_count
            FROM users
            WHERE created_at >= :since AND role IN ('ADMIN', 'TESTER')
            GROUP BY DATE_TRUNC('day', created_at)
            ORDER BY DATE_TRUNC('day', created_at)
            """, nativeQuery = true)
    List<Object[]> findRoleTrendRaw(@Param("since") Instant since);

    /** :from 이상 :to 미만 구간의 신규 가입 수 */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :from AND u.createdAt < :to")
    long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** state가 :state이면서 가입일이 :cutoff 이전인 유저 수 — PENDING 장기 체류 감지용 */
    @Query("SELECT COUNT(u) FROM User u WHERE u.state = :state AND u.createdAt < :cutoff")
    long countByStateAndCreatedAtBefore(@Param("state") UserState state, @Param("cutoff") Instant cutoff);

    /** 최근 :since 이후 일별 가입 수 — MM-DD 형식 날짜 반환 */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', created_at), 'MM-DD') AS date,
                   COUNT(*)                                         AS value
            FROM users
            WHERE created_at >= :since
            GROUP BY DATE_TRUNC('day', created_at)
            ORDER BY DATE_TRUNC('day', created_at)
            """, nativeQuery = true)
    List<Object[]> findSignupTrendRaw(@Param("since") Instant since);
}
