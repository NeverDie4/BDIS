package com.bdis.audit.event;

import java.time.LocalDateTime;

public record DataSyncAuditEvent(
        String traceId,
        String syncType,
        String sourceSystem,
        String targetTable,
        Long targetId,
        Long taskId,
        Long exchangeId,
        String bizType,
        Long bizId,
        String externalNo,
        String syncStatus,
        Integer successCount,
        Integer failureCount,
        String errorMessage,
        Long operatorId,
        String operatorName,
        LocalDateTime operationTime) {}
