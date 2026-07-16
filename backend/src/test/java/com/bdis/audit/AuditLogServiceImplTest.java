package com.bdis.audit;

import static org.mockito.Mockito.verify;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.event.OperationAuditPublisher;
import com.bdis.audit.service.impl.AuditLogServiceImpl;
import com.bdis.modules.audit.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private OperationLogMapper operationLogMapper;

    @Mock private OperationAuditPublisher operationAuditPublisher;

    @InjectMocks private AuditLogServiceImpl auditLogService;

    @Test
    void recordShouldPublishOperationEvent() {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule("M05_FILE");
        dto.setOperationType("UPLOAD");
        dto.setBizType("file_resource");
        dto.setBizId(100L);

        auditLogService.record(dto);

        verify(operationAuditPublisher).publish(dto);
    }
}
