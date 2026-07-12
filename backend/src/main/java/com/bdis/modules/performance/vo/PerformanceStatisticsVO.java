package com.bdis.modules.performance.vo;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceStatisticsVO {

    private Long totalCount;

    private Long draftCount;

    private Long submittedCount;

    private Long approvedCount;

    private Long rejectedCount;

    private Map<String, Long> typeCounts;
}
