package dev.woori.wooriLearn.domain.auth.dto.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UserIdValidator implements ConstraintValidator<ValidUserId, String> {
    // 5~20자 / 영문자 시작 / 영문 하나 이상 / 숫자 하나 이상 / 영문&숫자 조합으로만
    private static final String USER_ID_REGEX =
            "^(?=.{5,20}$)(?=[a-z])(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9]+$";

    private final Pattern pattern = Pattern.compile(USER_ID_REGEX);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return pattern.matcher(value).matches();
    }
}
