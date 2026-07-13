package com.bdis.modules.research.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.research.constant.ResearchAchievementStage;
import com.bdis.modules.research.constant.ResearchAchievementStatus;
import com.bdis.modules.research.constant.ResearchAchievementType;
import com.bdis.modules.research.constant.ResearchProjectStatus;
import com.bdis.modules.research.entity.ResearchAchievementEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ResearchAchievementMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.query.ResearchAchievementQuery;
import com.bdis.modules.research.request.ResearchAchievementCreateRequest;
import com.bdis.modules.research.request.ResearchAchievementUpdateRequest;
import com.bdis.modules.research.service.ResearchAchievementService;
import com.bdis.modules.research.vo.ResearchAchievementDetailVO;
import com.bdis.modules.research.vo.ResearchAchievementListVO;
import com.bdis.modules.research.vo.ResearchAchievementSummaryVO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ResearchAchievementServiceImpl implements ResearchAchievementService {
    private static final String BIZ_TYPE = "research_achievement";
    private static final String AUDIT_MODULE = "M13_RESEARCH_ACHIEVEMENT";

    private final ResearchAchievementMapper achievementMapper;
    private final ResearchProjectMapper projectMapper;
    private final FileResourceMapper fileResourceMapper;
    private final AuditLogService auditLogService;

    public ResearchAchievementServiceImpl(ResearchAchievementMapper achievementMapper,
            ResearchProjectMapper projectMapper, FileResourceMapper fileResourceMapper,
            AuditLogService auditLogService) {
        this.achievementMapper = achievementMapper;
        this.projectMapper = projectMapper;
        this.fileResourceMapper = fileResourceMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public PageResult<ResearchAchievementListVO> page(ResearchAchievementQuery query) {
        ResearchAchievementQuery safe = query == null ? new ResearchAchievementQuery() : query;
        if (safe.getPageNo() == null || safe.getPageNo() < 1) safe.setPageNo(1);
        if (safe.getPageSize() == null || safe.getPageSize() < 1) safe.setPageSize(10);
        Page<ResearchAchievementEntity> page = achievementMapper.selectPage(
                Page.of(safe.getPageNo(), safe.getPageSize()), buildWrapper(safe));
        return PageResult.of(toList(page.getRecords()), page);
    }

    @Override
    public ResearchAchievementDetailVO getDetail(Long id) {
        ResearchAchievementEntity entity = requireActive(id);
        return toDetail(entity);
    }

    @Override
    @Transactional
    public Long create(ResearchAchievementCreateRequest request) {
        validateCreate(request);
        String no = request.getAchievementNo().trim();
        if (achievementMapper.selectByAchievementNoIncludingDeleted(no) != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Achievement number already exists");
        }
        ResearchProjectEntity project = requireWritableProject(request.getProjectId());
        validateTypeAndStage(request.getAchievementType(), request.getAchievementStage());
        validateFile(request.getFileId());

        LocalDateTime now = LocalDateTime.now();
        ResearchAchievementEntity entity = new ResearchAchievementEntity();
        entity.setAchievementNo(no);
        entity.setProjectId(project.getId());
        entity.setOwnerId(CurrentUserUtils.currentUserId());
        entity.setAchievementName(request.getAchievementName().trim());
        entity.setAchievementType(request.getAchievementType().trim());
        entity.setAchievementStage(trimToNull(request.getAchievementStage()));
        entity.setAchievementStatus(ResearchAchievementStatus.DRAFT);
        entity.setDescription(request.getDescription());
        entity.setFileId(request.getFileId());
        entity.setPublishedAt(request.getPublishedAt());
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        entity.setRemark(request.getRemark());
        entity.setVersion(0);
        if (achievementMapper.insert(entity) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Achievement create failed");
        }
        recordAudit("CREATE", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ResearchAchievementUpdateRequest request) {
        ResearchAchievementEntity entity = requireActive(id);
        if (ResearchAchievementStatus.isConfirmed(entity.getAchievementStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "confirmed achievement cannot be modified");
        }
        if (request == null || !StringUtils.hasText(request.getAchievementName())
                || !StringUtils.hasText(request.getAchievementType())
                || !StringUtils.hasText(request.getAchievementStatus()) || request.getVersion() == null) {
            throw new BusinessException("Achievement name, type, status and version are required");
        }
        if (!Objects.equals(request.getVersion(), entity.getVersion())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Achievement version conflict");
        }
        ResearchProjectEntity project = requireWritableProject(entity.getProjectId());
        ResearchAchievementStatus.validateTransition(entity.getAchievementStatus(), request.getAchievementStatus().trim());
        validateTypeAndStage(request.getAchievementType(), request.getAchievementStage());
        validateFile(request.getFileId());
        entity.setAchievementName(request.getAchievementName().trim());
        entity.setAchievementType(request.getAchievementType().trim());
        entity.setAchievementStage(trimToNull(request.getAchievementStage()));
        entity.setAchievementStatus(request.getAchievementStatus().trim());
        entity.setDescription(request.getDescription());
        entity.setFileId(request.getFileId());
        entity.setPublishedAt(request.getPublishedAt());
        entity.setRemark(request.getRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (achievementMapper.updateById(entity) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Achievement update failed");
        }
        recordAudit("UPDATE", id);
    }

    @Override
    public List<ResearchAchievementListVO> listByProjectId(Long projectId) {
        if (projectId == null) return List.of();
        return toList(achievementMapper.selectList(new LambdaQueryWrapper<ResearchAchievementEntity>()
                .eq(ResearchAchievementEntity::getProjectId, projectId)
                .orderByDesc(ResearchAchievementEntity::getCreatedAt)
                .orderByDesc(ResearchAchievementEntity::getId)));
    }

    @Override
    public ResearchAchievementSummaryVO summarizeByProjectId(Long projectId) {
        ResearchAchievementSummaryVO summary = new ResearchAchievementSummaryVO();
        List<ResearchAchievementEntity> records = projectId == null ? List.of()
                : achievementMapper.selectList(new LambdaQueryWrapper<ResearchAchievementEntity>()
                        .eq(ResearchAchievementEntity::getProjectId, projectId));
        summary.setTotal(records.size());
        records.forEach(entity -> {
            if (ResearchAchievementStatus.DRAFT.equals(entity.getAchievementStatus())) summary.setDraftCount(summary.getDraftCount() + 1);
            if (ResearchAchievementStatus.SUBMITTED.equals(entity.getAchievementStatus())) summary.setSubmittedCount(summary.getSubmittedCount() + 1);
            if (ResearchAchievementStatus.CONFIRMED.equals(entity.getAchievementStatus())) summary.setConfirmedCount(summary.getConfirmedCount() + 1);
            if (ResearchAchievementStage.INITIAL.equals(entity.getAchievementStage())) summary.setInitialCount(summary.getInitialCount() + 1);
            if (ResearchAchievementStage.MIDDLE.equals(entity.getAchievementStage())) summary.setMiddleCount(summary.getMiddleCount() + 1);
            if (ResearchAchievementStage.FINAL.equals(entity.getAchievementStage())) summary.setFinalCount(summary.getFinalCount() + 1);
        });
        return summary;
    }

    private ResearchAchievementEntity requireActive(Long id) {
        ResearchAchievementEntity entity = achievementMapper.selectById(id);
        if (entity == null) throw new ResourceNotFoundException("Research achievement not found");
        return entity;
    }

    private ResearchProjectEntity requireWritableProject(Long projectId) {
        ResearchProjectEntity project = projectMapper.selectById(projectId);
        if (project == null || !Objects.equals(project.getStatus(), 1) || !Objects.equals(project.getIsDeleted(), 0)) {
            throw new ResourceNotFoundException("Research project not found");
        }
        if (ResearchProjectStatus.COMPLETED.equals(project.getProjectStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "completed project cannot be modified");
        }
        return project;
    }

    private void validateCreate(ResearchAchievementCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAchievementNo())
                || request.getProjectId() == null || !StringUtils.hasText(request.getAchievementName())
                || !StringUtils.hasText(request.getAchievementType())) {
            throw new BusinessException("Achievement number, project, name and type are required");
        }
    }

    private void validateTypeAndStage(String type, String stage) {
        if (!ResearchAchievementType.ALL.contains(type.trim())) throw new BusinessException("Invalid achievement type");
        if (StringUtils.hasText(stage) && !ResearchAchievementStage.ALL.contains(stage.trim())) {
            throw new BusinessException("Invalid achievement stage");
        }
    }

    private void validateFile(Long fileId) {
        if (fileId == null) return;
        FileResourceEntity file = fileResourceMapper.selectById(fileId);
        if (file == null || !Objects.equals(file.getStatus(), 1) || !Objects.equals(file.getIsDeleted(), 0)) {
            throw new ResourceNotFoundException("File not found or inactive");
        }
    }

    private LambdaQueryWrapper<ResearchAchievementEntity> buildWrapper(ResearchAchievementQuery query) {
        LambdaQueryWrapper<ResearchAchievementEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(condition -> condition.like(ResearchAchievementEntity::getAchievementNo, query.getKeyword())
                    .or().like(ResearchAchievementEntity::getAchievementName, query.getKeyword()));
        }
        wrapper.eq(query.getProjectId() != null, ResearchAchievementEntity::getProjectId, query.getProjectId());
        wrapper.eq(StringUtils.hasText(query.getAchievementType()), ResearchAchievementEntity::getAchievementType, query.getAchievementType());
        wrapper.eq(StringUtils.hasText(query.getAchievementStage()), ResearchAchievementEntity::getAchievementStage, query.getAchievementStage());
        wrapper.eq(StringUtils.hasText(query.getAchievementStatus()), ResearchAchievementEntity::getAchievementStatus, query.getAchievementStatus());
        wrapper.ge(query.getPublishedFrom() != null, ResearchAchievementEntity::getPublishedAt, query.getPublishedFrom());
        wrapper.le(query.getPublishedTo() != null, ResearchAchievementEntity::getPublishedAt, query.getPublishedTo());
        return wrapper.orderByDesc(ResearchAchievementEntity::getCreatedAt).orderByDesc(ResearchAchievementEntity::getId);
    }

    private List<ResearchAchievementListVO> toList(List<ResearchAchievementEntity> records) {
        if (records == null || records.isEmpty()) return List.of();
        Map<Long, ResearchProjectEntity> projects = new HashMap<>();
        List<Long> projectIds = records.stream().map(ResearchAchievementEntity::getProjectId).filter(Objects::nonNull).distinct().toList();
        if (!projectIds.isEmpty()) {
            List<ResearchProjectEntity> rows = projectMapper.selectBatchIds(projectIds);
            if (rows != null) rows.forEach(row -> projects.put(row.getId(), row));
        }
        Map<Long, FileResourceEntity> files = files(records);
        return records.stream().map(entity -> toListVO(entity, projects.get(entity.getProjectId()), files.get(entity.getFileId()))).toList();
    }

    private Map<Long, FileResourceEntity> files(List<ResearchAchievementEntity> records) {
        Map<Long, FileResourceEntity> files = new HashMap<>();
        List<Long> ids = records.stream().map(ResearchAchievementEntity::getFileId).filter(Objects::nonNull).distinct().toList();
        if (!ids.isEmpty()) {
            List<FileResourceEntity> rows = fileResourceMapper.selectBatchIds(ids);
            if (rows != null) rows.stream().filter(this::isActiveFile).forEach(row -> files.put(row.getId(), row));
        }
        return files;
    }

    private ResearchAchievementListVO toListVO(ResearchAchievementEntity entity, ResearchProjectEntity project, FileResourceEntity file) {
        ResearchAchievementListVO vo = new ResearchAchievementListVO();
        vo.setId(entity.getId()); vo.setAchievementNo(entity.getAchievementNo()); vo.setProjectId(entity.getProjectId());
        if (project != null) { vo.setProjectNo(project.getProjectNo()); vo.setProjectName(project.getProjectName()); }
        vo.setAchievementName(entity.getAchievementName()); vo.setAchievementType(entity.getAchievementType());
        vo.setAchievementStage(entity.getAchievementStage()); vo.setAchievementStatus(entity.getAchievementStatus());
        vo.setFileId(entity.getFileId()); vo.setFileName(file == null ? null : file.getFileName());
        vo.setPublishedAt(entity.getPublishedAt()); vo.setStatus(entity.getStatus()); vo.setCreatedAt(entity.getCreatedAt()); vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private ResearchAchievementDetailVO toDetail(ResearchAchievementEntity entity) {
        ResearchProjectEntity project = entity.getProjectId() == null ? null : projectMapper.selectById(entity.getProjectId());
        FileResourceEntity file = entity.getFileId() == null ? null : fileResourceMapper.selectById(entity.getFileId());
        ResearchAchievementDetailVO vo = new ResearchAchievementDetailVO();
        ResearchAchievementListVO base = toListVO(entity, project, isActiveFile(file) ? file : null);
        vo.setId(base.getId()); vo.setAchievementNo(base.getAchievementNo()); vo.setProjectId(base.getProjectId());
        vo.setProjectNo(base.getProjectNo()); vo.setProjectName(base.getProjectName()); vo.setAchievementName(base.getAchievementName());
        vo.setAchievementType(base.getAchievementType()); vo.setAchievementStage(base.getAchievementStage()); vo.setAchievementStatus(base.getAchievementStatus());
        vo.setFileId(base.getFileId()); vo.setFileName(base.getFileName()); vo.setPublishedAt(base.getPublishedAt()); vo.setStatus(base.getStatus());
        vo.setCreatedAt(base.getCreatedAt()); vo.setUpdatedAt(base.getUpdatedAt()); vo.setDescription(entity.getDescription()); vo.setRemark(entity.getRemark());
        vo.setCreatedBy(entity.getCreatedBy()); vo.setUpdatedBy(entity.getUpdatedBy()); vo.setVersion(entity.getVersion());
        if (isActiveFile(file)) { vo.setFileNo(file.getFileNo()); vo.setOriginalFilename(file.getOriginalFilename()); vo.setFileType(file.getFileType()); vo.setFileFormat(file.getFileFormat()); vo.setFileSize(file.getFileSize()); vo.setFileUrl(file.getFileUrl()); vo.setThumbnailUrl(file.getThumbnailUrl()); }
        return vo;
    }

    private boolean isActiveFile(FileResourceEntity file) { return file != null && Objects.equals(file.getStatus(), 1) && Objects.equals(file.getIsDeleted(), 0); }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }

    private void recordAudit(String operation, Long id) {
        AuditRecordDTO audit = new AuditRecordDTO(); audit.setOperationModule(AUDIT_MODULE); audit.setOperationType(operation); audit.setBizType(BIZ_TYPE); audit.setBizId(id); auditLogService.record(audit);
    }
}
