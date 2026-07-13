package com.bdis.audit.event;

import java.time.LocalDateTime;

public record OperationAuditEvent(
        String traceId,
        Long operatorId,
        String operatorName,
        String operationModule,
        String operationType,
        String operationDesc,
        String bizType,
        Long bizId,
        String operationResult,
        String errorMessage,
        String requestMethod,
        String requestUrl,
        String requestParam,
        String ipAddress,
        String userAgent,
        LocalDateTime operationTime) {}
