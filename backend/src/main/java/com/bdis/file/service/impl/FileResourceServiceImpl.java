package com.bdis.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.response.PageResult;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.entity.FileResourceEntity;
import com.bdis.file.mapper.FileResourceMapper;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.vo.FileContentVO;
import com.bdis.file.vo.FileResourceVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FileResourceServiceImpl implements FileResourceService {

    private final FileResourceMapper fileResourceMapper;
    private final FileStorageService fileStorageService;
    private final FileAccessLogService fileAccessLogService;
    private final AuditLogService auditLogService;
    private final FileBusinessService fileBusinessService;

    public FileResourceServiceImpl(
            FileResourceMapper fileResourceMapper,
            FileStorageService fileStorageService,
            FileAccessLogService fileAccessLogService,
            AuditLogService auditLogService,
            FileBusinessService fileBusinessService) {
        this.fileResourceMapper = fileResourceMapper;
        this.fileStorageService = fileStorageService;
        this.fileAccessLogService = fileAccessLogService;
        this.auditLogService = auditLogService;
        this.fileBusinessService = fileBusinessService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResourceVO upload(FileUploadDTO dto) {
        FileStorageService.StoredFile storedFile = fileStorageService.save(dto.getFile());
        String originalFilename = dto.getFile().getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        FileResourceEntity entity = new FileResourceEntity();
        entity.setFileNo("FILE-" + UUID.randomUUID());
        entity.setFileName(storedFile.storedName());
        entity.setOriginalFilename(originalFilename);
        entity.setFileType(dto.getFileType());
        entity.setFileFormat(extension);
        entity.setFileSize(dto.getFile().getSize());
        entity.setFileUrl(storedFile.fileUrl());
        entity.setStoragePath(storedFile.storagePath());
        entity.setStorageType("LOCAL");
        entity.setContentType(dto.getFile().getContentType());
        entity.setUploaderId(CurrentUserUtils.currentUserId());
        entity.setUploaderName(CurrentUserUtils.currentUsername());
        entity.setUploadedAt(LocalDateTime.now());
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        entity.setRemark(dto.getRemark());
        entity.setVersion(0);
        fileResourceMapper.insert(entity);
        String bizType = firstNonBlank(dto.getBizType(), dto.getBusinessType());
        Long bizId = dto.getBizId() == null ? dto.getBusinessId() : dto.getBizId();
        if (bizType != null && bizId != null) {
            FileBusinessBindDTO bindDTO = new FileBusinessBindDTO();
            bindDTO.setFileId(entity.getId());
            bindDTO.setBizType(bizType);
            bindDTO.setBizId(bizId);
            bindDTO.setFileUsage(dto.getFileUsage());
            bindDTO.setRemark(dto.getRemark());
            fileBusinessService.bind(bindDTO);
        }
        recordAudit("UPLOAD", "file_resource", entity.getId());
        return toVO(entity);
    }

    @Override
    public PageResult<FileResourceVO> page(FileResourceQuery query) {
        String bizType = firstNonBlank(query.getBizType(), query.getBusinessType());
        Long bizId = query.getBizId() == null ? query.getBusinessId() : query.getBizId();
        if (bizType != null && bizId != null) {
            List<FileResourceVO> records =
                    fileBusinessService.listByBusiness(bizType, bizId);
            Page<FileResourceEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
            page.setTotal(records.size());
            return PageResult.of(records, page);
        }
        Page<FileResourceEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<FileResourceEntity> wrapper =
                new LambdaQueryWrapper<FileResourceEntity>()
                        .eq(query.getFileType() != null, FileResourceEntity::getFileType, query.getFileType())
                        .eq(query.getUploaderId() != null, FileResourceEntity::getUploaderId, query.getUploaderId())
                        .orderByDesc(FileResourceEntity::getUploadedAt);
        Page<FileResourceEntity> result = fileResourceMapper.selectPage(page, wrapper);
        List<FileResourceVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result);
    }

    @Override
    public FileResourceVO detail(Long fileId) {
        return toVO(requireFile(fileId));
    }

    @Override
    public FileContentVO content(Long fileId, String disposition) {
        FileResourceEntity entity = requireFile(fileId);
        FileContentVO content =
                fileStorageService.load(
                        entity.getStoragePath(),
                        entity.getOriginalFilename(),
                        entity.getContentType(),
                        entity.getFileSize());
        FileAccessRecordDTO record = new FileAccessRecordDTO();
        record.setFileId(fileId);
        String accessType = "attachment".equalsIgnoreCase(disposition) ? "DOWNLOAD" : "PREVIEW";
        record.setAccessType(accessType);
        fileAccessLogService.record(record);
        recordAudit(accessType, "file_resource", fileId);
        return content;
    }

    @Override
    public void delete(Long fileId) {
        FileResourceEntity entity = requireFile(fileId);
        fileResourceMapper.deleteById(fileId);
        recordAudit("DELETE", "file_resource", entity.getId());
    }

    private FileResourceEntity requireFile(Long fileId) {
        FileResourceEntity entity = fileResourceMapper.selectById(fileId);
        if (entity == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        return entity;
    }

    private FileResourceVO toVO(FileResourceEntity entity) {
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
