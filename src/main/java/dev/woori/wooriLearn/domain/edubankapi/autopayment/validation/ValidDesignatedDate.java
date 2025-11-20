package dev.woori.wooriLearn.domain.edubankapi.autopayment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 자동이체 지정일 검증 어노테이션
 * - 1~31일: 해당 일자에 이체 (해당 월에 없으면 말일에 실행)
 * - 99: 매월 말일에 이체 (특수값)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DesignatedDateValidator.class)
public @interface ValidDesignatedDate {
    String message() default "지정일은 1~31 또는 99(말일)여야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
