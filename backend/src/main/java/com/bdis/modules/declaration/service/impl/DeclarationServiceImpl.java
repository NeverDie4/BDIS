package com.bdis.modules.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.declaration.dto.DeclarationRequest;
import com.bdis.modules.declaration.dto.DeclarationReviewRequest;
import com.bdis.modules.declaration.entity.DeclarationArchiveEntity;
import com.bdis.modules.declaration.entity.DeclarationArchiveItemEntity;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.entity.DeclarationMaterialEntity;
import com.bdis.modules.declaration.entity.DeclarationReviewRecordEntity;
import com.bdis.modules.declaration.mapper.DeclarationArchiveItemMapper;
import com.bdis.modules.declaration.mapper.DeclarationArchiveMapper;
import com.bdis.modules.declaration.mapper.DeclarationMapper;
import com.bdis.modules.declaration.mapper.DeclarationMaterialMapper;
import com.bdis.modules.declaration.mapper.DeclarationReviewRecordMapper;
import com.bdis.modules.declaration.query.DeclarationQuery;
import com.bdis.modules.declaration.service.DeclarationArchiveService;
import com.bdis.modules.declaration.service.DeclarationService;
import com.bdis.modules.declaration.vo.DeclarationDetailVO;
import com.bdis.modules.declaration.vo.DeclarationSummaryVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DeclarationServiceImpl implements DeclarationService {

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> REVIEW_STATUSES =
            Set.of("draft", "submitted", "approved", "rejected", "archived");

    private final DeclarationMapper declarationMapper;
    private final DeclarationReviewRecordMapper reviewRecordMapper;
    private final DeclarationMaterialMapper materialMapper;
    private final DeclarationArchiveMapper archiveMapper;
    private final DeclarationArchiveItemMapper archiveItemMapper;
    private final DeclarationArchiveService archiveService;
    private final BusinessAccessService accessService;

    @Override
    public IPage<DeclarationEntity> listDeclarations(DeclarationQuery query) {
        DeclarationQuery safeQuery = query == null ? new DeclarationQuery() : query;
        LambdaQueryWrapper<DeclarationEntity> wrapper =
                new LambdaQueryWrapper<DeclarationEntity>()
                        .like(
                                StringUtils.hasText(safeQuery.getKeyword()),
                                DeclarationEntity::getApplicationTitle,
                                safeQuery.getKeyword())
                        .eq(
                                StringUtils.hasText(safeQuery.getApplicationType()),
                                DeclarationEntity::getApplicationType,
                                safeQuery.getApplicationType())
                        .eq(
                                StringUtils.hasText(safeQuery.getStatus()),
                                DeclarationEntity::getReviewStatus,
                                safeQuery.getStatus())
                        .eq(
                                safeQuery.getApplicantId() != null,
                                DeclarationEntity::getApplicantId,
                                safeQuery.getApplicantId())
                        .orderByDesc(DeclarationEntity::getUpdatedAt);
        accessService.requirePermission("declaration:application:view");
        accessService.applyOwnerScope(
                wrapper, "eval_application", DeclarationEntity::getApplicantId);
        return declarationMapper.selectPage(
                page(safeQuery.getPageNum(), safeQuery.getPageSize()), wrapper);
    }

    @Override
    public DeclarationEntity createDeclaration(DeclarationRequest request) {
        accessService.requirePermission("declaration:application:create");
        Long applicantId = accessService.currentUserId();
        DeclarationEntity entity = new DeclarationEntity();
        entity.setApplicationNo(defaultText(request.getApplicationNo(), generateNo("DECL")));
        entity.setApplicationTitle(request.getApplicationTitle());
        entity.setApplicationType(request.getApplicationType());
        entity.setApplicantId(applicantId);
        entity.setReviewStatus("draft");
        entity.setStatus(1);
        entity.setRemark(request.getRemark());
        entity.setCreatedBy(applicantId);
        declarationMapper.insert(entity);
        return declarationMapper.selectById(entity.getId());
    }

    @Override
    public DeclarationDetailVO getDeclarationDetail(Long declarationId) {
        DeclarationEntity declaration = findDeclaration(declarationId);
        accessService.requireResourceAccess(
                "eval_application",
                declarationId,
                "declaration:application:view",
                declaration.getApplicantId());
        DeclarationArchiveEntity archive = findArchiveByDeclarationId(declarationId);
        DeclarationDetailVO detail = new DeclarationDetailVO();
        detail.setDeclaration(declaration);
        detail.setMaterials(listMaterials(declarationId));
        detail.setReviewRecords(listReviewRecords(declarationId));
        detail.setArchive(archive);
        detail.setArchiveItems(archive == null ? List.of() : listArchiveItems(archive.getId()));
        return detail;
    }

    @Override
    @Transactional
    public DeclarationEntity submitDeclaration(Long declarationId) {
        DeclarationEntity declaration = findDeclaration(declarationId);
        Long applicantId = accessService.currentUserId();
        accessService.requireResourceAccess(
                "eval_application",
                declarationId,
                "declaration:application:submit",
                declaration.getApplicantId());
        if (!"draft".equals(declaration.getReviewStatus())
                && !"rejected".equals(declaration.getReviewStatus())) {
            throw new IllegalArgumentException("只有草稿或退回状态的申报可以提交");
        }
        if (applicantId == null || !applicantId.equals(declaration.getApplicantId())) {
            throw new IllegalArgumentException("提交人必须与申报人一致");
        }
        String beforeStatus = declaration.getReviewStatus();
        declaration.setReviewStatus("submitted");
        declaration.setSubmittedAt(LocalDateTime.now());
        declaration.setUpdatedBy(applicantId);
        declarationMapper.updateById(declaration);
        saveReviewRecord(
                declarationId, applicantId, "submit", beforeStatus, "submitted", "申报提交", null);
        return declarationMapper.selectById(declarationId);
    }

    @Override
    @Transactional
    public DeclarationReviewRecordEntity reviewDeclaration(
            Long declarationId, DeclarationReviewRequest request) {
        DeclarationEntity declaration = findDeclaration(declarationId);
        Long reviewerId = accessService.currentUserId();
        accessService.requireResourceAccess(
                "eval_application",
                declarationId,
                "declaration:application:audit",
                declaration.getApplicantId());
        assertReviewStatus(request.getReviewStatus());
        if (!"approve".equals(request.getReviewAction())
                && !"reject".equals(request.getReviewAction())) {
            throw new IllegalArgumentException("审核动作只能是 approve 或 reject");
        }
        if ("approve".equals(request.getReviewAction())
                && !"approved".equals(request.getReviewStatus())) {
            throw new IllegalArgumentException("approve 动作对应的审核状态必须是 approved");
        }
        if ("reject".equals(request.getReviewAction())
                && !"rejected".equals(request.getReviewStatus())) {
            throw new IllegalArgumentException("reject 动作对应的审核状态必须是 rejected");
        }
        if (!"submitted".equals(declaration.getReviewStatus())) {
            throw new IllegalArgumentException("只有已提交状态的申报可以审核");
        }
        String beforeStatus = declaration.getReviewStatus();
        declaration.setReviewStatus(request.getReviewStatus());
        declaration.setReviewerId(reviewerId);
        declaration.setReviewedAt(LocalDateTime.now());
        declaration.setReviewComment(request.getReviewComment());
        declaration.setUpdatedBy(reviewerId);
        declarationMapper.updateById(declaration);
        if ("approved".equals(request.getReviewStatus())) {
            archiveService.generateArchive(declarationId);
        }
        return saveReviewRecord(
                declarationId,
                reviewerId,
                request.getReviewAction(),
                beforeStatus,
                request.getReviewStatus(),
                request.getReviewComment(),
                request.getRemark());
    }

    @Override
    public DeclarationSummaryVO getDeclarationSummary(Long declarationId) {
        DeclarationEntity declaration = findDeclaration(declarationId);
        accessService.requireResourceAccess(
                "eval_application",
                declarationId,
                "declaration:application:view",
                declaration.getApplicantId());
        Long materialCount =
                materialMapper.selectCount(
                        new LambdaQueryWrapper<DeclarationMaterialEntity>()
                                .eq(DeclarationMaterialEntity::getApplicationId, declarationId));
        Long reviewRecordCount =
                reviewRecordMapper.selectCount(
                        new LambdaQueryWrapper<DeclarationReviewRecordEntity>()
                                .eq(
                                        DeclarationReviewRecordEntity::getApplicationId,
                                        declarationId));
        DeclarationArchiveEntity archive = findArchiveByDeclarationId(declarationId);
        Long archiveItemCount =
                archive == null
                        ? 0L
                        : archiveItemMapper.selectCount(
                                new LambdaQueryWrapper<DeclarationArchiveItemEntity>()
                                        .eq(
                                                DeclarationArchiveItemEntity::getArchiveId,
                                                archive.getId()));
        DeclarationSummaryVO summary = new DeclarationSummaryVO();
        summary.setDeclarationId(declarationId);
        summary.setApplicationTitle(declaration.getApplicationTitle());
        summary.setReviewStatus(declaration.getReviewStatus());
        summary.setMaterialCount(materialCount);
        summary.setReviewRecordCount(reviewRecordCount);
        summary.setArchiveId(archive == null ? null : archive.getId());
        summary.setArchiveNo(archive == null ? null : archive.getArchiveNo());
        summary.setArchiveItemCount(archiveItemCount);
        return summary;
    }

    private DeclarationReviewRecordEntity saveReviewRecord(
            Long declarationId,
            Long reviewerId,
            String reviewAction,
            String beforeStatus,
            String reviewStatus,
            String reviewComment,
            String remark) {
        DeclarationReviewRecordEntity record = new DeclarationReviewRecordEntity();
        record.setApplicationId(declarationId);
        record.setReviewerId(reviewerId);
        record.setReviewAction(reviewAction);
        record.setBeforeStatus(beforeStatus);
        record.setReviewStatus(reviewStatus);
        record.setReviewComment(reviewComment);
        record.setReviewedAt(LocalDateTime.now());
        record.setCreatedBy(reviewerId);
        record.setRemark(remark);
        reviewRecordMapper.insert(record);
        return reviewRecordMapper.selectById(record.getId());
    }

    private DeclarationEntity findDeclaration(Long declarationId) {
        DeclarationEntity declaration = declarationMapper.selectById(declarationId);
        if (declaration == null) {
            throw new IllegalArgumentException("申报档案不存在");
        }
        return declaration;
    }

    private DeclarationArchiveEntity findArchiveByDeclarationId(Long declarationId) {
        return archiveMapper.selectOne(
                new LambdaQueryWrapper<DeclarationArchiveEntity>()
                        .eq(DeclarationArchiveEntity::getApplicationId, declarationId));
    }

    private List<DeclarationMaterialEntity> listMaterials(Long declarationId) {
        return materialMapper.selectList(
                new LambdaQueryWrapper<DeclarationMaterialEntity>()
                        .eq(DeclarationMaterialEntity::getApplicationId, declarationId)
                        .orderByDesc(DeclarationMaterialEntity::getUploadedAt));
    }

    private List<DeclarationReviewRecordEntity> listReviewRecords(Long declarationId) {
        return reviewRecordMapper.selectList(
                new LambdaQueryWrapper<DeclarationReviewRecordEntity>()
                        .eq(DeclarationReviewRecordEntity::getApplicationId, declarationId)
                        .orderByDesc(DeclarationReviewRecordEntity::getReviewedAt));
    }

    private List<DeclarationArchiveItemEntity> listArchiveItems(Long archiveId) {
        return archiveItemMapper.selectList(
                new LambdaQueryWrapper<DeclarationArchiveItemEntity>()
                        .eq(DeclarationArchiveItemEntity::getArchiveId, archiveId)
                        .orderByAsc(DeclarationArchiveItemEntity::getSortOrder)
                        .orderByAsc(DeclarationArchiveItemEntity::getId));
    }

    private void assertReviewStatus(String reviewStatus) {
        if (!REVIEW_STATUSES.contains(reviewStatus)) {
            throw new IllegalArgumentException(
                    "审核状态只能是 draft、submitted、approved、rejected、archived");
        }
    }

    private Page<DeclarationEntity> page(Long pageNum, Long pageSize) {
        return new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateNo(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return prefix + "-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
