package com.bdis.audit.event;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.support.AuditPersistenceScheduler;
import com.bdis.common.utils.CurrentUserUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class OperationAuditPublisher {

    private static final int MAX_REQUEST_PARAM_LENGTH = 4000;

    private final AuditPersistenceScheduler scheduler;
    private final OperationAuditWriter writer;

    public OperationAuditPublisher(
            AuditPersistenceScheduler scheduler, OperationAuditWriter writer) {
        this.scheduler = scheduler;
        this.writer = writer;
    }

    public void publish(AuditRecordDTO dto) {
        OperationAuditEvent event = snapshot(dto);
        scheduler.afterCommit("operation", () -> writer.write(event));
    }

    private OperationAuditEvent snapshot(AuditRecordDTO dto) {
        Long operatorId = CurrentUserUtils.currentUserId();
        if (operatorId != null && operatorId <= 0) {
            operatorId = null;
        }
        return new OperationAuditEvent(
                CurrentUserUtils.currentTraceId(),
                operatorId,
                CurrentUserUtils.currentUsername(),
                dto.getOperationModule(),
                dto.getOperationType(),
                StringUtils.hasText(dto.getOperationDesc())
                        ? dto.getOperationDesc()
                        : dto.getOperationType(),
                dto.getBizType(),
                dto.getBizId(),
                dto.getOperationResult(),
                dto.getErrorMessage(),
                CurrentUserUtils.currentRequestMethod(),
                CurrentUserUtils.currentRequestUri(),
                sanitizedRequestParams(),
                CurrentUserUtils.currentIp(),
                CurrentUserUtils.currentUserAgent(),
                LocalDateTime.now());
    }

    private String sanitizedRequestParams() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String value =
                request.getParameterMap().entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + parameterValue(entry))
                        .collect(Collectors.joining("&"));
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= MAX_REQUEST_PARAM_LENGTH
                ? value
                : value.substring(0, MAX_REQUEST_PARAM_LENGTH);
    }

    private String parameterValue(Map.Entry<String, String[]> entry) {
        String key = entry.getKey().toLowerCase(Locale.ROOT);
        if (key.contains("password")
                || key.contains("token")
                || key.contains("secret")
                || key.contains("authorization")) {
            return "***";
        }
        return Arrays.toString(entry.getValue());
    }
}
