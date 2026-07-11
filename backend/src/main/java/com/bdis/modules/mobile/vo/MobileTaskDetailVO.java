package com.bdis.modules.mobile.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MobileTaskDetailVO {

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

    private Long collectorId;

    private String collectorName;

    private String taskStatus;

    private String description;

    private String remark;

    private List<MobileBatchVO> batches;
}
