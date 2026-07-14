package com.bdis.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.policy.FileBusinessAction;
import com.bdis.file.policy.FileBusinessPolicyRegistry;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.vo.FileResourceVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileBusinessServiceImpl implements FileBusinessService {

    private final FileBusinessMapper fileBusinessMapper;
    private final FileResourceMapper fileResourceMapper;
    private final FileBusinessPolicyRegistry policyRegistry;
    private final FileAccessGuard fileAccessGuard;

    public FileBusinessServiceImpl(
            FileBusinessMapper fileBusinessMapper,
            FileResourceMapper fileResourceMapper,
            FileBusinessPolicyRegistry policyRegistry,
            FileAccessGuard fileAccessGuard) {
        this.fileBusinessMapper = fileBusinessMapper;
        this.fileResourceMapper = fileResourceMapper;
        this.policyRegistry = policyRegistry;
        this.fileAccessGuard = fileAccessGuard;
    }

    @Override
    @Transactional
    public FileBusinessVO bind(FileBusinessBindDTO dto) {
        policyRegistry.require(dto.getBizType(), dto.getBizId(), FileBusinessAction.ATTACH);
        FileResourceEntity file = fileResourceMapper.selectById(dto.getFileId());
        if (file == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        if (!Integer.valueOf(1).equals(file.getStatus())) {
            throw new ResourceNotFoundException("文件已停用");
        }
        fileAccessGuard.requireAuthenticatedAccess(file);
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
        entity.setPublicVisible(false);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setRemark(dto.getRemark());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        fileBusinessMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public FileBusinessVO bindSystem(FileBusinessBindDTO dto) {
        return bind(dto);
    }

    @Override
    @Transactional
    public void setPublicVisibility(
            Long fileId, String bizType, Long bizId, boolean publicVisible) {
        policyRegistry.require(bizType, bizId, FileBusinessAction.PUBLISH);
        FileBusinessEntity relation =
                fileBusinessMapper.selectOne(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, fileId)
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId));
        if (relation == null) {
            throw new ResourceConflictException("文件尚未绑定到该业务对象");
        }
        relation.setPublicVisible(publicVisible);
        fileBusinessMapper.updateById(relation);
        synchronizeResourceVisibility(fileId);
    }

    @Override
    public void authorizeDeleteByFileId(Long fileId) {
        List<FileBusinessEntity> relations =
                fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, fileId));
        for (FileBusinessEntity relation : relations) {
            authorizeDetach(relation);
        }
    }

    @Override
    @Transactional
    public void unbind(Long relationId) {
        FileBusinessEntity entity = fileBusinessMapper.selectById(relationId);
        if (entity == null) {
            throw new ResourceNotFoundException("文件关联不存在");
        }
        authorizeDetach(entity);
        fileBusinessMapper.deleteById(relationId);
        synchronizeResourceVisibility(entity.getFileId());
    }

    @Override
    @Transactional
    public void unbind(String bizType, Long bizId, Long fileId) {
        FileBusinessEntity entity =
                fileBusinessMapper.selectOne(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId)
                                .eq(FileBusinessEntity::getFileId, fileId));
        if (entity == null) {
            throw new ResourceNotFoundException("文件关联不存在");
        }
        unbind(entity.getId());
    }

    @Override
    public void deleteByFileId(Long fileId) {
        fileBusinessMapper.delete(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getFileId, fileId));
    }

    @Override
    @Transactional
    public void deleteByBusiness(String bizType, Long bizId) {
        List<FileBusinessEntity> relations =
                fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId));
        relations.forEach(this::authorizeDetach);
        List<Long> fileIds =
                relations.stream().map(FileBusinessEntity::getFileId).distinct().sorted().toList();
        fileBusinessMapper.delete(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getBizType, bizType)
                        .eq(FileBusinessEntity::getBizId, bizId));
        fileIds.forEach(this::synchronizeResourceVisibility);
    }

    @Override
    @Transactional
    public void deleteByBusinessAndFile(String bizType, Long bizId, Long fileId) {
        FileBusinessEntity relation =
                fileBusinessMapper.selectOne(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId)
                                .eq(FileBusinessEntity::getFileId, fileId));
        if (relation == null) {
            return;
        }
        authorizeDetach(relation);
        fileBusinessMapper.delete(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getBizType, bizType)
                        .eq(FileBusinessEntity::getBizId, bizId)
                        .eq(FileBusinessEntity::getFileId, fileId));
        synchronizeResourceVisibility(fileId);
    }

    @Override
    public boolean isBound(Long fileId, String bizType, Long bizId) {
        return fileBusinessMapper.selectCount(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, fileId)
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId))
                > 0;
    }

    @Override
    public boolean existsByBusiness(String bizType, Long bizId) {
        return fileBusinessMapper.selectCount(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId))
                > 0;
    }

    @Override
    public PageResult<FileResourceVO> pageByBusiness(
            String bizType, Long bizId, long requestedPage, long requestedSize) {
        policyRegistry.require(bizType, bizId, FileBusinessAction.VIEW);
        long page = Math.max(1, requestedPage);
        long size = Math.max(1, requestedSize);
        long total = fileBusinessMapper.countActiveFiles(bizType, bizId);
        long pageIndex = page - 1;
        long offset = pageIndex > total / size ? total : pageIndex * size;
        List<FileResourceVO> records =
                fileBusinessMapper.selectActiveFiles(bizType, bizId, offset, size).stream()
                        .map(this::toFileVO)
                        .toList();
        return new PageResult<>(records, page, size, total);
    }

    @Override
    public List<FileResourceVO> listByBusiness(String bizType, Long bizId) {
        policyRegistry.require(bizType, bizId, FileBusinessAction.VIEW);
        return fileBusinessMapper
                .selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId)
                                .orderByAsc(FileBusinessEntity::getSortOrder)
                                .orderByAsc(FileBusinessEntity::getCreatedAt)
                                .orderByAsc(FileBusinessEntity::getId))
                .stream()
                .map(FileBusinessEntity::getFileId)
                .map(fileResourceMapper::selectById)
                .filter(entity -> entity != null && Integer.valueOf(1).equals(entity.getStatus()))
                .map(this::toFileVO)
                .toList();
    }

    @Override
    public List<FileResourceVO> listByBusiness(String bizType, Long bizId, String fileUsage) {
        policyRegistry.require(bizType, bizId, FileBusinessAction.VIEW);
        return fileBusinessMapper
                .selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, bizType)
                                .eq(FileBusinessEntity::getBizId, bizId)
                                .eq(
                                        fileUsage != null && !fileUsage.isBlank(),
                                        FileBusinessEntity::getFileUsage,
                                        fileUsage)
                                .orderByAsc(FileBusinessEntity::getSortOrder)
                                .orderByAsc(FileBusinessEntity::getCreatedAt)
                                .orderByAsc(FileBusinessEntity::getId))
                .stream()
                .map(FileBusinessEntity::getFileId)
                .map(fileResourceMapper::selectById)
                .filter(entity -> entity != null && Integer.valueOf(1).equals(entity.getStatus()))
                .map(this::toFileVO)
                .toList();
    }

    @Override
    public List<FileBusinessVO> listBindingsByBusiness(String bizType, Long bizId) {
        policyRegistry.require(bizType, bizId, FileBusinessAction.VIEW);
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

    private void synchronizeResourceVisibility(Long fileId) {
        FileResourceEntity file = fileResourceMapper.selectByIdForUpdate(fileId);
        if (file == null) {
            return;
        }
        boolean publicVisible =
                fileBusinessMapper.selectCount(
                                new LambdaQueryWrapper<FileBusinessEntity>()
                                        .eq(FileBusinessEntity::getFileId, fileId)
                                        .eq(FileBusinessEntity::getPublicVisible, true))
                        > 0;
        String accessLevel = publicVisible ? "public" : "private";
        String contentUrl =
                publicVisible
                        ? "/api/public-files/" + fileId + "/content"
                        : "/api/files/" + fileId + "/content";
        boolean unchanged =
                accessLevel.equalsIgnoreCase(file.getAccessLevel())
                        && contentUrl.equals(file.getFileUrl())
                        && (!("image".equals(file.getFileType()))
                                || contentUrl.equals(file.getThumbnailUrl()));
        if (unchanged) {
            return;
        }
        file.setAccessLevel(accessLevel);
        file.setFileUrl(contentUrl);
        file.setThumbnailUrl("image".equals(file.getFileType()) ? contentUrl : null);
        file.setUpdatedAt(LocalDateTime.now());
        file.setUpdatedBy(CurrentUserUtils.currentUserId());
        fileResourceMapper.updateById(file);
    }

    private void authorizeDetach(FileBusinessEntity relation) {
        policyRegistry.require(
                relation.getBizType(), relation.getBizId(), FileBusinessAction.DETACH);
        if (Boolean.TRUE.equals(relation.getPublicVisible())) {
            policyRegistry.require(
                    relation.getBizType(), relation.getBizId(), FileBusinessAction.PUBLISH);
        }
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
}
