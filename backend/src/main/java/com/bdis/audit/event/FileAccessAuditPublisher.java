package com.bdis.audit.event;

import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.support.AuditPersistenceScheduler;
import com.bdis.common.utils.CurrentUserUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class FileAccessAuditPublisher {

    private final AuditPersistenceScheduler scheduler;
    private final FileAccessAuditWriter writer;

    public FileAccessAuditPublisher(
            AuditPersistenceScheduler scheduler, FileAccessAuditWriter writer) {
        this.scheduler = scheduler;
        this.writer = writer;
    }

    public void publish(FileAccessRecordDTO dto) {
        Long operatorId = CurrentUserUtils.currentUserId();
        if (operatorId != null && operatorId <= 0) {
            operatorId = null;
        }
        FileAccessAuditEvent event =
                new FileAccessAuditEvent(
                        dto.getFileId(),
                        operatorId,
                        CurrentUserUtils.currentUsername(),
                        dto.getAccessType(),
                        dto.getAccessResult(),
                        dto.getFailureReason(),
                        CurrentUserUtils.currentIp(),
                        CurrentUserUtils.currentUserAgent(),
                        LocalDateTime.now());
        scheduler.afterCommit("file-access", () -> writer.write(event));
    }
}
