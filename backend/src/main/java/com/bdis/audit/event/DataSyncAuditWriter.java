package com.bdis.audit.event;

import com.bdis.modules.audit.entity.DataSyncLogEntity;
import com.bdis.modules.audit.mapper.DataSyncLogMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSyncAuditWriter {

    private final DataSyncLogMapper dataSyncLogMapper;

    public DataSyncAuditWriter(DataSyncLogMapper dataSyncLogMapper) {
        this.dataSyncLogMapper = dataSyncLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(DataSyncAuditEvent event) {
        DataSyncLogEntity entity = new DataSyncLogEntity();
        entity.setTraceId(event.traceId());
        entity.setSyncType(event.syncType());
        entity.setSourceSystem(event.sourceSystem());
        entity.setTargetTable(event.targetTable());
        entity.setTargetId(event.targetId());
        entity.setTaskId(event.taskId());
        entity.setExchangeId(event.exchangeId());
        entity.setBizType(event.bizType());
        entity.setBizId(event.bizId());
        entity.setExternalNo(event.externalNo());
        entity.setSyncStatus(event.syncStatus());
        entity.setSuccessCount(event.successCount());
        entity.setFailureCount(event.failureCount());
        entity.setErrorMessage(event.errorMessage());
        entity.setOperatorId(event.operatorId());
        entity.setOperatorName(event.operatorName());
        entity.setOperationTime(event.operationTime());
        dataSyncLogMapper.insert(entity);
    }
}
