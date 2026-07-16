package com.bdis.modules.performance.query;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceStatisticsQuery {

    private Long userId;

    private String performanceType;

    private String performanceLevel;

    private String identifyStatus;

    private Long participantUserId;

    private LocalDateTime occurredFrom;

    private LocalDateTime occurredTo;
}
