package com.bdis.soap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.dto.DataSyncRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.DataSyncLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.soap.entity.SoapExchangeRecordEntity;
import com.bdis.modules.soap.entity.SoapSyncTaskEntity;
import com.bdis.modules.soap.mapper.SoapExchangeRecordMapper;
import com.bdis.modules.soap.mapper.SoapSyncTaskMapper;
import com.bdis.soap.component.SoapClient;
import com.bdis.soap.dto.SoapRetryDTO;
import com.bdis.soap.dto.SoapSyncTaskDTO;
import com.bdis.soap.query.SoapSyncTaskQuery;
import com.bdis.soap.service.SoapImportService;
import com.bdis.soap.service.SoapSyncTaskService;
import com.bdis.soap.vo.SoapExchangeRecordVO;
import com.bdis.soap.vo.SoapImportResultVO;
import com.bdis.soap.vo.SoapSyncTaskVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoapSyncTaskServiceImpl implements SoapSyncTaskService {

    private final SoapSyncTaskMapper taskMapper;
    private final SoapExchangeRecordMapper exchangeRecordMapper;
    private final SoapClient soapClient;
    private final SoapImportService soapImportService;
    private final DataSyncLogService dataSyncLogService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public SoapSyncTaskServiceImpl(
            SoapSyncTaskMapper taskMapper,
            SoapExchangeRecordMapper exchangeRecordMapper,
            SoapClient soapClient,
            SoapImportService soapImportService,
            DataSyncLogService dataSyncLogService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.exchangeRecordMapper = exchangeRecordMapper;
        this.soapClient = soapClient;
        this.soapImportService = soapImportService;
        this.dataSyncLogService = dataSyncLogService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SoapExchangeRecordVO createAndExecute(SoapSyncTaskDTO dto) {
        SoapSyncTaskEntity task = new SoapSyncTaskEntity();
        task.setTaskNo("SOAP-" + UUID.randomUUID());
        task.setTaskName(dto.getResourceType());
        task.setResourceType(dto.getResourceType());
        task.setServiceName(defaultValue(dto.getServiceName(), "MockHerbService"));
        task.setMethodName(defaultValue(dto.getMethodName(), "syncGrowthRecord"));
        task.setSyncDirection(defaultValue(dto.getDirection(), "INBOUND"));
        task.setSyncStatus("PENDING");
        task.setMock(dto.getMock() == null || dto.getMock());
        task.setRetryCount(0);
        task.setStatus(1);
        task.setIsDeleted(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setCreatedBy(CurrentUserUtils.currentUserId());
        task.setRemark(dto.getRemark());
        task.setVersion(0);
        taskMapper.insert(task);
        recordAudit("CREATE", task.getId());
        return execute(task, dto.getRequestXml());
    }

    @Override
    public PageResult<SoapSyncTaskVO> page(SoapSyncTaskQuery query) {
        Page<SoapSyncTaskEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<SoapSyncTaskEntity> wrapper =
                new LambdaQueryWrapper<SoapSyncTaskEntity>()
                        .eq(
                                query.getResourceType() != null,
                                SoapSyncTaskEntity::getResourceType,
                                query.getResourceType())
                        .eq(
                                query.getSyncStatus() != null,
                                SoapSyncTaskEntity::getSyncStatus,
                                query.getSyncStatus())
                        .eq(
                                query.getDirection() != null,
                                SoapSyncTaskEntity::getSyncDirection,
                                query.getDirection())
                        .orderByDesc(SoapSyncTaskEntity::getCreatedAt);
        Page<SoapSyncTaskEntity> result = taskMapper.selectPage(page, wrapper);
        List<SoapSyncTaskVO> records = result.getRecords().stream().map(this::toTaskVO).toList();
        return PageResult.of(records, result);
    }

    @Override
    public SoapExchangeRecordVO detail(Long jobId) {
        SoapExchangeRecordEntity record =
                exchangeRecordMapper.selectOne(
                        new LambdaQueryWrapper<SoapExchangeRecordEntity>()
                                .eq(SoapExchangeRecordEntity::getTaskId, jobId)
                                .orderByDesc(SoapExchangeRecordEntity::getCalledAt)
                                .last("limit 1"));
        if (record == null) {
            throw new ResourceNotFoundException("SOAP 交换记录不存在");
        }
        return toExchangeVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SoapExchangeRecordVO retry(Long jobId, SoapRetryDTO dto) {
        SoapSyncTaskEntity task = taskMapper.selectById(jobId);
        if (task == null) {
            throw new ResourceNotFoundException("SOAP 任务不存在");
        }
        if (!"FAILED".equals(task.getSyncStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "只有失败任务可以重试");
        }
        task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        task.setSyncStatus("PENDING");
        task.setRemark(dto == null ? task.getRemark() : dto.getReason());
        taskMapper.updateById(task);
        recordAudit("RETRY", task.getId());
        return execute(task, null);
    }

    private SoapExchangeRecordVO execute(SoapSyncTaskEntity task, String requestXml) {
        task.setSyncStatus("PROCESSING");
        taskMapper.updateById(task);
        String responseXml = null;
        String parsedPayload = null;
        String status = "SUCCESS";
        String errorMessage = null;
        SoapImportResultVO importResult = null;
        try {
            responseXml =
                    requestXml != null && !requestXml.isBlank()
                            ? requestXml
                            : soapClient.mockResponse(task.getResourceType());
            importResult =
                    soapImportService.parseAndPrepareImport(task.getResourceType(), responseXml);
            parsedPayload = objectMapper.writeValueAsString(importResult.getParsedData());
            task.setSyncStatus("SUCCESS");
        } catch (Exception exception) {
            status = "FAILED";
            errorMessage = exception.getMessage();
            task.setSyncStatus("FAILED");
        }
        task.setLastSyncAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        SoapExchangeRecordEntity record = new SoapExchangeRecordEntity();
        record.setExchangeNo("EX-" + UUID.randomUUID());
        record.setTaskId(task.getId());
        record.setServiceName(task.getServiceName());
        record.setMethodName(task.getMethodName());
        record.setRequestXml(requestXml);
        record.setResponseXml(responseXml);
        record.setSyncStatus(status);
        record.setErrorMessage(errorMessage);
        record.setParsedPayload(parsedPayload);
        record.setCalledBy(CurrentUserUtils.currentUserId());
        record.setCalledByName(CurrentUserUtils.currentUsername());
        record.setCalledAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        exchangeRecordMapper.insert(record);
        recordSync(task, record, importResult, status, errorMessage);
        recordAudit("SUCCESS".equals(status) ? "EXECUTE_SUCCESS" : "EXECUTE_FAILED", task.getId());
        return toExchangeVO(record);
    }

    private void recordSync(
            SoapSyncTaskEntity task,
            SoapExchangeRecordEntity record,
            SoapImportResultVO importResult,
            String status,
            String failureReason) {
        DataSyncRecordDTO dto = new DataSyncRecordDTO();
        dto.setSyncType("SOAP");
        dto.setSourceType("SOAP");
        dto.setTargetType("BUSINESS_IMPORT");
        dto.setTaskId(task.getId());
        dto.setExchangeId(record.getId());
        dto.setBusinessType(
                importResult == null ? "herb_growth_record" : importResult.getBusinessType());
        dto.setBusinessId(importResult == null ? null : importResult.getBusinessId());
        dto.setExternalNo(importResult == null ? null : importResult.getExternalNo());
        dto.setSyncStatus(importResult == null ? status : importResult.getStatus());
        dto.setSuccessCount(
                importResult == null
                        ? ("SUCCESS".equals(status) ? 1 : 0)
                        : importResult.getSuccessCount());
        dto.setFailureCount(
                importResult == null
                        ? ("FAILED".equals(status) ? 1 : 0)
                        : importResult.getFailureCount());
        dto.setFailureReason(failureReason);
        dataSyncLogService.record(dto);
    }

    private void recordAudit(String operationType, Long taskId) {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule("M19_SOAP");
        dto.setOperationType(operationType);
        dto.setBizType("soap_sync_task");
        dto.setBizId(taskId);
        auditLogService.record(dto);
    }

    private SoapSyncTaskVO toTaskVO(SoapSyncTaskEntity entity) {
        SoapSyncTaskVO vo = new SoapSyncTaskVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private SoapExchangeRecordVO toExchangeVO(SoapExchangeRecordEntity entity) {
        SoapExchangeRecordVO vo = new SoapExchangeRecordVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setExchangeStatus(entity.getSyncStatus());
        return vo;
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
