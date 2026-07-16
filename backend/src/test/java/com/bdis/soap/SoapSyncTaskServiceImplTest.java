package com.bdis.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.dto.DataSyncRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.DataSyncLogService;
import com.bdis.common.exception.BusinessException;
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
import java.time.LocalDateTime;
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
        when(soapClient.createGrowthQueryRequest(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("<request/>");
        when(soapClient.invoke("<request/>")).thenReturn("<Envelope/>");
        SoapImportResultVO importResult = new SoapImportResultVO();
        importResult.setStatus("SUCCESS");
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
        ArgumentCaptor<SoapSyncTaskEntity> taskCaptor =
                ArgumentCaptor.forClass(SoapSyncTaskEntity.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue())
                .extracting(
                        SoapSyncTaskEntity::getResourceType,
                        SoapSyncTaskEntity::getServiceName,
                        SoapSyncTaskEntity::getMethodName,
                        SoapSyncTaskEntity::getSyncDirection)
                .containsExactly(
                        "GROWTH_RECORD",
                        "CampusGrowthDataService",
                        "queryGrowthRecords",
                        "INBOUND");
        ArgumentCaptor<DataSyncRecordDTO> syncCaptor =
                ArgumentCaptor.forClass(DataSyncRecordDTO.class);
        verify(dataSyncLogService).record(syncCaptor.capture());
        DataSyncRecordDTO sync = syncCaptor.getValue();
        assertThat(sync.getTaskId()).isEqualTo(10L);
        assertThat(sync.getExchangeId()).isEqualTo(20L);
        assertThat(sync.getBusinessType()).isEqualTo("herb_growth_record");
        assertThat(sync.getBusinessId()).isEqualTo(30L);
        assertThat(sync.getExternalNo()).isEqualTo("SOAP-001");
        assertThat(sync.getSyncStatus()).isEqualTo("SUCCESS");
        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService, times(2)).record(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditRecordDTO::getOperationType)
                .contains("CREATE", "EXECUTE_SUCCESS");
    }

    @Test
    void rejectsUnsupportedResourceBeforeCreatingTask() {
        SoapSyncTaskServiceImpl service = service();
        SoapSyncTaskDTO dto = new SoapSyncTaskDTO();
        dto.setResourceType("USER");

        assertThatThrownBy(() -> service.createAndExecute(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持 GROWTH_RECORD");
        verifyNoInteractions(taskMapper, exchangeRecordMapper, soapClient, soapImportService);
    }

    @Test
    void rejectsCustomSoapMethodBeforeCreatingTask() {
        SoapSyncTaskServiceImpl service = service();
        SoapSyncTaskDTO dto = new SoapSyncTaskDTO();
        dto.setResourceType("GROWTH_RECORD");
        dto.setMethodName("queryUsers");

        assertThatThrownBy(() -> service.createAndExecute(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("操作固定为 queryGrowthRecords");
        verifyNoInteractions(taskMapper, exchangeRecordMapper, soapClient, soapImportService);
    }

    @Test
    void failedRetryMustKeepLastSuccessfulSyncCursor() {
        SoapSyncTaskServiceImpl service = service();
        LocalDateTime lastSuccessfulSyncAt = LocalDateTime.of(2026, 7, 15, 9, 0);
        SoapSyncTaskEntity failedTask = new SoapSyncTaskEntity();
        failedTask.setId(10L);
        failedTask.setTaskNo("SOAP-FAILED-001");
        failedTask.setResourceType("GROWTH_RECORD");
        failedTask.setServiceName("CampusGrowthDataService");
        failedTask.setMethodName("queryGrowthRecords");
        failedTask.setSyncDirection("INBOUND");
        failedTask.setSyncStatus("FAILED");
        failedTask.setIsMock(true);
        failedTask.setLastSyncAt(lastSuccessfulSyncAt);
        when(taskMapper.selectById(10L)).thenReturn(failedTask);
        when(soapClient.createGrowthQueryRequest("SOAP-FAILED-001", lastSuccessfulSyncAt))
                .thenThrow(new RuntimeException("mock endpoint unavailable"));

        service.retry(10L, null);

        ArgumentCaptor<SoapSyncTaskEntity> taskCaptor =
                ArgumentCaptor.forClass(SoapSyncTaskEntity.class);
        verify(taskMapper, times(3)).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues().getLast().getLastSyncAt())
                .isEqualTo(lastSuccessfulSyncAt);
    }

    private SoapSyncTaskServiceImpl service() {
        return new SoapSyncTaskServiceImpl(
                taskMapper,
                exchangeRecordMapper,
                soapClient,
                soapImportService,
                dataSyncLogService,
                auditLogService,
                new ObjectMapper());
    }
}
