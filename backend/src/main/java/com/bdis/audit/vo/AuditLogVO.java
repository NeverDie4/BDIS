package com.bdis.audit.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuditLogVO {

    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operationModule;
    private String operationType;
    private String bizType;
    private Long bizId;
    private String operationResult;
    private String errorMessage;
    private LocalDateTime operationTime;
}
