package dev.woori.wooriLearn.domain.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointsDepositResponseDto {
    private Long userId;
    private Integer addedPoint;
    private Integer currentBalance;
    private String status;
    private String message;
    private LocalDateTime depositDate;
}

