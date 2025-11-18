package dev.woori.wooriLearn.domain.account.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static dev.woori.wooriLearn.domain.account.entity.QPointsHistory.pointsHistory;
import static dev.woori.wooriLearn.domain.user.entity.QUsers.users;

@Repository
@RequiredArgsConstructor
public class PointsHistoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<PointsHistory> findAllByFilters(
            Long userId,
            PointsHistoryType type,
            PointsStatus status,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {

        var query = queryFactory
                .select(pointsHistory)
                .from(pointsHistory)
                .join(pointsHistory.user, users).fetchJoin()
                .where(
                        typeEq(type),
                        userIdEq(userId),
                        statusEq(status),
                        startGoe(start),
                        endLoe(end)
                );

        var countQuery = queryFactory
                .select(pointsHistory.count())
                .from(pointsHistory)
                .where(
                        typeEq(type),
                        userIdEq(userId),
                        statusEq(status),
                        startGoe(start),
                        endLoe(end)
                );

        Sort sort = pageable.getSort();

        var orderSpecifiers = toOrderSpecifiers(sort);

        List<PointsHistory> content = query
                .orderBy(orderSpecifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression typeEq(PointsHistoryType type) {
        return type == null ? null : pointsHistory.type.eq(type);
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : pointsHistory.user.id.eq(userId);
    }

    private BooleanExpression statusEq(PointsStatus status) {
        return status == null ? null : pointsHistory.status.eq(status);
    }

    private BooleanExpression startGoe(LocalDateTime start) {
        return start == null ? null : pointsHistory.createdAt.goe(start);
    }

    private BooleanExpression endLoe(LocalDateTime end) {
        return end == null ? null : pointsHistory.createdAt.loe(end);
    }

    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier[]{pointsHistory.createdAt.desc()};
        }

        java.util.List<OrderSpecifier<?>> specs = new java.util.ArrayList<>();
        for (Sort.Order o : sort) {
            Order direction = o.isAscending() ? Order.ASC : Order.DESC;
            String property = o.getProperty();
            if (property == null) continue;

            switch (property) {
                case "createdAt":
                    specs.add(new OrderSpecifier<>(direction, pointsHistory.createdAt));
                    break;
                case "updatedAt":
                    specs.add(new OrderSpecifier<>(direction, pointsHistory.updatedAt));
                    break;
                case "id":
                    specs.add(new OrderSpecifier<>(direction, pointsHistory.id));
                    break;
                case "amount":
                    specs.add(new OrderSpecifier<>(direction, pointsHistory.amount));
                    break;
                case "processedAt":
                    specs.add(new OrderSpecifier<>(direction, pointsHistory.processedAt));
                    break;
                case "type":
                    specs.add(new OrderSpecifier<>(direction, pointsHistory.type));
                    break;
                case "status":
                    specs.add(new OrderSpecifier<>(direction, pointsHistory.status));
                    break;
                default:
                    // Unknown property: ignore to avoid errors
            }
        }

        if (specs.isEmpty()) {
            return new OrderSpecifier[]{pointsHistory.createdAt.desc()};
        }
        return specs.toArray(new OrderSpecifier<?>[0]);
    }
}
