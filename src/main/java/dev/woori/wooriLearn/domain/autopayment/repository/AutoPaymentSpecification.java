package dev.woori.wooriLearn.domain.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class AutoPaymentSpecification {

    public static Specification<AutoPayment> hasEducationalAccountId(Long educationalAccountId) {
        return (root, query, criteriaBuilder) -> {
            // N+1 방지를 위한 fetch join
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("educationalAccount", JoinType.LEFT);
            }
            return criteriaBuilder.equal(root.get("educationalAccount").get("id"), educationalAccountId);
        };
    }

    public static Specification<AutoPayment> hasStatus(AutoPaymentStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("processingStatus"), status);
    }

    // 나중에 추가할 조건들
    public static Specification<AutoPayment> amountGreaterThan(Integer amount) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get("amount"), amount);
    }

    public static Specification<AutoPayment> transferDayBetween(Integer startDay, Integer endDay) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("designatedDate"), startDay, endDay);
    }
}