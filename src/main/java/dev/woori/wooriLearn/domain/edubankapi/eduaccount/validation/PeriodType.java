package dev.woori.wooriLearn.domain.edubankapi.eduaccount.validation;

/**
 * 거래 조회 기간 (Period) Enum
 */
public enum PeriodType {
    ONE_MONTH("1M", 1),
    THREE_MONTHS("3M", 3),
    SIX_MONTHS("6M", 6),
    ONE_YEAR("1Y", 12);

    private final String code;
    private final int months;

    PeriodType(String code, int months) {
        this.code = code;
        this.months = months;
    }

    public String getCode() {
        return code;
    }

    public int getMonths() {
        return months;
    }

    /**
     * 문자열 값 -> Enum 매핑
     */
    public static PeriodType from(String code) {
        for (PeriodType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid period value: " + code + ". Allowed values: 1M, 3M, 6M, 1Y");
    }
}
