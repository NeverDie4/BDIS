package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.query.FileAccessLogQuery;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.audit.vo.FileAccessLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.audit.entity.FileAccessLogEntity;
import com.bdis.modules.audit.mapper.FileAccessLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class FileAccessLogServiceImpl implements FileAccessLogService {

    private final FileAccessLogMapper fileAccessLogMapper;

    public FileAccessLogServiceImpl(FileAccessLogMapper fileAccessLogMapper) {
        this.fileAccessLogMapper = fileAccessLogMapper;
    }

    @Override
    public void record(FileAccessRecordDTO dto) {
        FileAccessLogEntity entity = new FileAccessLogEntity();
        entity.setFileId(dto.getFileId());
        entity.setOperatorId(CurrentUserUtils.currentUserId());
        entity.setOperatorName(CurrentUserUtils.currentUsername());
        entity.setAccessType(dto.getAccessType());
        entity.setAccessResult(dto.getAccessResult());
        entity.setIpAddress(CurrentUserUtils.currentIp());
        entity.setUserAgent(CurrentUserUtils.currentUserAgent());
        entity.setOperationTime(LocalDateTime.now());
        fileAccessLogMapper.insert(entity);
    }

    @Override
    public PageResult<FileAccessLogVO> page(FileAccessLogQuery query) {
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
                        .orderByDesc(FileAccessLogEntity::getOperationTime);
        Page<FileAccessLogEntity> result = fileAccessLogMapper.selectPage(page, wrapper);
        List<FileAccessLogVO> records =
                result.getRecords().stream()
                        .map(
                                entity -> {
                                    FileAccessLogVO vo = new FileAccessLogVO();
                                    BeanUtils.copyProperties(entity, vo);
                                    return vo;
                                })
                        .toList();
        return PageResult.of(records, result);
    }
}
