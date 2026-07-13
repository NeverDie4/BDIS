package com.bdis.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.vo.FileResourceVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileBusinessServiceImpl implements FileBusinessService {

    private final FileBusinessMapper fileBusinessMapper;
    private final FileResourceMapper fileResourceMapper;
    private final BusinessReferenceValidator businessReferenceValidator;
    private final FileAccessGuard fileAccessGuard;
    private final AuditLogService auditLogService;

    public FileBusinessServiceImpl(
            FileBusinessMapper fileBusinessMapper,
            FileResourceMapper fileResourceMapper,
            BusinessReferenceValidator businessReferenceValidator,
            FileAccessGuard fileAccessGuard,
            AuditLogService auditLogService) {
        this.fileBusinessMapper = fileBusinessMapper;
        this.fileResourceMapper = fileResourceMapper;
        this.businessReferenceValidator = businessReferenceValidator;
        this.fileAccessGuard = fileAccessGuard;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public FileBusinessVO bind(FileBusinessBindDTO dto) {
        return bindInternal(dto, true);
    }

    @Override
    @Transactional
    public FileBusinessVO bindSystem(FileBusinessBindDTO dto) {
        return bindInternal(dto, false);
    }

    private FileBusinessVO bindInternal(FileBusinessBindDTO dto, boolean validateAccess) {
        if (validateAccess) {
            businessReferenceValidator.validate(dto.getBizType(), dto.getBizId());
        }
        FileResourceEntity file = fileResourceMapper.selectById(dto.getFileId());
        if (file == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        if (Objects.equals(file.getStatus(), 0)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "文件已禁用");
        }
        if (validateAccess) {
            fileAccessGuard.requireAuthenticatedAccess(file);
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
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setRemark(dto.getRemark());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        fileBusinessMapper.insert(entity);
        recordAudit("BIND", dto.getBizType(), dto.getBizId());
        return toVO(entity);
    }

    @Override
    @Transactional
    public void unbind(Long relationId) {
        FileBusinessEntity entity = fileBusinessMapper.selectById(relationId);
        if (entity == null) {
            throw new ResourceNotFoundException("文件关联不存在");
        }
        businessReferenceValidator.validate(entity.getBizType(), entity.getBizId());
        fileBusinessMapper.deleteById(relationId);
        recordAudit("UNBIND", entity.getBizType(), entity.getBizId());
    }

    @Override
    @Transactional
    public void unbind(String bizType, Long bizId, Long fileId) {
        businessReferenceValidator.validate(bizType, bizId);
        FileBusinessEntity entity = fileBusinessMapper.selectOne(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getBizType, bizType)
                        .eq(FileBusinessEntity::getBizId, bizId)
                        .eq(FileBusinessEntity::getFileId, fileId));
        if (entity == null) {
            throw new ResourceNotFoundException("文件关联不存在");
        }
        fileBusinessMapper.deleteById(entity.getId());
        recordAudit("UNBIND", bizType, bizId);
    }

    @Override
    public void deleteByFileId(Long fileId) {
        fileBusinessMapper.delete(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getFileId, fileId));
    }

    @Override
    public void deleteByBusiness(String bizType, Long bizId) {
        fileBusinessMapper.delete(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getBizType, bizType)
                        .eq(FileBusinessEntity::getBizId, bizId));
    }

    @Override
    public void deleteByBusinessAndFile(String bizType, Long bizId, Long fileId) {
        fileBusinessMapper.delete(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getBizType, bizType)
                        .eq(FileBusinessEntity::getBizId, bizId)
                        .eq(FileBusinessEntity::getFileId, fileId));
    }

    @Override
    public List<FileResourceVO> listByBusiness(String bizType, Long bizId) {
        return listByBusiness(bizType, bizId, null);
    }

    @Override
    public List<FileResourceVO> listByBusiness(String bizType, Long bizId, String fileUsage) {
        businessReferenceValidator.validate(bizType, bizId);
        List<FileBusinessEntity> relations =
                new ArrayList<>(fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId)
                                .eq(fileUsage != null && !fileUsage.isBlank(),
                                        FileBusinessEntity::getFileUsage, fileUsage)
                                .orderByAsc(FileBusinessEntity::getSortOrder)
                                .orderByAsc(FileBusinessEntity::getCreatedAt)
                                .orderByAsc(FileBusinessEntity::getId)));
        if (relations.isEmpty()) {
            return List.of();
        }
        relations.sort(
                Comparator.comparing(
                                FileBusinessEntity::getSortOrder,
                                Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(
                                FileBusinessEntity::getCreatedAt,
                                Comparator.nullsFirst(LocalDateTime::compareTo))
                        .thenComparing(FileBusinessEntity::getId));
        List<Long> fileIds =
                relations.stream()
                        .map(FileBusinessEntity::getFileId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<Long, FileResourceEntity> files = new LinkedHashMap<>();
        for (FileResourceEntity file : fileResourceMapper.selectByIds(fileIds)) {
            files.put(file.getId(), file);
        }
        return relations.stream()
                .map(FileBusinessEntity::getFileId)
                .map(files::get)
                .filter(entity -> entity != null)
                .map(this::toFileVO)
                .toList();
    }

    @Override
    public boolean existsByBusiness(String bizType, Long bizId) {
        businessReferenceValidator.validate(bizType, bizId);
        return fileBusinessMapper.selectCount(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId))
                > 0;
    }

    @Override
    public List<FileBusinessVO> listBindingsByBusiness(String bizType, Long bizId) {
        businessReferenceValidator.validate(bizType, bizId);
        return fileBusinessMapper
                .selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId)
                                .orderByAsc(FileBusinessEntity::getSortOrder)
                                .orderByAsc(FileBusinessEntity::getCreatedAt)
                                .orderByAsc(FileBusinessEntity::getId))
                .stream()
                .map(this::toVO)
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
        vo.setFileUrl(
                "public".equalsIgnoreCase(entity.getAccessLevel())
                        ? "/api/public-files/" + entity.getId() + "/content"
                        : "/api/files/" + entity.getId() + "/content");
        if (entity.getThumbnailUrl() != null) {
            vo.setThumbnailUrl(vo.getFileUrl());
        }
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
