package dev.woori.wooriLearn.domain.autopayment.repository;

import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment_;
import dev.woori.wooriLearn.domain.edubankapi.entity.AutoPayment.AutoPaymentStatus;
import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount_;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class AutoPaymentSpecification {

    public static Specification<AutoPayment> hasEducationalAccountId(Long educationalAccountId) {
        return (root, query, criteriaBuilder) -> {
            // N+1 방지를 위한 fetch join (count 쿼리에서는 제외)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch(AutoPayment_.educationalAccount, JoinType.INNER);
            }
            return criteriaBuilder.equal(
                    root.get(AutoPayment_.educationalAccount).get(EducationalAccount_.id),
                    educationalAccountId
            );
        };
    }

    public static Specification<AutoPayment> hasStatus(AutoPaymentStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(AutoPayment_.processingStatus), status);
    }
}