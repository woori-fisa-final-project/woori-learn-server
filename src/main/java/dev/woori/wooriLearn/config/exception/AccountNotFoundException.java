package dev.woori.wooriLearn.config.exception;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;

public class AccountNotFoundException extends CommonException {
    public AccountNotFoundException(String accountNum) {
        super(ErrorCode.ENTITY_NOT_FOUND, "계좌를 찾을 수 없습니다. accountNum=" + accountNum);
    }
}
