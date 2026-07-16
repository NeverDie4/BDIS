package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.event.OperationAuditPublisher;
import com.bdis.audit.query.AuditLogQuery;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.vo.AuditLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.modules.audit.entity.OperationLogEntity;
import com.bdis.modules.audit.mapper.OperationLogMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final OperationLogMapper operationLogMapper;
    private final OperationAuditPublisher operationAuditPublisher;

    public AuditLogServiceImpl(
            OperationLogMapper operationLogMapper,
            OperationAuditPublisher operationAuditPublisher) {
        this.operationLogMapper = operationLogMapper;
        this.operationAuditPublisher = operationAuditPublisher;
    }

    @Override
    public void record(AuditRecordDTO dto) {
        operationAuditPublisher.publish(dto);
    }

    @Override
    public PageResult<AuditLogVO> page(AuditLogQuery query) {
        String operationResult = normalize(query.getOperationResult());
        Page<OperationLogEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<OperationLogEntity> wrapper =
                new LambdaQueryWrapper<OperationLogEntity>()
                        .eq(
                                query.getOperatorId() != null,
                                OperationLogEntity::getOperatorId,
                                query.getOperatorId())
                        .eq(
                                query.getBizId() != null,
                                OperationLogEntity::getBizId,
                                query.getBizId())
                        .eq(
                                query.getOperationModule() != null,
                                OperationLogEntity::getOperationModule,
                                query.getOperationModule())
                        .eq(
                                query.getOperationType() != null,
                                OperationLogEntity::getOperationType,
                                query.getOperationType())
                        .eq(
                                operationResult != null,
                                OperationLogEntity::getResultStatus,
                                operationResult)
                        .eq(
                                query.getBizType() != null,
                                OperationLogEntity::getBizType,
                                query.getBizType())
                        .ge(
                                query.getStartTime() != null,
                                OperationLogEntity::getOperationTime,
                                query.getStartTime())
                        .le(
                                query.getEndTime() != null,
                                OperationLogEntity::getOperationTime,
                                query.getEndTime())
                        .orderByDesc(OperationLogEntity::getOperationTime);
        Page<OperationLogEntity> result = operationLogMapper.selectPage(page, wrapper);
        List<AuditLogVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.toUpperCase(Locale.ROOT);
    }

    @Override
    public AuditLogVO detail(Long logId) {
        OperationLogEntity entity = operationLogMapper.selectById(logId);
        if (entity == null) {
            throw new ResourceNotFoundException("操作日志不存在");
        }
        return toVO(entity);
    }

    private AuditLogVO toVO(OperationLogEntity entity) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setOperationResult(entity.getResultStatus());
        return vo;
    }
}
