package com.bdis.audit.event;

import java.time.LocalDateTime;

public record FileAccessAuditEvent(
        Long fileId,
        Long operatorId,
        String operatorName,
        String accessType,
        String accessResult,
        String failureReason,
        String ipAddress,
        String userAgent,
        LocalDateTime operationTime) {}
