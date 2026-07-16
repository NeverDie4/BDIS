package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthTraceEventVO {
    private String eventType;
    private String eventTitle;
    private String eventContent;
    private String action;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String comment;
    private LocalDateTime eventTime;
    private String remark;
    private String metadataJson;
}
