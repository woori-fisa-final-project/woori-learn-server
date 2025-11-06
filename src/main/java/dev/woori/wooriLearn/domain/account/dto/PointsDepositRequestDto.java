package dev.woori.wooriLearn.domain.account.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointsDepositRequestDto {
    private Long userId;
    private Integer amount;
    private String reason;
}
