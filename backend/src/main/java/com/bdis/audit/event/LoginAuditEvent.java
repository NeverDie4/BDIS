package com.bdis.audit.event;

import java.time.LocalDateTime;

public record LoginAuditEvent(
        Long userId,
        String username,
        String loginResult,
        String failureReason,
        String ipAddress,
        String userAgent,
        LocalDateTime loggedInAt) {}
