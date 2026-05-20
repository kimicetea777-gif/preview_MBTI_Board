package com.board.kimicetea.domain.post.repository;

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

/**
 * 게시글 목록 QueryDSL 구현체 — 유형·정렬·MBTI 필터 조합
 */
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findPosts(final String typeCode, final String sort,
                                final String authorMbti, final Pageable pageable) {
        final QPost post = QPost.post;
        final BooleanBuilder builder = new BooleanBuilder();

        builder.and(post.postType.code.eq(typeCode));
        builder.and(post.isVisible.isTrue());

        if (authorMbti != null && !authorMbti.isBlank()) {
            builder.and(post.authorMbti.id.eq(authorMbti));
        }

        final OrderSpecifier<?> order = switch (sort) {
            case "comments" -> post.commentCount.desc();
            case "views"    -> post.viewCount.desc();
            default         -> post.createdAt.desc();
        };

        final List<Post> content = queryFactory
                .selectFrom(post)
                .join(post.postType).fetchJoin()
                .join(post.authorMbti).fetchJoin()
                .leftJoin(post.user).fetchJoin()
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
