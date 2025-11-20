package dev.woori.wooriLearn.domain.auth.dto.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UserIdValidator implements ConstraintValidator<ValidUserId, String> {
    // 5~20자 / 영문자 소문자로 시작 / 영문과 숫자로만 구성 / 숫자 최소 1개 포함
    private static final String USER_ID_REGEX =
            "^(?=[a-z])(?=.*\\d)[a-zA-Z0-9]{5,20}$";

    private static final Pattern PATTERN = Pattern.compile(USER_ID_REGEX);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return PATTERN.matcher(value).matches();
    }
}
