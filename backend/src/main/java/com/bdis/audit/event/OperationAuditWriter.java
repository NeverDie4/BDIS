package com.bdis.audit.event;

import com.bdis.modules.audit.entity.OperationLogEntity;
import com.bdis.modules.audit.mapper.OperationLogMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OperationAuditWriter {

    private final OperationLogMapper operationLogMapper;

    public OperationAuditWriter(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(OperationAuditEvent event) {
        OperationLogEntity entity = new OperationLogEntity();
        entity.setTraceId(event.traceId());
        entity.setOperatorId(event.operatorId());
        entity.setOperatorName(event.operatorName());
        entity.setOperationModule(event.operationModule());
        entity.setOperationType(event.operationType());
        entity.setOperationDesc(event.operationDesc());
        entity.setBizType(event.bizType());
        entity.setBizId(event.bizId());
        entity.setResultStatus(event.operationResult());
        entity.setErrorMessage(event.errorMessage());
        entity.setRequestMethod(event.requestMethod());
        entity.setRequestUrl(event.requestUrl());
        entity.setRequestParam(event.requestParam());
        entity.setIpAddress(event.ipAddress());
        entity.setUserAgent(event.userAgent());
        entity.setOperationTime(event.operationTime());
        operationLogMapper.insert(entity);
    }
}
