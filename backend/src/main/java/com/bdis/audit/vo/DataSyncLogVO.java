package com.bdis.audit.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DataSyncLogVO {

    private Long id;
    private String traceId;
    private String syncType;
    private String sourceType;
    private String targetType;
    private Long targetId;
    private Long taskId;
    private Long exchangeId;
    private String businessType;
    private Long businessId;
    private String externalNo;
    private String syncStatus;
    private Integer successCount;
    private Integer failureCount;
    private String failureReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operationTime;
}
