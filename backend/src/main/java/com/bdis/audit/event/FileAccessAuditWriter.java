package com.bdis.audit.event;

import com.bdis.modules.audit.entity.FileAccessLogEntity;
import com.bdis.modules.audit.mapper.FileAccessLogMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FileAccessAuditWriter {

    private final FileAccessLogMapper fileAccessLogMapper;

    public FileAccessAuditWriter(FileAccessLogMapper fileAccessLogMapper) {
        this.fileAccessLogMapper = fileAccessLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(FileAccessAuditEvent event) {
        FileAccessLogEntity entity = new FileAccessLogEntity();
        entity.setFileId(event.fileId());
        entity.setOperatorId(event.operatorId());
        entity.setOperatorName(event.operatorName());
        entity.setAccessType(event.accessType());
        entity.setResultStatus(event.accessResult());
        entity.setFailureReason(event.failureReason());
        entity.setIpAddress(event.ipAddress());
        entity.setUserAgent(event.userAgent());
        entity.setOperationTime(event.operationTime());
        fileAccessLogMapper.insert(entity);
    }
}
