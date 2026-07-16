package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.DataSyncRecordDTO;
import com.bdis.audit.event.DataSyncAuditPublisher;
import com.bdis.audit.query.DataSyncLogQuery;
import com.bdis.audit.service.DataSyncLogService;
import com.bdis.audit.vo.DataSyncLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.modules.audit.entity.DataSyncLogEntity;
import com.bdis.modules.audit.mapper.DataSyncLogMapper;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class DataSyncLogServiceImpl implements DataSyncLogService {

    private final DataSyncLogMapper dataSyncLogMapper;
    private final DataSyncAuditPublisher dataSyncAuditPublisher;

    public DataSyncLogServiceImpl(
            DataSyncLogMapper dataSyncLogMapper, DataSyncAuditPublisher dataSyncAuditPublisher) {
        this.dataSyncLogMapper = dataSyncLogMapper;
        this.dataSyncAuditPublisher = dataSyncAuditPublisher;
    }

    @Override
    public void record(DataSyncRecordDTO dto) {
        dataSyncAuditPublisher.publish(dto);
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
                        .ge(
                                query.getStartTime() != null,
                                DataSyncLogEntity::getOperationTime,
                                query.getStartTime())
                        .le(
                                query.getEndTime() != null,
                                DataSyncLogEntity::getOperationTime,
                                query.getEndTime())
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
