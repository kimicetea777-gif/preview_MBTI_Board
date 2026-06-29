package com.board.kimicetea.domain.comment.repository;

import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.comment.entity.QComment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 목록 QueryDSL 구현체 — postId·visibility·authorNickname 필터, sortDir 정렬 (어드민 전용)
 */
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Comment> findAllCommentsForAdmin(final Long postId, final Boolean isVisible,
            final String authorNickname, final String sortDir, final Pageable pageable) {
        final QComment comment = QComment.comment;
        final BooleanBuilder builder = new BooleanBuilder();

        if (postId != null) {
            builder.and(comment.post.id.eq(postId));
        }
        if (isVisible != null) {
            builder.and(comment.isVisible.eq(isVisible));
        }
        if (authorNickname != null && !authorNickname.isBlank()) {
            builder.and(comment.user.nickname.containsIgnoreCase(authorNickname));
        }

        final OrderSpecifier<?> order = "ASC".equalsIgnoreCase(sortDir)
                ? comment.createdAt.asc()
                : comment.createdAt.desc();

        final List<Comment> content = new ArrayList<>(queryFactory
                .selectFrom(comment)
                .join(comment.authorMbti).fetchJoin()
                .join(comment.post).fetchJoin()
                .leftJoin(comment.user).fetchJoin()
                .leftJoin(comment.parent).fetchJoin()
                .where(builder)
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch());

        final Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
