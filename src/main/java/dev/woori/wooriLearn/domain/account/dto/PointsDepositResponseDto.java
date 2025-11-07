package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointsDepositResponseDto {
    private Long userId;
    private Integer addedPoint;
    private Integer currentBalance;
    private PointsStatus status;
    private String message;
    private LocalDateTime depositDate;
}

