package dev.woori.wooriLearn.domain.account.dto.request;

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
    public PointsUnifiedHistoryRequestDto {
        if (period == null) period = SearchPeriod.ALL;
        if (sort == null) sort = SortDirection.DESC;
        if (status == null) status = HistoryFilter.ALL;
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) {
            size = 20;
        } else if (size > 200) {
            size = 200;
        }
    }
}

