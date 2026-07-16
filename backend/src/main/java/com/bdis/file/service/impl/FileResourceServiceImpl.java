package com.bdis.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.FileStorageException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.vo.FileResourceVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileResourceServiceImpl implements FileResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileResourceServiceImpl.class);
    private static final String PENDING_PRIVATE_URL = "/api/files/0/content";

    private final FileResourceMapper fileResourceMapper;
    private final FileStorageService fileStorageService;
    private final FileAccessLogService fileAccessLogService;
    private final AuditLogService auditLogService;
    private final FileBusinessService fileBusinessService;
    private final FileAccessGuard fileAccessGuard;

    public FileResourceServiceImpl(
            FileResourceMapper fileResourceMapper,
            FileStorageService fileStorageService,
            FileAccessLogService fileAccessLogService,
            AuditLogService auditLogService,
            FileBusinessService fileBusinessService,
            FileAccessGuard fileAccessGuard) {
        this.fileResourceMapper = fileResourceMapper;
        this.fileStorageService = fileStorageService;
        this.fileAccessLogService = fileAccessLogService;
        this.auditLogService = auditLogService;
        this.fileBusinessService = fileBusinessService;
        this.fileAccessGuard = fileAccessGuard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResourceVO upload(FileUploadDTO dto) {
        requirePrivateUpload(dto);
        FileStorageService.StoredFile storedFile = fileStorageService.save(dto.getFile());
        registerRollbackCleanup(storedFile.storagePath());
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
            entity.setFileUrl(PENDING_PRIVATE_URL);
            entity.setThumbnailUrl(null);
            entity.setStoragePath(storedFile.storagePath());
            entity.setStorageType("local");
            entity.setAccessLevel("private");
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
            applyControlledUrls(entity);
            fileResourceMapper.updateById(entity);
            bindIfRequested(dto, entity.getId());
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
    public FileResourceVO uploadOwnedPrivateImage(MultipartFile file, String remark) {
        FileUploadDTO dto = new FileUploadDTO();
        dto.setFile(file);
        dto.setFileType("image");
        dto.setAccessLevel("private");
        dto.setRemark(remark);
        return upload(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResourceVO importPrivate(Path sourceFile, String originalFilename, String remark) {
        FileStorageService.StoredFile storedFile = fileStorageService.save(sourceFile);
        registerRollbackCleanup(storedFile.storagePath());
        try {
            Path storedPath = fileStorageService.resolve(storedFile.storagePath());
            return createImportedPrivateResource(storedFile, storedPath, originalFilename, remark);
        } catch (RuntimeException exception) {
            fileStorageService.delete(storedFile.storagePath());
            throw exception;
        }
    }

    @Override
    public PageResult<FileResourceVO> page(FileResourceQuery query) {
        String bizType = firstNonBlank(query.getBizType(), query.getBusinessType());
        Long bizId = query.getBizId() == null ? query.getBusinessId() : query.getBizId();
        if (bizType != null && bizId != null) {
            return fileBusinessService.pageByBusiness(
                    bizType, bizId, query.getPage(), query.getSize());
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
        fileAccessGuard.requireAuthenticatedAccess(entity);
        return toVO(entity);
    }

    @Override
    public FileContentVO content(Long fileId, String disposition) {
        String accessType = "attachment".equalsIgnoreCase(disposition) ? "DOWNLOAD" : "PREVIEW";
        try {
            FileResourceEntity entity = requireFile(fileId);
            fileAccessGuard.requireAuthenticatedAccess(entity);
            FileContentVO content =
                    fileStorageService.load(
                            entity.getStoragePath(),
                            entity.getOriginalFilename(),
                            entity.getContentType(),
                            entity.getFileSize());
            recordFileAccess(fileId, accessType, "SUCCESS", null);
            recordAudit(accessType, "file_resource", fileId, "SUCCESS", null);
            return content;
        } catch (RuntimeException exception) {
            recordFileAccess(fileId, accessType, "FAILED", exception.getMessage());
            recordAudit(accessType, "file_resource", fileId, "FAILED", exception.getMessage());
            throw exception;
        }
    }

    @Override
    public FileContentVO publicContent(Long fileId) {
        try {
            FileResourceEntity entity = requireFile(fileId);
            if (!"public".equalsIgnoreCase(entity.getAccessLevel())) {
                throw new ResourceNotFoundException("公开文件不存在");
            }
            FileContentVO content = loadContent(entity);
            recordFileAccess(fileId, "PREVIEW", "SUCCESS", null);
            return content;
        } catch (RuntimeException exception) {
            recordFileAccess(fileId, "PREVIEW", "FAILED", exception.getMessage());
            throw exception;
        }
    }

    @Override
    public FileContentVO internalContent(Long fileId) {
        return loadContent(requireFile(fileId));
    }

    private FileContentVO loadContent(FileResourceEntity entity) {
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
            throw new ResourceNotFoundException("文件地址不是受控内容地址");
        }
        return fileStorageService.resolve(requireFile(fileId).getStoragePath());
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
        if (fileUrl.contains("/files/uploads/")) {
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
    public void publishForBusiness(Long fileId, String bizType, Long bizId) {
        changeBusinessVisibility(fileId, bizType, bizId, "public");
    }

    @Override
    @Transactional
    public void makePrivateForBusiness(Long fileId, String bizType, Long bizId) {
        changeBusinessVisibility(fileId, bizType, bizId, "private");
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
        fileBusinessService.authorizeDeleteByFileId(fileId);
        deleteEntity(entity);
    }

    @Override
    @Transactional
    public void deleteOwnUnboundUpload(Long fileId) {
        FileResourceEntity entity = requireFile(fileId);
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (currentUserId == null || !currentUserId.equals(entity.getUploaderId())) {
            throw new ForbiddenException("只能清理本人上传的临时文件");
        }
        if (!"private".equalsIgnoreCase(entity.getAccessLevel())) {
            throw new ForbiddenException("已发布文件不能作为临时上传清理");
        }
        long bindingCount = fileBusinessService.countByFileId(fileId);
        if (bindingCount != 0) {
            throw new ForbiddenException("已绑定业务的文件不能作为临时上传清理");
        }
        deleteEntity(entity);
    }

    @Override
    @Transactional
    public void deleteSystem(Long fileId) {
        deleteEntity(requireFile(fileId));
    }

    private FileResourceVO createImportedPrivateResource(
            FileStorageService.StoredFile storedFile,
            Path storedPath,
            String originalFilename,
            String remark) {
        try {
            String safeOriginalFilename =
                    StringUtils.hasText(originalFilename)
                            ? originalFilename
                            : storedPath.getFileName().toString();
            String extension = StringUtils.getFilenameExtension(safeOriginalFilename);
            String contentType = Files.probeContentType(storedPath);
            FileResourceEntity entity = new FileResourceEntity();
            entity.setFileNo("FILE-" + UUID.randomUUID());
            entity.setFileName(storedFile.storedName());
            entity.setOriginalFilename(safeOriginalFilename);
            entity.setFileType(
                    contentType != null && contentType.startsWith("image/") ? "image" : "file");
            entity.setFileFormat(extension);
            entity.setFileSize(Files.size(storedPath));
            entity.setFileUrl(PENDING_PRIVATE_URL);
            entity.setThumbnailUrl(null);
            entity.setStoragePath(storedFile.storagePath());
            entity.setStorageType("local");
            entity.setAccessLevel("private");
            entity.setContentType(contentType);
            entity.setUploaderId(CurrentUserUtils.currentUserId());
            entity.setUploaderName(CurrentUserUtils.currentUsername());
            entity.setUploadedAt(LocalDateTime.now());
            entity.setStatus(1);
            entity.setIsDeleted(0);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setRemark(remark);
            entity.setVersion(0);
            fileResourceMapper.insert(entity);
            applyControlledUrls(entity);
            fileResourceMapper.updateById(entity);
            recordAudit("IMPORT_PRIVATE", "file_resource", entity.getId());
            return toVO(entity);
        } catch (IOException exception) {
            throw new FileStorageException("读取导入文件信息失败", exception);
        }
    }

    private void deleteEntity(FileResourceEntity entity) {
        fileBusinessService.deleteByFileId(entity.getId());
        fileResourceMapper.deleteById(entity.getId());
        registerAfterCommitDelete(entity.getStoragePath());
    }

    private FileResourceEntity requireFile(Long fileId) {
        FileResourceEntity entity = fileResourceMapper.selectById(fileId);
        if (entity == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getStatus())) {
            throw new ResourceNotFoundException("文件已停用");
        }
        return entity;
    }

    private void changeBusinessVisibility(
            Long fileId, String bizType, Long bizId, String accessLevel) {
        requireFile(fileId);
        fileBusinessService.setPublicVisibility(
                fileId, bizType, bizId, "public".equals(accessLevel));
        recordAudit("public".equals(accessLevel) ? "PUBLISH" : "MAKE_PRIVATE", bizType, bizId);
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

    private void applyControlledUrls(FileResourceEntity entity) {
        String contentUrl =
                "public".equalsIgnoreCase(entity.getAccessLevel())
                        ? "/api/public-files/" + entity.getId() + "/content"
                        : "/api/files/" + entity.getId() + "/content";
        entity.setFileUrl(contentUrl);
        entity.setThumbnailUrl("image".equals(entity.getFileType()) ? contentUrl : null);
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
                fileUrl.substring(start + marker.length(), fileUrl.length() - "/content".length());
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void recordAudit(String operationType, String bizType, Long bizId) {
        recordAudit(operationType, bizType, bizId, "SUCCESS", null);
    }

    private void recordAudit(
            String operationType,
            String bizType,
            Long bizId,
            String operationResult,
            String errorMessage) {
        AuditRecordDTO audit = new AuditRecordDTO();
        audit.setOperationModule("M05_FILE");
        audit.setOperationType(operationType);
        audit.setBizType(bizType);
        audit.setBizId(bizId);
        audit.setOperationResult(operationResult);
        audit.setErrorMessage(errorMessage);
        try {
            auditLogService.record(audit);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to persist file resource audit log", exception);
        }
    }

    private void recordFileAccess(
            Long fileId, String accessType, String accessResult, String failureReason) {
        FileAccessRecordDTO record = new FileAccessRecordDTO();
        record.setFileId(fileId);
        record.setAccessType(accessType);
        record.setAccessResult(accessResult);
        record.setFailureReason(failureReason);
        fileAccessLogService.record(record);
    }

    private void bindIfRequested(FileUploadDTO dto, Long fileId) {
        String bizType = firstNonBlank(dto.getBizType(), dto.getBusinessType());
        Long bizId = dto.getBizId() == null ? dto.getBusinessId() : dto.getBizId();
        if (bizType == null && bizId == null) {
            return;
        }
        if (bizType == null || bizId == null) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务类型和业务 ID 必须同时提供");
        }
        FileBusinessBindDTO bindDTO = new FileBusinessBindDTO();
        bindDTO.setFileId(fileId);
        bindDTO.setBizType(bizType);
        bindDTO.setBizId(bizId);
        bindDTO.setFileUsage(dto.getFileUsage());
        bindDTO.setRemark(dto.getRemark());
        fileBusinessService.bind(bindDTO);
    }

    private void requirePrivateUpload(FileUploadDTO dto) {
        if (StringUtils.hasText(dto.getAccessLevel())
                && !"private".equalsIgnoreCase(dto.getAccessLevel())) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "上传文件必须先保存为私有文件");
        }
        String bizType = firstNonBlank(dto.getBizType(), dto.getBusinessType());
        Long bizId = dto.getBizId() == null ? dto.getBusinessId() : dto.getBizId();
        if ((bizType == null) != (bizId == null)) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务类型和业务 ID 必须同时提供");
        }
    }

    private void registerRollbackCleanup(String storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            deleteStorageSafely(storagePath, "rollback cleanup");
                        }
                    }
                });
    }

    private void registerAfterCommitDelete(String storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteStorageSafely(storagePath, "immediate delete");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deleteStorageSafely(storagePath, "after-commit delete");
                    }
                });
    }

    private void deleteStorageSafely(String storagePath, String operation) {
        try {
            fileStorageService.delete(storagePath);
        } catch (RuntimeException exception) {
            LOGGER.error("File storage {} failed for {}", operation, storagePath, exception);
        }
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
