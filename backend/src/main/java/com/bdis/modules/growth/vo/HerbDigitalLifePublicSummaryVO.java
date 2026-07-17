package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HerbDigitalLifePublicSummaryVO {

    private Long taskId;

    private String traceCode;

    private String taskCode;

    private String taskName;

    private String speciesName;

    private String baseName;

    private String description;

    private Integer stageCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
