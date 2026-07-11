package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.DataSyncRecordDTO;
import com.bdis.audit.query.DataSyncLogQuery;
import com.bdis.audit.service.DataSyncLogService;
import com.bdis.audit.vo.DataSyncLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.audit.entity.DataSyncLogEntity;
import com.bdis.modules.audit.mapper.DataSyncLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class DataSyncLogServiceImpl implements DataSyncLogService {

    private final DataSyncLogMapper dataSyncLogMapper;

    public DataSyncLogServiceImpl(DataSyncLogMapper dataSyncLogMapper) {
        this.dataSyncLogMapper = dataSyncLogMapper;
    }

    @Override
    public void record(DataSyncRecordDTO dto) {
        DataSyncLogEntity entity = new DataSyncLogEntity();
        entity.setSyncType(dto.getSyncType());
        entity.setSourceSystem(dto.getSourceType());
        entity.setTargetTable(dto.getTargetType());
        entity.setTaskId(dto.getTaskId());
        entity.setExchangeId(dto.getExchangeId());
        entity.setBizType(dto.getBusinessType());
        entity.setBizId(dto.getBusinessId());
        entity.setExternalNo(dto.getExternalNo());
        entity.setSyncStatus(dto.getSyncStatus());
        entity.setSuccessCount(dto.getSuccessCount());
        entity.setFailureCount(dto.getFailureCount());
        entity.setErrorMessage(dto.getFailureReason());
        entity.setOperatorId(CurrentUserUtils.currentUserId());
        entity.setOperatorName(CurrentUserUtils.currentUsername());
        entity.setOperationTime(LocalDateTime.now());
        dataSyncLogMapper.insert(entity);
    }

    @Override
    public PageResult<DataSyncLogVO> page(DataSyncLogQuery query) {
        Page<DataSyncLogEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<DataSyncLogEntity> wrapper =
                new LambdaQueryWrapper<DataSyncLogEntity>()
                        .eq(
                                query.getSyncType() != null,
                                DataSyncLogEntity::getSyncType,
                                query.getSyncType())
                        .eq(
                                query.getSourceType() != null,
                                DataSyncLogEntity::getSourceSystem,
                                query.getSourceType())
                        .eq(
                                query.getTargetType() != null,
                                DataSyncLogEntity::getTargetTable,
                                query.getTargetType())
                        .eq(
                                query.getSyncStatus() != null,
                                DataSyncLogEntity::getSyncStatus,
                                query.getSyncStatus())
                        .eq(
                                query.getTaskId() != null,
                                DataSyncLogEntity::getTaskId,
                                query.getTaskId())
                        .eq(
                                query.getBusinessType() != null,
                                DataSyncLogEntity::getBizType,
                                query.getBusinessType())
                        .eq(
                                query.getBusinessId() != null,
                                DataSyncLogEntity::getBizId,
                                query.getBusinessId())
                        .eq(
                                query.getExternalNo() != null,
                                DataSyncLogEntity::getExternalNo,
                                query.getExternalNo())
                        .orderByDesc(DataSyncLogEntity::getOperationTime);
        Page<DataSyncLogEntity> result = dataSyncLogMapper.selectPage(page, wrapper);
        List<DataSyncLogVO> records =
                result.getRecords().stream()
                        .map(
                                entity -> {
                                    DataSyncLogVO vo = new DataSyncLogVO();
                                    BeanUtils.copyProperties(entity, vo);
                                    vo.setSourceType(entity.getSourceSystem());
                                    vo.setTargetType(entity.getTargetTable());
                                    vo.setBusinessType(entity.getBizType());
                                    vo.setBusinessId(entity.getBizId());
                                    vo.setFailureReason(entity.getErrorMessage());
                                    return vo;
                                })
                        .toList();
        return PageResult.of(records, result);
    }
}
