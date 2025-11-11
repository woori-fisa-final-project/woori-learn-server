package dev.woori.wooriLearn.domain.account.dto;

import dev.woori.wooriLearn.common.HistoryStatus;
import dev.woori.wooriLearn.common.SearchPeriod;
import dev.woori.wooriLearn.common.SortDirection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class PointsHistorySearchRequestDto {
    private String startDate;                      // optional
    private String endDate;                        // optional
    private HistoryStatus status = HistoryStatus.ALL;
    private SortDirection sort = SortDirection.DESC;
    private SearchPeriod period = SearchPeriod.ALL; // 1주일, 1개월, 3개월, 전체기간
}
