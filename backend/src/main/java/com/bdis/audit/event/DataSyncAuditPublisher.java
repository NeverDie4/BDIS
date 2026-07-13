package com.bdis.audit.event;

import com.bdis.audit.dto.DataSyncRecordDTO;
import com.bdis.audit.support.AuditPersistenceScheduler;
import com.bdis.common.utils.CurrentUserUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class DataSyncAuditPublisher {

    private final AuditPersistenceScheduler scheduler;
    private final DataSyncAuditWriter writer;

    public DataSyncAuditPublisher(AuditPersistenceScheduler scheduler, DataSyncAuditWriter writer) {
        this.scheduler = scheduler;
        this.writer = writer;
    }

    public void publish(DataSyncRecordDTO dto) {
        Long operatorId = CurrentUserUtils.currentUserId();
        if (operatorId != null && operatorId <= 0) {
            operatorId = null;
        }
        DataSyncAuditEvent event =
                new DataSyncAuditEvent(
                        CurrentUserUtils.currentTraceId(),
                        dto.getSyncType(),
                        dto.getSourceType(),
                        dto.getTargetType(),
                        dto.getTargetId(),
                        dto.getTaskId(),
                        dto.getExchangeId(),
                        dto.getBusinessType(),
                        dto.getBusinessId(),
                        dto.getExternalNo(),
                        dto.getSyncStatus(),
                        dto.getSuccessCount(),
                        dto.getFailureCount(),
                        dto.getFailureReason(),
                        operatorId,
                        CurrentUserUtils.currentUsername(),
                        LocalDateTime.now());
        scheduler.afterCommit("data-sync", () -> writer.write(event));
    }
}
