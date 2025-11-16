package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.common.SearchPeriod;
import dev.woori.wooriLearn.common.SortDirection;
import dev.woori.wooriLearn.domain.account.entity.HistoryFilter;

public record PointsUnifiedHistoryRequestDto(
        String startDate,
        String endDate,
        SearchPeriod period,
        SortDirection sort,
        HistoryFilter status,
        Integer page,
        Integer size,
        Long userId
) {
    public PointsUnifiedHistoryRequestDto(
            String startDate,
            String endDate,
            SearchPeriod period,
            SortDirection sort,
            HistoryFilter status,
            Integer page,
            Integer size,
            Long userId
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period == null ? SearchPeriod.ALL : period;
        this.sort = sort == null ? SortDirection.DESC : sort;
        this.status = status == null ? HistoryFilter.ALL : status;
        this.page = page == null || page < 1 ? 1 : page;
        this.size = size == null || size < 1 || size > 200 ? 20 : size;
        this.userId = userId;
    }
}

