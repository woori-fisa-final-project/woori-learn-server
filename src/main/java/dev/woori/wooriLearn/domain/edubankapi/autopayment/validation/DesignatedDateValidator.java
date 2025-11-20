package dev.woori.wooriLearn.domain.edubankapi.autopayment.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 자동이체 지정일 검증 로직
 * - 1~31일: 허용
 * - 99: 말일 특수값으로 허용
 * - 그 외: 거부
 */
public class DesignatedDateValidator implements ConstraintValidator<ValidDesignatedDate, Integer> {

    private static final int END_OF_MONTH_CODE = 99;
    private static final int MIN_DATE = 1;
    private static final int MAX_DATE = 31;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // null 검증은 @NotNull이 담당
        if (value == null) {
            return true;
        }

        // 1~31 범위 또는 99(말일 코드)
        return (value >= MIN_DATE && value <= MAX_DATE) || value == END_OF_MONTH_CODE;
    }
}
