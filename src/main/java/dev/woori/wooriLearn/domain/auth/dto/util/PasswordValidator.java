package dev.woori.wooriLearn.domain.auth.dto.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    // 8~20자 / 영문자 1개 이상 / 숫자 1개 이상 / 특수문자 1개 이상
    private static final String PASSWORD_REGEX =
            "^(?=.{8,20}$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]|\\\\:;\"'<>,.?/]).+$";

    private static final Pattern PATTERN = Pattern.compile(PASSWORD_REGEX);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return PATTERN.matcher(value).matches();
    }

}
