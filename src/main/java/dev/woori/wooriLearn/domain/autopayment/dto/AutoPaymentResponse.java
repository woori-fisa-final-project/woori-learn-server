package dev.woori.wooriLearn.domain.autopayment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 자동이체 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoPaymentResponse {

    private Long id;
    private Long educationalAccountId;
    private String depositNumber;
    private String depositBankCode;
    private Integer amount;
    private String counterpartyName;
    private String displayName;
    private Integer transferCycle;
    private Integer designatedDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    private String processingStatus;
}