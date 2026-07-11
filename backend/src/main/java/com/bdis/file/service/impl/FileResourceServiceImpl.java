package com.bdis.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.vo.FileResourceVO;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FileResourceServiceImpl implements FileResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileResourceServiceImpl.class);

    private final FileResourceMapper fileResourceMapper;
    private final FileStorageService fileStorageService;
    private final FileAccessLogService fileAccessLogService;
    private final AuditLogService auditLogService;
    private final FileBusinessService fileBusinessService;
    private final FileBusinessMapper fileBusinessMapper;
    private final BusinessReferenceValidator businessReferenceValidator;

    public FileResourceServiceImpl(
            FileResourceMapper fileResourceMapper,
            FileStorageService fileStorageService,
            FileAccessLogService fileAccessLogService,
            AuditLogService auditLogService,
            FileBusinessService fileBusinessService,
            FileBusinessMapper fileBusinessMapper,
            BusinessReferenceValidator businessReferenceValidator) {
        this.fileResourceMapper = fileResourceMapper;
        this.fileStorageService = fileStorageService;
        this.fileAccessLogService = fileAccessLogService;
        this.auditLogService = auditLogService;
        this.fileBusinessService = fileBusinessService;
        this.fileBusinessMapper = fileBusinessMapper;
        this.businessReferenceValidator = businessReferenceValidator;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResourceVO upload(FileUploadDTO dto) {
        FileStorageService.StoredFile storedFile = fileStorageService.save(dto.getFile());
        try {
            String originalFilename = dto.getFile().getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(originalFilename);
            FileResourceEntity entity = new FileResourceEntity();
            entity.setFileNo("FILE-" + UUID.randomUUID());
            entity.setFileName(storedFile.storedName());
            entity.setOriginalFilename(originalFilename);
            entity.setFileType(resolveFileType(dto));
            entity.setFileFormat(extension);
            entity.setFileSize(dto.getFile().getSize());
            entity.setFileUrl(storedFile.fileUrl());
            entity.setThumbnailUrl(
                    "image".equals(entity.getFileType()) ? storedFile.fileUrl() : null);
            entity.setStoragePath(storedFile.storagePath());
            entity.setStorageType("local");
            entity.setAccessLevel(
                    StringUtils.hasText(dto.getAccessLevel()) ? dto.getAccessLevel() : "private");
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
            bindIfRequested(dto, entity.getId());
            recordAudit("UPLOAD", "file_resource", entity.getId());
            return toVO(entity);
        } catch (RuntimeException exception) {
            try {
                fileStorageService.delete(storedFile.storagePath());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Override
    public PageResult<FileResourceVO> page(FileResourceQuery query) {
        String bizType = firstNonBlank(query.getBizType(), query.getBusinessType());
        Long bizId = query.getBizId() == null ? query.getBusinessId() : query.getBizId();
        if (bizType != null && bizId != null) {
            List<FileResourceVO> records = fileBusinessService.listByBusiness(bizType, bizId);
            Page<FileResourceEntity> page = new Page<>(query.getPage(), query.getSize());
            page.setTotal(records.size());
            return PageResult.of(records, page);
        }
        Page<FileResourceEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<FileResourceEntity> wrapper =
                new LambdaQueryWrapper<FileResourceEntity>()
                        .eq(
                                query.getFileType() != null,
                                FileResourceEntity::getFileType,
                                query.getFileType())
                        .eq(
                                query.getUploaderId() != null,
                                FileResourceEntity::getUploaderId,
                                query.getUploaderId());
        if (!isAdmin()) {
            wrapper.eq(FileResourceEntity::getUploaderId, CurrentUserUtils.currentUserId());
        }
        wrapper.orderByDesc(FileResourceEntity::getUploadedAt);
        Page<FileResourceEntity> result = fileResourceMapper.selectPage(page, wrapper);
        List<FileResourceVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result);
    }

    @Override
    public FileResourceVO detail(Long fileId) {
        FileResourceEntity entity = requireFile(fileId);
        requireAuthenticatedAccess(entity);
        return toVO(entity);
    }

    @Override
    public FileContentVO content(Long fileId, String disposition) {
        FileResourceEntity entity = requireFile(fileId);
        requireAuthenticatedAccess(entity);
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
    public FileContentVO publicContent(Long fileId) {
        FileResourceEntity entity = requireFile(fileId);
        if (!"public".equalsIgnoreCase(entity.getAccessLevel())) {
            throw new ResourceNotFoundException("公开文件不存在");
        }
        return fileStorageService.load(
                entity.getStoragePath(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getFileSize());
    }

    @Override
    public Path resolveLocalPath(String fileUrl) {
        Long fileId = resolveFileId(fileUrl);
        if (fileId == null) {
            return fileStorageService.resolve(fileUrl);
        }
        return fileStorageService.resolve(requireFile(fileId).getFileUrl());
    }

    @Override
    public Long resolveFileId(String fileUrl) {
        Long contentId = contentFileId(fileUrl);
        if (contentId != null) {
            return contentId;
        }
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        FileResourceEntity entity =
                fileResourceMapper.selectOne(
                        new LambdaQueryWrapper<FileResourceEntity>()
                                .eq(FileResourceEntity::getFileUrl, fileUrl)
                                .last("limit 1"));
        return entity == null ? null : entity.getId();
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        FileResourceEntity entity = requireFile(fileId);
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (!isAdmin()
                && (currentUserId == null || !currentUserId.equals(entity.getUploaderId()))) {
            throw new ForbiddenException("只能删除本人上传的文件");
        }
        fileBusinessService.deleteByFileId(fileId);
        fileResourceMapper.deleteById(fileId);
        fileStorageService.delete(entity.getStoragePath());
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
        vo.setFileUrl(
                "public".equalsIgnoreCase(entity.getAccessLevel())
                        ? "/api/public-files/" + entity.getId() + "/content"
                        : "/api/files/" + entity.getId() + "/content");
        if (entity.getThumbnailUrl() != null) {
            vo.setThumbnailUrl(vo.getFileUrl());
        }
        return vo;
    }

    private void requireAuthenticatedAccess(FileResourceEntity entity) {
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (isAdmin()
                || "public".equalsIgnoreCase(entity.getAccessLevel())
                || (currentUserId != null && currentUserId.equals(entity.getUploaderId()))) {
            return;
        }
        List<FileBusinessEntity> relations =
                fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, entity.getId()));
        boolean allowed =
                relations.stream()
                        .anyMatch(
                                relation ->
                                        businessReferenceValidator.canAccess(
                                                relation.getBizType(), relation.getBizId()));
        if (!allowed) {
            throw new ForbiddenException("无权访问该文件");
        }
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }

    private Long contentFileId(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        String marker = fileUrl.contains("/public-files/") ? "/public-files/" : "/files/";
        int start = fileUrl.indexOf(marker);
        if (start < 0 || !fileUrl.endsWith("/content")) {
            return null;
        }
        String value =
                fileUrl.substring(
                        start + marker.length(), fileUrl.length() - "/content".length());
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void recordAudit(String operationType, String bizType, Long bizId) {
        AuditRecordDTO audit = new AuditRecordDTO();
        audit.setOperationModule("M05_FILE");
        audit.setOperationType(operationType);
        audit.setBizType(bizType);
        audit.setBizId(bizId);
        try {
            auditLogService.record(audit);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to persist file resource audit log", exception);
        }
    }

    private void bindIfRequested(FileUploadDTO dto, Long fileId) {
        String bizType = firstNonBlank(dto.getBizType(), dto.getBusinessType());
        Long bizId = dto.getBizId() == null ? dto.getBusinessId() : dto.getBizId();
        if (bizType == null || bizId == null) {
            return;
        }
        FileBusinessBindDTO bindDTO = new FileBusinessBindDTO();
        bindDTO.setFileId(fileId);
        bindDTO.setBizType(bizType);
        bindDTO.setBizId(bizId);
        bindDTO.setFileUsage(dto.getFileUsage());
        bindDTO.setRemark(dto.getRemark());
        fileBusinessService.bind(bindDTO);
    }

    private String resolveFileType(FileUploadDTO dto) {
        if (StringUtils.hasText(dto.getFileType())) {
            return dto.getFileType();
        }
        String contentType = dto.getFile().getContentType();
        if (contentType == null) {
            return "file";
        }
        if (contentType.startsWith("image/")) {
            return "image";
        }
        if (contentType.startsWith("video/")) {
            return "video";
        }
        if (contentType.startsWith("audio/")) {
            return "audio";
        }
        return "document";
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
