package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthPublicTraceEventVO {

    private String eventType;
    private String eventTitle;
    private String eventContent;
    private String beforeStatus;
    private String afterStatus;
    private String operatorName;
    private LocalDateTime eventTime;
}
