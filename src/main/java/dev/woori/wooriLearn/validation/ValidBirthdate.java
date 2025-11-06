package dev.woori.wooriLearn.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BirthdateValidator.class)
public @interface ValidBirthdate {
    String message() default "유효하지 않은 생년월일입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /** 미래 날짜 허용 여부 */
    boolean allowFuture() default false;

    /** 최소 나이(년). 0이면 제한 없음 */
    int minAgeYears() default 0;
}
