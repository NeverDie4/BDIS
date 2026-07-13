package com.bdis.audit.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuditLogVO {

    private Long id;
    private String traceId;
    private Long operatorId;
    private String operatorName;
    private String operationModule;
    private String operationType;
    private String operationDesc;
    private String bizType;
    private Long bizId;
    private String operationResult;
    private String errorMessage;
    private String requestMethod;
    private String requestUrl;
    private String requestParam;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime operationTime;
}
