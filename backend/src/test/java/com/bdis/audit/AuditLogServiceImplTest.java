package com.bdis.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.impl.AuditLogServiceImpl;
import com.bdis.modules.audit.entity.OperationLogEntity;
import com.bdis.modules.audit.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private OperationLogMapper operationLogMapper;

    @InjectMocks private AuditLogServiceImpl auditLogService;

    @Test
    void recordShouldInsertOperationLog() {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule("M05_FILE");
        dto.setOperationType("UPLOAD");
        dto.setBizType("file_resource");
        dto.setBizId(100L);

        auditLogService.record(dto);

        ArgumentCaptor<OperationLogEntity> captor =
                ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogMapper).insert(captor.capture());

        OperationLogEntity entity = captor.getValue();
        assertThat(entity.getOperationModule()).isEqualTo("M05_FILE");
        assertThat(entity.getOperationType()).isEqualTo("UPLOAD");
        assertThat(entity.getBizType()).isEqualTo("file_resource");
        assertThat(entity.getBizId()).isEqualTo(100L);
        assertThat(entity.getOperationResult()).isEqualTo("SUCCESS");
        assertThat(entity.getOperationTime()).isNotNull();
    }
}
