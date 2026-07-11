package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthTraceEventVO {
    private String eventType;
    private String action;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String comment;
    private LocalDateTime eventTime;
}
