package com.bdis.modules.herb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.support.ImageContentValidator;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.herb.service.HerbSpeciesCoverFileService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HerbSpeciesCoverFileServiceImpl implements HerbSpeciesCoverFileService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HerbSpeciesCoverFileServiceImpl.class);
    private static final String BIZ_TYPE = "herb_species";
    private static final String FILE_USAGE = "cover";

    private final FileResourceService fileResourceService;
    private final FileBusinessService fileBusinessService;
    private final FileResourceMapper fileResourceMapper;
    private final FileBusinessMapper fileBusinessMapper;
    private final ImageContentValidator imageContentValidator;

    @Override
    @Transactional
    public String replaceCover(Long speciesId, String previousFileUrl, String requestedFileUrl) {
        Long previousFileId =
                StringUtils.hasText(previousFileUrl)
                        ? fileResourceService.resolveFileId(previousFileUrl)
                        : null;
        Long requestedFileId =
                StringUtils.hasText(requestedFileUrl)
                        ? fileResourceService.resolveFileId(requestedFileUrl)
                        : null;

        if (!StringUtils.hasText(requestedFileUrl)) {
            releaseIfBound(speciesId, previousFileId);
            return null;
        }
        if (requestedFileId == null) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "药材封面必须使用文件上传模块返回的图片");
        }

        String publicUrl = publishAndBind(speciesId, requestedFileId);
        if (!Objects.equals(previousFileId, requestedFileId)) {
            releaseIfBound(speciesId, previousFileId);
        }
        return publicUrl;
    }

    @Override
    @Transactional
    public void deleteCover(Long speciesId, String fileUrl) {
        Long fileId =
                StringUtils.hasText(fileUrl) ? fileResourceService.resolveFileId(fileUrl) : null;
        releaseIfBound(speciesId, fileId);
    }

    private String publishAndBind(Long speciesId, Long fileId) {
        FileResourceEntity file = fileResourceMapper.selectById(fileId);
        if (file == null) {
            throw new ResourceNotFoundException("药材封面文件不存在");
        }
        List<FileBusinessEntity> bindings = bindings(fileId);
        boolean alreadyBound =
                bindings.stream()
                        .anyMatch(
                                binding ->
                                        BIZ_TYPE.equals(binding.getBizType())
                                                && speciesId.equals(binding.getBizId())
                                                && FILE_USAGE.equals(binding.getFileUsage()));
        if (!alreadyBound) {
            if (!bindings.isEmpty()) {
                throw new BusinessException(ResultCodeEnum.CONFLICT, "该文件已绑定到其他业务对象");
            }
            if (!Objects.equals(CurrentUserUtils.currentUserId(), file.getUploaderId())) {
                throw new ForbiddenException("只能使用本人上传的药材封面");
            }
            if (!"private".equalsIgnoreCase(file.getAccessLevel())) {
                throw new BusinessException(ResultCodeEnum.CONFLICT, "未绑定的药材封面必须保持私有状态");
            }
        }

        imageContentValidator.requireAllowedImage(
                fileResourceService.resolveLocalPath("/api/files/" + file.getId() + "/content"),
                file.getOriginalFilename());
        if (!alreadyBound) {
            FileBusinessBindDTO bind = new FileBusinessBindDTO();
            bind.setFileId(fileId);
            bind.setBizType(BIZ_TYPE);
            bind.setBizId(speciesId);
            bind.setFileUsage(FILE_USAGE);
            fileBusinessService.bind(bind);
        }
        fileResourceService.publishForBusiness(fileId, BIZ_TYPE, speciesId);
        return publicUrl(fileId);
    }

    private void releaseIfBound(Long speciesId, Long fileId) {
        if (fileId == null) {
            return;
        }
        long bindingCount =
                fileBusinessMapper.selectCount(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, fileId)
                                .eq(FileBusinessEntity::getBizType, BIZ_TYPE)
                                .eq(FileBusinessEntity::getBizId, speciesId)
                                .eq(FileBusinessEntity::getFileUsage, FILE_USAGE));
        if (bindingCount == 0) {
            return;
        }

        fileBusinessService.deleteByBusinessAndFile(BIZ_TYPE, speciesId, fileId);
        long remainingBindings =
                fileBusinessMapper.selectCount(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, fileId));
        if (remainingBindings == 0) {
            deleteAfterCommit(fileId);
        }
    }

    private List<FileBusinessEntity> bindings(Long fileId) {
        return fileBusinessMapper.selectList(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getFileId, fileId));
    }

    private void deleteAfterCommit(Long fileId) {
        Runnable cleanup =
                () -> {
                    try {
                        fileResourceService.deleteSystem(fileId);
                    } catch (RuntimeException exception) {
                        LOGGER.warn(
                                "Failed to delete released herb species cover after commit: {}",
                                fileId,
                                exception);
                    }
                };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanup.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cleanup.run();
                    }
                });
    }

    private String publicUrl(Long fileId) {
        return "/api/public-files/" + fileId + "/content";
    }
}
