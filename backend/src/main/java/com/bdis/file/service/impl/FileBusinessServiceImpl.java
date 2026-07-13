package com.bdis.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.core.PageResult;
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
        entity.setRemark(dto.getRemark());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        fileBusinessMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public void authorizeDeleteByFileId(Long fileId, boolean published) {
        List<FileBusinessEntity> relations =
                fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, fileId));
        for (FileBusinessEntity relation : relations) {
            policyRegistry.require(
                    relation.getBizType(), relation.getBizId(), FileBusinessAction.DETACH);
            if (published) {
                policyRegistry.require(
                        relation.getBizType(), relation.getBizId(), FileBusinessAction.PUBLISH);
            }
        }
    }

    @Override
    @Transactional
    public void unbind(Long relationId) {
        FileBusinessEntity entity = fileBusinessMapper.selectById(relationId);
        if (entity == null) {
            throw new ResourceNotFoundException("文件关联不存在");
        }
        policyRegistry.require(entity.getBizType(), entity.getBizId(), FileBusinessAction.DETACH);
        fileBusinessMapper.deleteById(relationId);
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
    public boolean isBound(Long fileId, String bizType, Long bizId) {
        return fileBusinessMapper.selectCount(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, fileId)
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
}
