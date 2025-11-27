package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import lombok.Builder;

@Builder
public record EdubankapiTransferRequestDto(
        String fromAccountNumber,   // 출금계좌번호
        String toAccountNumber,     // 입금계좌번호
        Integer amount,             // 이체금액
        String accountPassword,     // 4자리 비밀번호
        String displayName         // 받는사람표시명
) {
}
