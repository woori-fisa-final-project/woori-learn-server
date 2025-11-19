package dev.woori.wooriLearn.domain.edubankapi.entity;

public enum AccountType {
    CHECKING("1001"),   // 입출금
    SAVINGS("1002"),    // 예금
    DEPOSIT("1003");    // 적금

    private final String bankCode;

    AccountType(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankCode() {
        return bankCode;
    }
}
