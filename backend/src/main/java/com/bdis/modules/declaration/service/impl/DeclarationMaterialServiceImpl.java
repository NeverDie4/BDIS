package com.bdis.modules.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.declaration.dto.DeclarationMaterialRequest;
import com.bdis.modules.declaration.entity.DeclarationArchiveEntity;
import com.bdis.modules.declaration.entity.DeclarationArchiveItemEntity;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.entity.DeclarationMaterialEntity;
import com.bdis.modules.declaration.mapper.DeclarationArchiveItemMapper;
import com.bdis.modules.declaration.mapper.DeclarationArchiveMapper;
import com.bdis.modules.declaration.mapper.DeclarationMapper;
import com.bdis.modules.declaration.mapper.DeclarationMaterialMapper;
import com.bdis.modules.declaration.service.DeclarationMaterialService;
import com.bdis.modules.file.vo.FileResourceVO;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeclarationMaterialServiceImpl implements DeclarationMaterialService {

    private final DeclarationMapper declarationMapper;

    private final DeclarationMaterialMapper materialMapper;

    private final DeclarationArchiveMapper archiveMapper;

    private final DeclarationArchiveItemMapper archiveItemMapper;
    private final BusinessAccessService accessService;
    private final FileResourceService fileResourceService;
    private final FileBusinessService fileBusinessService;

    @Override
    @Transactional
    public DeclarationMaterialEntity addMaterial(
            Long declarationId, DeclarationMaterialRequest request) {
        DeclarationEntity declaration = declarationMapper.selectById(declarationId);
        if (declaration == null) {
            throw new IllegalArgumentException("申报档案不存在");
        }
        accessService.requireResourceAccess(
                "eval_application",
                declarationId,
                "declaration:application:update",
                declaration.getApplicantId());
        if (!"draft".equals(declaration.getReviewStatus())
                && !"rejected".equals(declaration.getReviewStatus())) {
            throw new IllegalArgumentException("只有草稿或退回状态的申报可以添加材料");
        }
        Long uploaderId = accessService.currentUserId();
        FileResourceVO file = fileResourceService.detail(request.getFileId());

        FileBusinessBindDTO bind = new FileBusinessBindDTO();
        bind.setFileId(request.getFileId());
        bind.setBizType("eval_application");
        bind.setBizId(declarationId);
        bind.setFileUsage("application_material");
        bind.setRemark(request.getRemark());
        fileBusinessService.bind(bind);

        DeclarationMaterialEntity entity = new DeclarationMaterialEntity();
        entity.setApplicationId(declarationId);
        entity.setFileId(request.getFileId());
        entity.setFileName(file.getOriginalFilename());
        entity.setFileType(file.getFileType());
        entity.setFileUrl(file.getFileUrl());
        entity.setFileSize(file.getFileSize());
        entity.setUploaderId(uploaderId);
        entity.setUploadedAt(LocalDateTime.now());
        entity.setStatus(1);
        entity.setCreatedBy(uploaderId);
        entity.setRemark(request.getRemark());
        materialMapper.insert(entity);

        DeclarationArchiveEntity archive =
                archiveMapper.selectOne(
                        new LambdaQueryWrapper<DeclarationArchiveEntity>()
                                .eq(DeclarationArchiveEntity::getApplicationId, declarationId));
        if (archive != null) {
            addArchiveItemIfAbsent(archive.getId(), entity);
        }

        return materialMapper.selectById(entity.getId());
    }

    private void addArchiveItemIfAbsent(Long archiveId, DeclarationMaterialEntity material) {
        Long count =
                archiveItemMapper.selectCount(
                        new LambdaQueryWrapper<DeclarationArchiveItemEntity>()
                                .eq(DeclarationArchiveItemEntity::getArchiveId, archiveId)
                                .eq(DeclarationArchiveItemEntity::getSourceType, "attachment")
                                .eq(DeclarationArchiveItemEntity::getSourceId, material.getId()));
        if (count > 0) {
            return;
        }
        DeclarationArchiveItemEntity item = new DeclarationArchiveItemEntity();
        item.setArchiveId(archiveId);
        item.setSourceType("attachment");
        item.setSourceId(material.getId());
        item.setItemName(material.getFileName());
        item.setItemDesc(material.getRemark());
        item.setSortOrder(0);
        item.setCreatedBy(material.getUploaderId());
        archiveItemMapper.insert(item);
    }
}
