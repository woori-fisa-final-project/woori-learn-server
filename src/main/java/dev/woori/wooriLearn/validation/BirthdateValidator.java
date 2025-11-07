package dev.woori.wooriLearn.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ValidBirthdate 로 붙은 문자열 필드에 대해
 * - 형식: "YYMMDDG" (G = 1~4)
 * - 존재하는 날짜 여부
 * - 미래 날짜 허용 여부
 * - 최소/최대 나이 제한
 * 을 검증하는 커스텀 Bean Validation 검증기.
 */
public class BirthdateValidator implements ConstraintValidator<ValidBirthdate, String> {

    // YYMMDDG 패턴: (YY)(MM)(DD)(성별코드 1~4)
    private static final Pattern P = Pattern.compile("^(\\d{2})(\\d{2})(\\d{2})([1-4])$");
    private boolean allowFuture;
    private int minAgeYears;

    @Override
    public void initialize(ValidBirthdate ann) {
        this.allowFuture = ann.allowFuture();
        this.minAgeYears = ann.minAgeYears();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // @NotBlank가 따로 있으니 null/blank는 여기서 true로 두고 NotBlank에 맡겨도 됨
        if (value == null || value.isBlank()) return true;

        // 형식(정규식) 검사: YYMMDDG
        Matcher m = P.matcher(value);
        if (!m.matches()) return false;

        int yy = Integer.parseInt(m.group(1));
        int mm = Integer.parseInt(m.group(2));
        int dd = Integer.parseInt(m.group(3));
        int g  = Integer.parseInt(m.group(4)); // 1~4

        // 성별코드 기준 세기 판정: 1,2=1900~1999 / 3,4=2000~2099
        int century = (g == 1 || g == 2) ? 1900 : 2000;
        int year = century + yy;

        // 실제 존재하는 날짜인지 확인
        final LocalDate birth;
        try {
            birth = LocalDate.of(year, mm, dd);
        } catch (DateTimeException e) {
            return false;
        }

        // 오늘 날짜
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (!allowFuture && birth.isAfter(today)) return false;

        // 만 나이 계산
        int age = Period.between(birth, today).getYears();
        // 최소 나이 제한 적용
        if (minAgeYears > 0 && age < minAgeYears) return false;

        return true;
    }
}