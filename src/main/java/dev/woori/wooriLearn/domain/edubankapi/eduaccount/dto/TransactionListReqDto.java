package dev.woori.wooriLearn.domain.edubankapi.eduaccount.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record TransactionListReqDto(
        @NotNull(message = "계좌 ID는 필수입니다.")
        Long accountId, // 계좌번호
        String period, // 조회기간 1M/3M/6M/1년, 없으면 1월
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate, // 직접 시작일 지정
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,   // 직접 종료일 지정
        String type // 거래구분 필터, 기본 전체 보기
) {
    public String typeOrDefault() {
        return (type == null || type.isBlank()) ? "ALL" : type;
    }

    public String periodOrDefault() {
        return (period == null || period.isBlank()) ? "1M" : period;
    }
}
