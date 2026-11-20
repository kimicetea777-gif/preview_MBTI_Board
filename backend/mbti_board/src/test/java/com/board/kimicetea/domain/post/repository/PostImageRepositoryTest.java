package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.PostImage;
import com.board.kimicetea.domain.post.enums.PostImageStatus;
import com.board.kimicetea.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * post_images 조건부 갱신·소유 등록·FK/CHECK 제약을 실제 PostgreSQL에서 확인한다(V14 마이그레이션 포함).
 * 주의: 실제 DB가 필요하다 — application-test.yaml의 datasource(기본 localhost:5432). 각 테스트는 롤백된다.
 * 상태 확인은 영속성 컨텍스트 캐시를 피하려고 JdbcTemplate으로 직접 읽는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class) // QueryDSL JPAQueryFactory — 다른 커스텀 리포지토리(UserRepositoryImpl 등)가 슬라이스에서도 생성되도록
class PostImageRepositoryTest {

    @Autowired private PostImageRepository postImageRepository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("claim: 먼저 등록한 글이 소유하고, 다른 글의 등록은 무시된다")
    void claimFirstPostWins() {
        final long original = insertPost();
        final long other = insertPost();
        final UUID image = UUID.randomUUID();

        assertThat(postImageRepository.claim(image, original)).isEqualTo(1);
        assertThat(postImageRepository.claim(image, other)).isZero();

        assertThat(ownerOf(image)).isEqualTo(original);
        assertThat(statusOf(image)).isEqualTo("PENDING_CONFIRM");
    }

    @Test
    @DisplayName("findByPostId: 상태가 enum으로 매핑된다")
    void findByPostIdMapsStatus() {
        final long post = insertPost();
        final UUID image = UUID.randomUUID();
        insertImage(image, post, "CONFIRM_FAILED", 2, Instant.now());

        final List<PostImage> rows = postImageRepository.findByPostId(post);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getImageId()).isEqualTo(image);
            assertThat(row.getStatus()).isEqualTo(PostImageStatus.CONFIRM_FAILED);
            assertThat(row.getFailCount()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("markPendingRelease: 그 글의 지정한 행만 바꾸고 실패 횟수를 초기화한다")
    void markPendingReleaseOnlyThatPost() {
        final long post = insertPost();
        final long other = insertPost();
        final UUID mine = UUID.randomUUID();
        final UUID othersImage = UUID.randomUUID();
        insertImage(mine, post, "CONFIRMED", 3, Instant.now());
        insertImage(othersImage, other, "CONFIRMED", 0, Instant.now());

        final int updated = postImageRepository.markPendingRelease(post, List.of(mine, othersImage));

        assertThat(updated).isEqualTo(1);
        assertThat(statusOf(mine)).isEqualTo("PENDING_RELEASE");
        assertThat(failCountOf(mine)).isZero();
        assertThat(statusOf(othersImage)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("reactivate: 정리 대기(PENDING_RELEASE)인 행만 확정 대상으로 되돌린다")
    void reactivateOnlyPendingRelease() {
        final long post = insertPost();
        final UUID releasing = UUID.randomUUID();
        final UUID failed = UUID.randomUUID();
        insertImage(releasing, post, "PENDING_RELEASE", 4, Instant.now());
        insertImage(failed, post, "CONFIRM_FAILED", 0, Instant.now());

        final int updated = postImageRepository.reactivate(post, List.of(releasing, failed));

        assertThat(updated).isEqualTo(1);
        assertThat(statusOf(releasing)).isEqualTo("PENDING_CONFIRM");
        assertThat(failCountOf(releasing)).isZero();
        assertThat(statusOf(failed)).isEqualTo("CONFIRM_FAILED");
    }

    @Test
    @DisplayName("markConfirmed/markConfirmFailed: 묶음에서 그사이 상태가 바뀐 행은 빠진다")
    void confirmUpdatesAreConditional() {
        final long post = insertPost();
        final UUID pending = UUID.randomUUID();
        final UUID removedMeanwhile = UUID.randomUUID();
        final UUID pending2 = UUID.randomUUID();
        insertImage(pending, post, "PENDING_CONFIRM", 1, Instant.now());
        insertImage(removedMeanwhile, post, "PENDING_RELEASE", 0, Instant.now());
        insertImage(pending2, post, "PENDING_CONFIRM", 0, Instant.now());

        assertThat(postImageRepository.markConfirmed(List.of(pending, removedMeanwhile))).isEqualTo(1);
        assertThat(postImageRepository.markConfirmFailed(List.of(pending2, removedMeanwhile))).isEqualTo(1);

        assertThat(statusOf(pending)).isEqualTo("CONFIRMED");
        assertThat(failCountOf(pending)).isZero();
        assertThat(statusOf(pending2)).isEqualTo("CONFIRM_FAILED");
        assertThat(statusOf(removedMeanwhile)).isEqualTo("PENDING_RELEASE");
    }

    @Test
    @DisplayName("scheduleRetry: 기대 상태인 행만 실패 횟수 +1, 다음 시도 시각 변경")
    void scheduleRetryIsConditional() {
        final long post = insertPost();
        final UUID pending = UUID.randomUUID();
        final UUID changed = UUID.randomUUID();
        insertImage(pending, post, "PENDING_CONFIRM", 2, Instant.now());
        insertImage(changed, post, "PENDING_RELEASE", 2, Instant.now());
        final Instant next = Instant.now().plus(Duration.ofMinutes(5));

        final int updated = postImageRepository.scheduleRetry(List.of(pending, changed), "PENDING_CONFIRM", next);

        assertThat(updated).isEqualTo(1);
        assertThat(failCountOf(pending)).isEqualTo(3);
        assertThat(nextAttemptOf(pending)).isCloseTo(next, within1s());
        assertThat(failCountOf(changed)).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteIfPendingRelease: 그사이 다시 붙은(PENDING_CONFIRM) 행은 남긴다")
    void deleteKeepsReattached() {
        final long post = insertPost();
        final UUID releasing = UUID.randomUUID();
        final UUID reattached = UUID.randomUUID();
        insertImage(releasing, post, "PENDING_RELEASE", 0, Instant.now());
        insertImage(reattached, post, "PENDING_CONFIRM", 0, Instant.now());

        assertThat(postImageRepository.deleteIfPendingRelease(List.of(releasing, reattached))).isEqualTo(1);

        assertThat(exists(releasing)).isFalse();
        assertThat(statusOf(reattached)).isEqualTo("PENDING_CONFIRM");
    }

    @Test
    @DisplayName("findDue: 처리 대상 상태이면서 시각이 된 행만, 오래된 순으로")
    void findDueReturnsOnlyDueRowsInOrder() {
        jdbc.update("DELETE FROM post_images"); // 마이그레이션 백필 행 제외 (테스트 끝나면 롤백)
        final long post = insertPost();
        final Instant now = Instant.now();
        final UUID older = UUID.randomUUID();
        final UUID newer = UUID.randomUUID();
        final UUID future = UUID.randomUUID();
        final UUID confirmed = UUID.randomUUID();
        insertImage(newer, post, "PENDING_RELEASE", 0, now.minusSeconds(10));
        insertImage(older, post, "PENDING_CONFIRM", 0, now.minusSeconds(60));
        insertImage(future, post, "PENDING_CONFIRM", 1, now.plusSeconds(600));
        insertImage(confirmed, post, "CONFIRMED", 0, now.minusSeconds(600));

        final List<PostImage> due = postImageRepository.findDue(now, 100);

        assertThat(due).extracting(PostImage::getImageId).containsExactly(older, newer);
    }

    @Test
    @DisplayName("글 영구 삭제: 정리 대기 행은 post_id가 NULL이 되어 남는다(워커가 release)")
    void deletingPostKeepsPendingReleaseRow() {
        final long post = insertPost();
        final UUID image = UUID.randomUUID();
        insertImage(image, post, "PENDING_RELEASE", 0, Instant.now());

        jdbc.update("DELETE FROM posts WHERE id = ?", post);

        assertThat(exists(image)).isTrue();
        assertThat(jdbc.queryForObject("SELECT post_id FROM post_images WHERE image_id = ?", Long.class, image)).isNull();
    }

    @Test
    @DisplayName("글 영구 삭제: 정리 표시 없이 붙어 있는 이미지가 있으면 CHECK 제약이 삭제를 막는다")
    void deletingPostWithAttachedImageIsRejected() {
        final long post = insertPost();
        insertImage(UUID.randomUUID(), post, "CONFIRMED", 0, Instant.now());

        assertThatThrownBy(() -> jdbc.update("DELETE FROM posts WHERE id = ?", post))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_post_images_orphan");
    }

    // ------------------------------------------------------------------------------------------

    private long insertPost() {
        return jdbc.queryForObject("""
                INSERT INTO posts (author_mbti, post_type, title, content, guest_password)
                VALUES ('NONE', 'FREE', 'post-images-test', '{}'::jsonb, 'test-hash')
                RETURNING id
                """, Long.class);
    }

    private void insertImage(final UUID imageId, final long postId, final String status,
            final int failCount, final Instant nextAttemptAt) {
        jdbc.update("""
                INSERT INTO post_images (image_id, post_id, status, fail_count, next_attempt_at)
                VALUES (?, ?, ?, ?, ?)
                """, imageId, postId, status, failCount, Timestamp.from(nextAttemptAt));
    }

    private String statusOf(final UUID imageId) {
        return jdbc.queryForObject("SELECT status FROM post_images WHERE image_id = ?", String.class, imageId);
    }

    private int failCountOf(final UUID imageId) {
        return jdbc.queryForObject("SELECT fail_count FROM post_images WHERE image_id = ?", Integer.class, imageId);
    }

    private Instant nextAttemptOf(final UUID imageId) {
        return jdbc.queryForObject("SELECT next_attempt_at FROM post_images WHERE image_id = ?", Timestamp.class, imageId)
                .toInstant();
    }

    private Long ownerOf(final UUID imageId) {
        return jdbc.queryForObject("SELECT post_id FROM post_images WHERE image_id = ?", Long.class, imageId);
    }

    private boolean exists(final UUID imageId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM post_images WHERE image_id = ?)", Boolean.class, imageId));
    }

    private static org.assertj.core.data.TemporalUnitOffset within1s() {
        return org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS);
    }
}
