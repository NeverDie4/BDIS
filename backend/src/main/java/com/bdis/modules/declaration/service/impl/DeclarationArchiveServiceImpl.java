package com.bdis.modules.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.common.security.BusinessReferenceAccessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.declaration.dto.DeclarationArchiveItemRequest;
import com.bdis.modules.declaration.entity.DeclarationArchiveEntity;
import com.bdis.modules.declaration.entity.DeclarationArchiveItemEntity;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.entity.DeclarationMaterialEntity;
import com.bdis.modules.declaration.mapper.DeclarationArchiveItemMapper;
import com.bdis.modules.declaration.mapper.DeclarationArchiveMapper;
import com.bdis.modules.declaration.mapper.DeclarationMapper;
import com.bdis.modules.declaration.mapper.DeclarationMaterialMapper;
import com.bdis.modules.declaration.service.DeclarationArchiveService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DeclarationArchiveServiceImpl implements DeclarationArchiveService {

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DeclarationMapper declarationMapper;

    private final DeclarationMaterialMapper materialMapper;

    private final DeclarationArchiveMapper archiveMapper;

    private final DeclarationArchiveItemMapper archiveItemMapper;
    private final BusinessAccessService accessService;
    private final BusinessReferenceAccessService referenceAccessService;
    private final FileResourceService fileResourceService;

    @Override
    @Transactional
    public DeclarationArchiveEntity generateArchive(Long declarationId) {
        DeclarationEntity declaration = declarationMapper.selectById(declarationId);
        if (declaration == null) {
            throw new IllegalArgumentException("申报档案不存在");
        }
        accessService.requireResourceAccess(
                "eval_application",
                declarationId,
                "declaration:application:archive",
                declaration.getApplicantId());
        if (!"approved".equals(declaration.getReviewStatus())) {
            throw new IllegalArgumentException("只有审核通过的申报可以生成档案");
        }

        DeclarationArchiveEntity archive =
                archiveMapper.selectOne(
                        new LambdaQueryWrapper<DeclarationArchiveEntity>()
                                .eq(DeclarationArchiveEntity::getApplicationId, declarationId));
        if (archive == null) {
            archive = new DeclarationArchiveEntity();
            archive.setArchiveNo(generateNo());
            archive.setApplicationId(declarationId);
            archive.setArchiveTitle(declaration.getApplicationTitle() + "档案袋");
            archive.setOwnerId(declaration.getApplicantId());
            archive.setCreatedBy(accessService.currentUserId());
        }
        archive.setArchiveStatus("generated");
        archive.setGeneratedAt(LocalDateTime.now());
        archive.setStatus(1);
        archive.setUpdatedBy(accessService.currentUserId());
        archive.setRemark("由申报档案生成");

        if (archive.getId() == null) {
            archiveMapper.insert(archive);
        } else {
            archiveMapper.updateById(archive);
        }

        syncMaterialsToArchiveItems(archive.getId(), declarationId);
        return archiveMapper.selectById(archive.getId());
    }

    @Override
    public DeclarationArchiveItemEntity addArchiveItem(
            Long archiveId, DeclarationArchiveItemRequest request) {
        DeclarationArchiveEntity archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new IllegalArgumentException("申报档案袋不存在");
        }
        accessService.requireResourceAccess(
                "eval_application",
                archive.getApplicationId(),
                "declaration:application:archive",
                archive.getOwnerId());
        referenceAccessService.validate(request.getSourceType(), request.getSourceId());
        if ("file".equals(request.getSourceType())
                || "sys_file_resource".equals(request.getSourceType())) {
            fileResourceService.detail(request.getSourceId());
        }
        DeclarationArchiveItemEntity item = new DeclarationArchiveItemEntity();
        item.setArchiveId(archiveId);
        item.setSourceType(request.getSourceType());
        item.setSourceId(request.getSourceId());
        item.setItemName(defaultText(request.getItemName(), request.getSourceType()));
        item.setItemDesc(request.getItemDesc());
        item.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        item.setCreatedBy(accessService.currentUserId());
        item.setRemark(request.getRemark());
        archiveItemMapper.insert(item);
        return archiveItemMapper.selectById(item.getId());
    }

    private void syncMaterialsToArchiveItems(Long archiveId, Long declarationId) {
        List<DeclarationMaterialEntity> materials =
                materialMapper.selectList(
                        new LambdaQueryWrapper<DeclarationMaterialEntity>()
                                .eq(DeclarationMaterialEntity::getApplicationId, declarationId));
        for (DeclarationMaterialEntity material : materials) {
            Long count =
                    archiveItemMapper.selectCount(
                            new LambdaQueryWrapper<DeclarationArchiveItemEntity>()
                                    .eq(DeclarationArchiveItemEntity::getArchiveId, archiveId)
                                    .eq(DeclarationArchiveItemEntity::getSourceType, "attachment")
                                    .eq(
                                            DeclarationArchiveItemEntity::getSourceId,
                                            material.getId()));
            if (count > 0) {
                continue;
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

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "ARCH-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
