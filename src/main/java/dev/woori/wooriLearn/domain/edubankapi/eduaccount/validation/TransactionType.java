package dev.woori.wooriLearn.domain.edubankapi.eduaccount.validation;

/**
 * 거래 유형 (Type) Enum
 */
public enum TransactionType {
    ALL, DEPOSIT, WITHDRAW;

    public static TransactionType from(String type) {
        for (TransactionType t : values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid transaction type: " + type + ". Allowed values: ALL, DEPOSIT, WITHDRAW");
    }
}
