package com.bdis.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.dto.DataSyncRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.DataSyncLogService;
import com.bdis.modules.soap.entity.SoapExchangeRecordEntity;
import com.bdis.modules.soap.entity.SoapSyncTaskEntity;
import com.bdis.modules.soap.mapper.SoapExchangeRecordMapper;
import com.bdis.modules.soap.mapper.SoapSyncTaskMapper;
import com.bdis.soap.component.SoapClient;
import com.bdis.soap.dto.SoapSyncTaskDTO;
import com.bdis.soap.service.SoapImportService;
import com.bdis.soap.service.impl.SoapSyncTaskServiceImpl;
import com.bdis.soap.vo.SoapExchangeRecordVO;
import com.bdis.soap.vo.SoapImportResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SoapSyncTaskServiceImplTest {

    @Mock private SoapSyncTaskMapper taskMapper;
    @Mock private SoapExchangeRecordMapper exchangeRecordMapper;
    @Mock private SoapClient soapClient;
    @Mock private SoapImportService soapImportService;
    @Mock private DataSyncLogService dataSyncLogService;
    @Mock private AuditLogService auditLogService;

    @Test
    void createAndExecuteShouldRecordExchangeSyncAndAuditLogs() {
        SoapSyncTaskServiceImpl service =
                new SoapSyncTaskServiceImpl(
                        taskMapper,
                        exchangeRecordMapper,
                        soapClient,
                        soapImportService,
                        dataSyncLogService,
                        auditLogService,
                        new ObjectMapper());
        doAnswer(
                        invocation -> {
                            SoapSyncTaskEntity task = invocation.getArgument(0);
                            task.setId(10L);
                            return 1;
                        })
                .when(taskMapper)
                .insert(any(SoapSyncTaskEntity.class));
        doAnswer(
                        invocation -> {
                            SoapExchangeRecordEntity record = invocation.getArgument(0);
                            record.setId(20L);
                            return 1;
                        })
                .when(exchangeRecordMapper)
                .insert(any(SoapExchangeRecordEntity.class));
        when(soapClient.mockResponse("GROWTH_RECORD")).thenReturn("<Envelope/>");
        SoapImportResultVO importResult = new SoapImportResultVO();
        importResult.setStatus("IMPORTED");
        importResult.setBusinessType("herb_growth_record");
        importResult.setBusinessId(30L);
        importResult.setExternalNo("SOAP-001");
        importResult.setSuccessCount(1);
        importResult.setFailureCount(0);
        importResult.setParsedData(Map.of("externalNo", "SOAP-001"));
        when(soapImportService.parseAndPrepareImport("GROWTH_RECORD", "<Envelope/>"))
                .thenReturn(importResult);
        SoapSyncTaskDTO dto = new SoapSyncTaskDTO();
        dto.setResourceType("GROWTH_RECORD");

        SoapExchangeRecordVO vo = service.createAndExecute(dto);

        assertThat(vo.getId()).isEqualTo(20L);
        ArgumentCaptor<DataSyncRecordDTO> syncCaptor =
                ArgumentCaptor.forClass(DataSyncRecordDTO.class);
        verify(dataSyncLogService).record(syncCaptor.capture());
        DataSyncRecordDTO sync = syncCaptor.getValue();
        assertThat(sync.getTaskId()).isEqualTo(10L);
        assertThat(sync.getExchangeId()).isEqualTo(20L);
        assertThat(sync.getBusinessType()).isEqualTo("herb_growth_record");
        assertThat(sync.getBusinessId()).isEqualTo(30L);
        assertThat(sync.getExternalNo()).isEqualTo("SOAP-001");
        assertThat(sync.getSyncStatus()).isEqualTo("IMPORTED");
        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService, times(2)).record(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditRecordDTO::getOperationType)
                .contains("CREATE", "EXECUTE_SUCCESS");
    }
}
