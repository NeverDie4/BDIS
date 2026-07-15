package com.bdis.modules.performance.query;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceQuery {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private Long userId;

    private String performanceType;

    private String performanceLevel;

    private String identifyStatus;

    private Long standardId;

    private String sourceType;

    private LocalDateTime occurredFrom;

    private LocalDateTime occurredTo;
}
