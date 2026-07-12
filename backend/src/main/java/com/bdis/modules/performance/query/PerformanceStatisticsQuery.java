package com.bdis.modules.performance.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceStatisticsQuery {

    private Long userId;

    private String performanceType;

    private String identifyStatus;
}
