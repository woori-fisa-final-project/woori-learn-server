package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.domain.account.entity.HistoryStatus;
import dev.woori.wooriLearn.domain.account.entity.SearchPeriod;
import dev.woori.wooriLearn.domain.account.entity.SortDirection;

public record PointsHistorySearchRequestDto(
        String startDate,                      // optional
        String endDate,                        // optional
        HistoryStatus status,                  // default: ALL
        SortDirection sort,                    // default: DESC
        SearchPeriod period,                   // default: ALL (사용자 조회에서만 활용)
        Integer page,                          // default: 1
        Integer size,                          // default: 20
        Long userId                            // optional (관리자 필터)
) {
    public PointsHistorySearchRequestDto {
        if (status == null) status = HistoryStatus.ALL;
        if (sort == null) sort = SortDirection.DESC;
        if (period == null) period = SearchPeriod.ALL;
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1 || size > 200) size = 20;
    }
}
