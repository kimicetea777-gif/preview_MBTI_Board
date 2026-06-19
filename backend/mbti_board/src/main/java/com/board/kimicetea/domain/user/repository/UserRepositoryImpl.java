package com.board.kimicetea.domain.user.repository;

import com.board.kimicetea.domain.user.entity.QUser;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserSortField;
import com.board.kimicetea.domain.user.enums.UserState;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 유저 목록 QueryDSL 구현체 — 이메일·상태·역할 필터 조합
 * 주의: mbti fetchJoin으로 N+1 방지
 */
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<User> searchUsers(final String email, final UserState state,
            final UserRole role, final UserSortField sortBy, final Sort.Direction direction,
            final Pageable pageable) {
        final QUser user = QUser.user;
        final BooleanBuilder builder = new BooleanBuilder();

        if (email != null && !email.isBlank()) {
            builder.and(user.email.containsIgnoreCase(email));
        }
        if (state != null) {
            builder.and(user.state.eq(state));
        }
        if (role != null) {
            builder.and(user.role.eq(role));
        }

        final List<User> content = queryFactory
                .selectFrom(user)
                .join(user.mbti).fetchJoin()
                .where(builder)
                .orderBy(toOrderSpecifier(user, sortBy, direction))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Long total = queryFactory
                .select(user.count())
                .from(user)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?> toOrderSpecifier(final QUser user, final UserSortField sortBy,
            final Sort.Direction direction) {
        final ComparableExpressionBase<?> field = switch (sortBy) {
            case UPDATED_AT -> user.updatedAt;
            case LAST_LOGIN_AT -> user.lastLoginAt;
            default -> user.createdAt;
        };
        // NULL 컬럼(lastLoginAt 등)이 항상 마지막에 위치하도록 명시 (PostgreSQL 기본은 DESC → NULLS FIRST)
        return direction == Sort.Direction.ASC ? field.asc().nullsLast() : field.desc().nullsLast();
    }
}
