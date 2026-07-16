package com.bdis.modules.performance.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceStandardQuery {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private String performanceType;

    private Integer status;

    private String lifecycleStatus;
}
