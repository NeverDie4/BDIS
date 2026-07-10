package com.bdis.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.entity.FileBusinessEntity;
import com.bdis.file.entity.FileResourceEntity;
import com.bdis.file.mapper.FileBusinessMapper;
import com.bdis.file.mapper.FileResourceMapper;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.file.vo.FileResourceVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class FileBusinessServiceImpl implements FileBusinessService {

    private final FileBusinessMapper fileBusinessMapper;
    private final FileResourceMapper fileResourceMapper;
    private final BusinessReferenceValidator businessReferenceValidator;
    private final AuditLogService auditLogService;

    public FileBusinessServiceImpl(
            FileBusinessMapper fileBusinessMapper,
            FileResourceMapper fileResourceMapper,
            BusinessReferenceValidator businessReferenceValidator,
            AuditLogService auditLogService) {
        this.fileBusinessMapper = fileBusinessMapper;
        this.fileResourceMapper = fileResourceMapper;
        this.businessReferenceValidator = businessReferenceValidator;
        this.auditLogService = auditLogService;
    }

    @Override
    public FileBusinessVO bind(FileBusinessBindDTO dto) {
        businessReferenceValidator.validate(dto.getBizType(), dto.getBizId());
        if (fileResourceMapper.selectById(dto.getFileId()) == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        LambdaQueryWrapper<FileBusinessEntity> wrapper =
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getFileId, dto.getFileId())
                        .eq(FileBusinessEntity::getBizType, dto.getBizType())
                        .eq(FileBusinessEntity::getBizId, dto.getBizId());
        FileBusinessEntity existing = fileBusinessMapper.selectOne(wrapper);
        if (existing != null) {
            return toVO(existing);
        }
        FileBusinessEntity entity = new FileBusinessEntity();
        entity.setFileId(dto.getFileId());
        entity.setBizType(dto.getBizType());
        entity.setBizId(dto.getBizId());
        entity.setFileUsage(dto.getFileUsage());
        entity.setRemark(dto.getRemark());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        fileBusinessMapper.insert(entity);
        recordAudit("BIND", dto.getBizType(), dto.getBizId());
        return toVO(entity);
    }

    @Override
    public void unbind(Long relationId) {
        FileBusinessEntity entity = fileBusinessMapper.selectById(relationId);
        if (entity == null) {
            throw new ResourceNotFoundException("文件关联不存在");
        }
        fileBusinessMapper.deleteById(relationId);
        recordAudit("UNBIND", entity.getBizType(), entity.getBizId());
    }

    @Override
    public List<FileResourceVO> listByBusiness(String bizType, Long bizId) {
        businessReferenceValidator.validate(bizType, bizId);
        List<FileBusinessEntity> relations =
                fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId));
        return relations.stream()
                .map(FileBusinessEntity::getFileId)
                .map(fileResourceMapper::selectById)
                .filter(entity -> entity != null)
                .map(this::toFileVO)
                .toList();
    }

    private FileBusinessVO toVO(FileBusinessEntity entity) {
        FileBusinessVO vo = new FileBusinessVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private FileResourceVO toFileVO(FileResourceEntity entity) {
        FileResourceVO vo = new FileResourceVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private void recordAudit(String operationType, String bizType, Long bizId) {
        AuditRecordDTO audit = new AuditRecordDTO();
        audit.setOperationModule("M05_FILE");
        audit.setOperationType(operationType);
        audit.setBizType(bizType);
        audit.setBizId(bizId);
        auditLogService.record(audit);
    }
}
