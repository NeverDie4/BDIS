package com.bdis.modules.mobile.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MobileTaskVO {

    private Long taskId;

    private String taskCode;

    private String taskName;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String collectPlace;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private String taskStatus;

    private Integer batchCount;

    private Integer unfinishedBatchCount;

    private String description;
}
