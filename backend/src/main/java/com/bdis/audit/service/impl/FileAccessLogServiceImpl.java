package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.event.FileAccessAuditPublisher;
import com.bdis.audit.query.FileAccessLogQuery;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.audit.vo.FileAccessLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.modules.audit.entity.FileAccessLogEntity;
import com.bdis.modules.audit.mapper.FileAccessLogMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class FileAccessLogServiceImpl implements FileAccessLogService {

    private final FileAccessLogMapper fileAccessLogMapper;
    private final FileAccessAuditPublisher fileAccessAuditPublisher;

    public FileAccessLogServiceImpl(
            FileAccessLogMapper fileAccessLogMapper,
            FileAccessAuditPublisher fileAccessAuditPublisher) {
        this.fileAccessLogMapper = fileAccessLogMapper;
        this.fileAccessAuditPublisher = fileAccessAuditPublisher;
    }

    @Override
    public void record(FileAccessRecordDTO dto) {
        fileAccessAuditPublisher.publish(dto);
    }

    @Override
    public PageResult<FileAccessLogVO> page(FileAccessLogQuery query) {
        String accessResult = normalize(query.getAccessResult());
        Page<FileAccessLogEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<FileAccessLogEntity> wrapper =
                new LambdaQueryWrapper<FileAccessLogEntity>()
                        .eq(
                                query.getFileId() != null,
                                FileAccessLogEntity::getFileId,
                                query.getFileId())
                        .eq(
                                query.getOperatorId() != null,
                                FileAccessLogEntity::getOperatorId,
                                query.getOperatorId())
                        .eq(
                                query.getAccessType() != null,
                                FileAccessLogEntity::getAccessType,
                                query.getAccessType())
                        .eq(
                                accessResult != null,
                                FileAccessLogEntity::getResultStatus,
                                accessResult)
                        .ge(
                                query.getStartTime() != null,
                                FileAccessLogEntity::getOperationTime,
                                query.getStartTime())
                        .le(
                                query.getEndTime() != null,
                                FileAccessLogEntity::getOperationTime,
                                query.getEndTime())
                        .orderByDesc(FileAccessLogEntity::getOperationTime);
        Page<FileAccessLogEntity> result = fileAccessLogMapper.selectPage(page, wrapper);
        List<FileAccessLogVO> records =
                result.getRecords().stream()
                        .map(
                                entity -> {
                                    FileAccessLogVO vo = new FileAccessLogVO();
                                    BeanUtils.copyProperties(entity, vo);
                                    vo.setAccessResult(entity.getResultStatus());
                                    return vo;
                                })
                        .toList();
        return PageResult.of(records, result);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.toUpperCase(Locale.ROOT);
    }
}
