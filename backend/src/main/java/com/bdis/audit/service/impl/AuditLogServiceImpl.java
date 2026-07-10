package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.query.AuditLogQuery;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.vo.AuditLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.audit.entity.OperationLogEntity;
import com.bdis.modules.audit.mapper.OperationLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final OperationLogMapper operationLogMapper;

    public AuditLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void record(AuditRecordDTO dto) {
        OperationLogEntity entity = new OperationLogEntity();
        entity.setOperatorId(CurrentUserUtils.currentUserId());
        entity.setOperatorName(CurrentUserUtils.currentUsername());
        entity.setOperationModule(dto.getOperationModule());
        entity.setOperationType(dto.getOperationType());
        entity.setBizType(dto.getBizType());
        entity.setBizId(dto.getBizId());
        entity.setOperationResult(dto.getOperationResult());
        entity.setErrorMessage(dto.getErrorMessage());
        entity.setRequestMethod(CurrentUserUtils.currentRequestMethod());
        entity.setRequestUri(CurrentUserUtils.currentRequestUri());
        entity.setIpAddress(CurrentUserUtils.currentIp());
        entity.setUserAgent(CurrentUserUtils.currentUserAgent());
        entity.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(entity);
    }

    @Override
    public PageResult<AuditLogVO> page(AuditLogQuery query) {
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
                                query.getBizType() != null,
                                OperationLogEntity::getBizType,
                                query.getBizType())
                        .orderByDesc(OperationLogEntity::getOperationTime);
        Page<OperationLogEntity> result = operationLogMapper.selectPage(page, wrapper);
        List<AuditLogVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result);
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
        return vo;
    }
}
