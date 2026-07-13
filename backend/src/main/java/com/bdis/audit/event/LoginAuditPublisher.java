package com.bdis.audit.event;

import com.bdis.audit.support.AuditPersistenceScheduler;
import com.bdis.common.utils.CurrentUserUtils;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LoginAuditPublisher {

    private final AuditPersistenceScheduler scheduler;
    private final LoginAuditWriter writer;

    public LoginAuditPublisher(AuditPersistenceScheduler scheduler, LoginAuditWriter writer) {
        this.scheduler = scheduler;
        this.writer = writer;
    }

    public void publish(Long userId, String username, String result, String failureReason) {
        String normalized = result == null ? null : result.toUpperCase(Locale.ROOT);
        LoginAuditEvent event =
                new LoginAuditEvent(
                        userId,
                        username,
                        normalized,
                        failureReason,
                        CurrentUserUtils.currentIp(),
                        CurrentUserUtils.currentUserAgent(),
                        LocalDateTime.now());
        if ("FAILED".equals(normalized)) {
            scheduler.afterCompletion("login", () -> writer.write(event));
            return;
        }
        scheduler.afterCommit("login", () -> writer.write(event));
    }
}
