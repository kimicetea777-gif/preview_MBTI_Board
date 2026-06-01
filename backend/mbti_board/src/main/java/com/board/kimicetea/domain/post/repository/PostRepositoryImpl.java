package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.PostConstants;
import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.QPost;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 게시글 목록 QueryDSL 구현체 — 유형·정렬·MBTI 필터 조합
 */
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findPosts(final String typeCode, final PostSortType sort,
            final String authorMbti, final String targetMbti,
            final Pageable pageable) {
        final QPost post = QPost.post;
        final BooleanBuilder builder = new BooleanBuilder();

        builder.and(post.isVisible.isTrue());

        if (typeCode != null && !typeCode.isBlank()) {
            builder.and(post.postType.code.eq(typeCode));
        }

        if (authorMbti != null && !authorMbti.isBlank()) {
            builder.and(post.authorMbti.id.eq(authorMbti));
        }

        // "ALL" 이면 전체 조회, 특정 MBTI ID면 해당 타겟만
        if (!PostConstants.TARGET_ALL.equals(targetMbti)) {
            builder.and(post.targetMbti.id.eq(targetMbti));
        }

        final OrderSpecifier<?> order = switch (sort) {
            case VIEWS    -> post.viewCount.desc();
            case COMMENTS -> post.commentCount.desc();
            case LATEST   -> post.createdAt.desc();
        };

        final List<Post> content = queryFactory
                .selectFrom(post)
                .join(post.postType).fetchJoin()
                .join(post.authorMbti).fetchJoin()
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.targetMbti).fetchJoin()
                .leftJoin(post.debatePost).fetchJoin()
                .leftJoin(post.votePost).fetchJoin()
                .where(builder)
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Post> findAllPostsForAdmin(final String typeCode, final Boolean isVisible,
            final Pageable pageable) {
        final QPost post = QPost.post;
        final BooleanBuilder builder = new BooleanBuilder();

        if (typeCode != null && !typeCode.isBlank()) {
            builder.and(post.postType.code.eq(typeCode));
        }
        if (isVisible != null) {
            builder.and(post.isVisible.eq(isVisible));
        }

        final List<Post> content = queryFactory
                .selectFrom(post)
                .join(post.postType).fetchJoin()
                .join(post.authorMbti).fetchJoin()
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.targetMbti).fetchJoin()
                .leftJoin(post.debatePost).fetchJoin()
                .leftJoin(post.votePost).fetchJoin()
                .where(builder)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Post> findMyPosts(final UUID userId, final String typeCode,
            final PostSortType sort, final Pageable pageable) {
        final QPost post = QPost.post;
        final BooleanBuilder builder = new BooleanBuilder();

        builder.and(post.user.id.eq(userId));
        builder.and(post.isVisible.isTrue());

        if (typeCode != null && !typeCode.isBlank()) {
            builder.and(post.postType.code.eq(typeCode));
        }

        final OrderSpecifier<?> order = switch (sort) {
            case VIEWS    -> post.viewCount.desc();
            case COMMENTS -> post.commentCount.desc();
            case LATEST   -> post.createdAt.desc();
        };

        final List<Post> content = queryFactory
                .selectFrom(post)
                .join(post.postType).fetchJoin()
                .join(post.authorMbti).fetchJoin()
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.targetMbti).fetchJoin()
                .leftJoin(post.debatePost).fetchJoin()
                .leftJoin(post.votePost).fetchJoin()
                .where(builder)
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
