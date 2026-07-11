package com.bdis.audit.dto;

import lombok.Data;

@Data
public class DataSyncRecordDTO {

    private String syncType;
    private String sourceType;
    private String targetType;
    private Long taskId;
    private Long exchangeId;
    private String businessType;
    private Long businessId;
    private String externalNo;
    private String syncStatus;
    private Integer successCount = 0;
    private Integer failureCount = 0;
    private String failureReason;
}
