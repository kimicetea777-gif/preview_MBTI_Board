package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.board.kimicetea.domain.post.enums.PostImageStatus.CONFIRMED;
import static com.board.kimicetea.domain.post.enums.PostImageStatus.CONFIRM_FAILED;
import static com.board.kimicetea.domain.post.enums.PostImageStatus.PENDING_CONFIRM;
import static com.board.kimicetea.domain.post.enums.PostImageStatus.PENDING_RELEASE;

/**
 * post_images 접근 — 상태 변경은 전부 "WHERE status = 기대 상태" 조건부다. 워커가 Image Server 응답을
 * 기다리는 사이 사용자가 같은 이미지의 상태를 바꿨으면 그 행은 조건에 맞지 않아 갱신에서 빠진다.
 * 주의: @Modifying 쿼리는 기본 트랜잭션이 없어 메서드에 @Transactional을 붙인다 — PostService처럼 바깥
 *       트랜잭션이 있으면 거기에 합류하고, 워커처럼 없으면 문장 단위로 짧게 커밋된다.
 * 주의: imageIds가 빈 컬렉션이면 IN () 이 되어 SQL 오류 — 호출 측에서 비어 있으면 부르지 않는다.
 * 주의: 상태 이름은 SQL에 적지 않고 PostImageStatus에서 .name()으로 만들어 파라미터로 넘긴다(enum이 바뀌어도 따라간다).
 *       그래서 쿼리마다 "파라미터를 받는 쿼리 메서드"와 "상태를 채워 넣는 기본 메서드" 쌍이 있다.
 *       파라미터를 받는 메서드는 직접 부르지 말고 짝이 되는 기본 메서드를 쓴다 — 상태를 잘못 넘기는 실수를 막는다.
 */
public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

    /**
     * 아직 어느 글도 소유하지 않은 이미지면 이 글 소유로 등록한다(상태는 DB 기본값 PENDING_CONFIRM).
     * 반환값 1 = 이번에 소유자가 됨, 0 = 이미 다른 글(또는 이 글)이 소유 중
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO post_images (image_id, post_id) VALUES (:imageId, :postId)
            ON CONFLICT (image_id) DO NOTHING
            """, nativeQuery = true)
    int claim(@Param("imageId") UUID imageId, @Param("postId") Long postId);

    List<PostImage> findByPostId(Long postId);

    /** 글에서 빠진(또는 글 삭제) 이미지를 정리 대상으로 표시 — 이미 PENDING_RELEASE인 행은 건드리지 않음 */
    default int markPendingRelease(final Long postId, final Collection<UUID> imageIds) {
        return setStatusUnlessAlready(postId, imageIds, PENDING_RELEASE.name());
    }

    /** 뺐다가 다시 넣은 이미지(아직 정리 전)를 확정 대상으로 되돌림 */
    default int reactivate(final Long postId, final Collection<UUID> imageIds) {
        return moveStatusAndResetRetry(postId, imageIds, PENDING_RELEASE.name(), PENDING_CONFIRM.name());
    }

    /** 워커가 처리할 차례인 행 — idx_post_images_due 부분 인덱스(V14, 같은 두 상태)를 탄다 */
    default List<PostImage> findDue(final Instant now, final int limit) {
        return findDueInStatuses(now, List.of(PENDING_CONFIRM.name(), PENDING_RELEASE.name()), limit);
    }

    default int markConfirmed(final Collection<UUID> imageIds) {
        return moveStatusAndResetFailCount(imageIds, PENDING_CONFIRM.name(), CONFIRMED.name());
    }

    default int markConfirmFailed(final Collection<UUID> imageIds) {
        return moveStatus(imageIds, PENDING_CONFIRM.name(), CONFIRM_FAILED.name());
    }

    /** release가 끝난 행 삭제 — 그사이 다시 붙은(PENDING_CONFIRM) 행은 남긴다 */
    default int deleteIfPendingRelease(final Collection<UUID> imageIds) {
        return deleteInStatus(imageIds, PENDING_RELEASE.name());
    }

    /** 실패 횟수를 1 올리고 다음 시도 시각을 미룸 — 호출 측이 실패 횟수가 같은 행끼리 묶어 부른다 */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE post_images
            SET fail_count = fail_count + 1, next_attempt_at = :nextAttemptAt, updated_at = NOW()
            WHERE image_id IN (:imageIds) AND status = :expectedStatus
            """, nativeQuery = true)
    int scheduleRetry(@Param("imageIds") Collection<UUID> imageIds,
            @Param("expectedStatus") String expectedStatus,
            @Param("nextAttemptAt") Instant nextAttemptAt);

    // ── 아래는 위 기본 메서드가 쓰는 쿼리 메서드 — 직접 부르지 않는다 (상태 이름 문자열을 호출 측이 채우게 되기 때문) ──

    /** [markPendingRelease용] 이미 status인 행은 제외하고 status로 바꾸며 재시도 상태를 초기화 */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE post_images
            SET status = :status, fail_count = 0, next_attempt_at = NOW(), updated_at = NOW()
            WHERE post_id = :postId AND image_id IN (:imageIds) AND status <> :status
            """, nativeQuery = true)
    int setStatusUnlessAlready(@Param("postId") Long postId, @Param("imageIds") Collection<UUID> imageIds,
            @Param("status") String status);

    /** [reactivate용] from 상태인 행만 to로 바꾸며 재시도 상태를 초기화 */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE post_images
            SET status = :to, fail_count = 0, next_attempt_at = NOW(), updated_at = NOW()
            WHERE post_id = :postId AND image_id IN (:imageIds) AND status = :from
            """, nativeQuery = true)
    int moveStatusAndResetRetry(@Param("postId") Long postId, @Param("imageIds") Collection<UUID> imageIds,
            @Param("from") String from, @Param("to") String to);

    /** [findDue용] 상태가 statuses 중 하나이고 시도할 차례가 된 행 */
    @Query(value = """
            SELECT * FROM post_images
            WHERE status IN (:statuses) AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            """, nativeQuery = true)
    List<PostImage> findDueInStatuses(@Param("now") Instant now, @Param("statuses") Collection<String> statuses,
            @Param("limit") int limit);

    /** [markConfirmed용] from 상태인 행만 to로 바꾸고 실패 횟수를 0으로 */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE post_images SET status = :to, fail_count = 0, updated_at = NOW()
            WHERE image_id IN (:imageIds) AND status = :from
            """, nativeQuery = true)
    int moveStatusAndResetFailCount(@Param("imageIds") Collection<UUID> imageIds,
            @Param("from") String from, @Param("to") String to);

    /** [markConfirmFailed용] from 상태인 행만 to로 바꿈 (실패 횟수는 그대로) */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE post_images SET status = :to, updated_at = NOW()
            WHERE image_id IN (:imageIds) AND status = :from
            """, nativeQuery = true)
    int moveStatus(@Param("imageIds") Collection<UUID> imageIds,
            @Param("from") String from, @Param("to") String to);

    /** [deleteIfPendingRelease용] status인 행만 삭제 */
    @Transactional
    @Modifying
    @Query(value = """
            DELETE FROM post_images WHERE image_id IN (:imageIds) AND status = :status
            """, nativeQuery = true)
    int deleteInStatus(@Param("imageIds") Collection<UUID> imageIds, @Param("status") String status);
}
