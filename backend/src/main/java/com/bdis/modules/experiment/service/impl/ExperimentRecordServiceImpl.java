package com.bdis.modules.experiment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.experiment.constant.ExperimentArchiveStatus;
import com.bdis.modules.experiment.constant.ExperimentRecordBusinessType;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.experiment.query.ExperimentRecordQuery;
import com.bdis.modules.experiment.request.ExperimentRecordArchiveRequest;
import com.bdis.modules.experiment.request.ExperimentRecordAttachmentBindRequest;
import com.bdis.modules.experiment.request.ExperimentRecordCreateRequest;
import com.bdis.modules.experiment.request.ExperimentRecordSubmitRequest;
import com.bdis.modules.experiment.request.ExperimentRecordUpdateRequest;
import com.bdis.modules.experiment.service.ExperimentRecordService;
import com.bdis.modules.experiment.vo.ExperimentRecordDetailVO;
import com.bdis.modules.experiment.vo.ExperimentRecordListVO;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.research.constant.ResearchProjectStatus;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExperimentRecordServiceImpl implements ExperimentRecordService {

    private static final String BIZ_TYPE = ExperimentRecordBusinessType.EXPERIMENT_RECORD;
    private static final String AUDIT_MODULE = "M14_EXPERIMENT_RECORD";
    private static final Set<String> ARCHIVE_STATUSES =
            Set.of(
                    ExperimentArchiveStatus.DRAFT,
                    ExperimentArchiveStatus.SUBMITTED,
                    ExperimentArchiveStatus.ARCHIVED);
    private static final Set<String> ATTACHMENT_USAGES = Set.of("attachment", "image");

    private final ExperimentRecordMapper recordMapper;
    private final UserMapper userMapper;
    private final FileBusinessService fileBusinessService;
    private final AuditLogService auditLogService;

    public ExperimentRecordServiceImpl(
            ExperimentRecordMapper recordMapper,
            UserMapper userMapper,
            FileBusinessService fileBusinessService,
            AuditLogService auditLogService) {
        this.recordMapper = recordMapper;
        this.userMapper = userMapper;
        this.fileBusinessService = fileBusinessService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExperimentRecordListVO> page(ExperimentRecordQuery query) {
        ExperimentRecordQuery safeQuery = query == null ? new ExperimentRecordQuery() : query;
        normalizePage(safeQuery);
        validateRecordedRange(safeQuery.getRecordedFrom(), safeQuery.getRecordedTo());
        validateArchiveStatus(safeQuery.getArchiveStatus());
        applyScope(safeQuery);
        Page<ExperimentRecordListVO> page =
                recordMapper.selectPageVO(
                        Page.of(safeQuery.getPageNo(), safeQuery.getPageSize()), safeQuery);
        return PageResult.of(page.getRecords(), page);
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentRecordDetailVO getDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Experiment record id must be positive");
        }
        ExperimentRecordDetailVO detail = recordMapper.selectDetailById(id);
        if (detail == null) {
            throw new ResourceNotFoundException("Experiment record not found");
        }
        requireRecordAccess(detail.getRecorderId(), detail.getCourseId(), detail.getProjectId(), false);
        return detail;
    }

    @Override
    @Transactional
    public Long create(ExperimentRecordCreateRequest request) {
        validateCreate(request);
        String recordNo = request.getRecordNo().trim();
        if (recordMapper.selectByRecordNoIncludingDeleted(recordNo) != null) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Experiment record number already exists");
        }
        validateSource(request.getCourseId(), request.getProjectId());
        UserEntity recorder = requireCurrentOperator();

        LocalDateTime now = LocalDateTime.now();
        ExperimentRecordEntity entity = new ExperimentRecordEntity();
        entity.setRecordNo(recordNo);
        entity.setCourseId(request.getCourseId());
        entity.setProjectId(request.getProjectId());
        entity.setExperimentTitle(request.getExperimentTitle().trim());
        entity.setExperimentProcess(request.getExperimentProcess());
        entity.setExperimentResult(request.getExperimentResult());
        entity.setRecorderId(recorder.getId());
        entity.setRecordedAt(request.getRecordedAt() == null ? now : request.getRecordedAt());
        entity.setArchiveStatus(ExperimentArchiveStatus.DRAFT);
        entity.setSubmittedAt(null);
        entity.setSubmittedBy(null);
        entity.setArchivedAt(null);
        entity.setArchivedBy(null);
        entity.setArchiveComment(null);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(recorder.getId());
        entity.setUpdatedBy(recorder.getId());
        entity.setRemark(request.getRemark());
        entity.setVersion(0);
        try {
            if (recordMapper.insert(entity) == 0) {
                throw new BusinessException(
                        ResultCodeEnum.CONFLICT, "Experiment record create failed");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Experiment record number already exists");
        }
        recordAudit("CREATE", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ExperimentRecordUpdateRequest request) {
        ExperimentRecordEntity entity = requireActive(id);
        requireRecordOwner(entity);
        ExperimentArchiveStatus.assertMutable(entity.getArchiveStatus());
        validateUpdate(request, entity);
        Long operatorId = CurrentUserUtils.currentUserId();

        entity.setExperimentTitle(request.getExperimentTitle().trim());
        entity.setExperimentProcess(request.getExperimentProcess());
        entity.setExperimentResult(request.getExperimentResult());
        if (request.getRecordedAt() != null) {
            entity.setRecordedAt(request.getRecordedAt());
        }
        entity.setRemark(request.getRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(operatorId);
        if (recordMapper.updateById(entity) == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Experiment record version conflict");
        }
        recordAudit("UPDATE", id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExperimentRecordEntity entity = requireActive(id);
        requireRecordOwner(entity);
        ExperimentArchiveStatus.assertDeletable(entity.getArchiveStatus());
        if (fileBusinessService.existsByBusiness(BIZ_TYPE, id)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Experiment record has attachments; unbind attachments before deletion");
        }
        Long operatorId = CurrentUserUtils.currentUserId();
        if (recordMapper.logicalDeleteByIdAndVersion(
                        id, entity.getVersion(), operatorId, LocalDateTime.now())
                == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Experiment record delete conflict");
        }
        recordAudit("DELETE", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id, ExperimentRecordSubmitRequest request) {
        ExperimentRecordEntity entity = requireActive(id);
        requireRecordOwner(entity);
        ExperimentArchiveStatus.assertSubmittable(entity.getArchiveStatus());
        validateWorkflowVersion(request == null ? null : request.getVersion(), entity.getVersion());
        validateSubmissionCompleteness(entity);
        validateSubmissionSource(entity.getCourseId(), entity.getProjectId());
        UserEntity operator = requireCurrentOperator();

        LocalDateTime submittedAt = LocalDateTime.now();
        if (recordMapper.submitByIdAndVersion(
                        id, request.getVersion(), operator.getId(), submittedAt)
                == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Experiment record submit state or version conflict");
        }
        recordAudit("SUBMIT", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id, ExperimentRecordArchiveRequest request) {
        ExperimentRecordEntity entity = requireActive(id);
        requireRecordAccess(entity.getRecorderId(), entity.getCourseId(), entity.getProjectId(), true);
        ExperimentArchiveStatus.assertArchivable(entity.getArchiveStatus());
        validateWorkflowVersion(request == null ? null : request.getVersion(), entity.getVersion());
        if (entity.getSubmittedAt() == null || entity.getSubmittedBy() == null) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Submitted experiment record metadata is incomplete");
        }
        UserEntity operator = requireCurrentOperator();

        LocalDateTime archivedAt = LocalDateTime.now();
        if (recordMapper.archiveByIdAndVersion(
                        id,
                        request.getVersion(),
                        operator.getId(),
                        archivedAt,
                        request.getArchiveComment())
                == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Experiment record archive state or version conflict");
        }
        recordAudit("ARCHIVE", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResourceVO> listAttachments(Long id, String fileUsage) {
        ExperimentRecordEntity entity = requireActive(id);
        requireRecordAccess(entity.getRecorderId(), entity.getCourseId(), entity.getProjectId(), false);
        validateAttachmentUsage(fileUsage, false);
        String normalizedUsage = StringUtils.hasText(fileUsage) ? fileUsage.trim() : null;
        return fileBusinessService.listByBusiness(BIZ_TYPE, id, normalizedUsage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileBusinessVO bindAttachment(
            Long id, ExperimentRecordAttachmentBindRequest request) {
        ExperimentRecordEntity entity = requireActive(id);
        requireRecordAccess(entity.getRecorderId(), entity.getCourseId(), entity.getProjectId(), true);
        ExperimentArchiveStatus.assertMutable(entity.getArchiveStatus());
        validateAttachmentRequest(request);
        requireCurrentOperator();

        FileBusinessBindDTO bindDTO = new FileBusinessBindDTO();
        bindDTO.setFileId(request.getFileId());
        bindDTO.setBizType(BIZ_TYPE);
        bindDTO.setBizId(id);
        bindDTO.setFileUsage(request.getFileUsage().trim());
        bindDTO.setSortOrder(request.getSortOrder());
        bindDTO.setRemark(request.getRemark());
        FileBusinessVO relation = fileBusinessService.bind(bindDTO);
        if (relation == null || relation.getId() == null) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Experiment attachment bind failed");
        }
        return relation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindAttachment(Long id, Long fileId) {
        ExperimentRecordEntity entity = requireActive(id);
        requireRecordAccess(entity.getRecorderId(), entity.getCourseId(), entity.getProjectId(), true);
        ExperimentArchiveStatus.assertMutable(entity.getArchiveStatus());
        if (fileId == null || fileId <= 0) {
            throw new BusinessException("File id must be positive");
        }
        requireCurrentOperator();
        fileBusinessService.unbind(BIZ_TYPE, id, fileId);
    }

    private ExperimentRecordEntity requireActive(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Experiment record id must be positive");
        }
        ExperimentRecordEntity entity = recordMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Experiment record not found");
        }
        return entity;
    }

    private void validateCreate(ExperimentRecordCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getRecordNo())
                || !StringUtils.hasText(request.getExperimentTitle())) {
            throw new BusinessException("Experiment record number and title are required");
        }
        validateSourceSelection(request.getCourseId(), request.getProjectId());
    }

    private void validateUpdate(
            ExperimentRecordUpdateRequest request, ExperimentRecordEntity entity) {
        if (request == null || !StringUtils.hasText(request.getExperimentTitle())) {
            throw new BusinessException("Experiment title is required");
        }
        if (request.getVersion() == null) {
            throw new BusinessException("Experiment record version is required");
        }
        if (!Objects.equals(request.getVersion(), entity.getVersion())) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Experiment record version conflict");
        }
        if (request.getRecordNo() != null
                || request.getCourseId() != null
                || request.getProjectId() != null
                || request.getRecorderId() != null
                || request.getArchiveStatus() != null
                || request.getSubmittedAt() != null
                || request.getSubmittedBy() != null
                || request.getArchivedAt() != null
                || request.getArchivedBy() != null
                || request.getArchiveComment() != null) {
            throw new BusinessException("Immutable or workflow fields cannot be changed");
        }
    }

    private void validateSource(Long courseId, Long projectId) {
        validateSourceSelection(courseId, projectId);
        if (courseId != null) {
            CourseEntity course = recordMapper.selectCourseByIdIncludingDeleted(courseId);
            if (course == null || Objects.equals(course.getIsDeleted(), 1)) {
                throw new ResourceNotFoundException("Course not found");
            }
            if (!Objects.equals(course.getStatus(), 1)) {
                throw new BusinessException(ResultCodeEnum.CONFLICT, "Course is disabled");
            }
            return;
        }
        ResearchProjectEntity project =
                recordMapper.selectProjectByIdIncludingDeleted(projectId);
        if (project == null || Objects.equals(project.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Research project not found");
        }
        if (!Objects.equals(project.getStatus(), 1)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Research project is disabled");
        }
        if (ResearchProjectStatus.COMPLETED.equals(project.getProjectStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Completed research projects cannot receive new experiment records");
        }
    }

    private void validateSourceSelection(Long courseId, Long projectId) {
        if ((courseId == null) == (projectId == null)) {
            throw new BusinessException("Exactly one of courseId and projectId is required");
        }
    }

    private void validateSubmissionCompleteness(ExperimentRecordEntity entity) {
        if (!StringUtils.hasText(entity.getExperimentTitle())
                || !StringUtils.hasText(entity.getExperimentProcess())
                || !StringUtils.hasText(entity.getExperimentResult())
                || entity.getRecordedAt() == null) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Experiment title, process, result and recorded time are required before submission");
        }
    }

    private void validateAttachmentRequest(ExperimentRecordAttachmentBindRequest request) {
        if (request == null || request.getFileId() == null || request.getFileId() <= 0) {
            throw new BusinessException("File id must be positive");
        }
        validateAttachmentUsage(request.getFileUsage(), true);
        if (request.getSortOrder() != null && request.getSortOrder() < 0) {
            throw new BusinessException("Attachment sort order must not be negative");
        }
    }

    private void validateAttachmentUsage(String fileUsage, boolean required) {
        if (!StringUtils.hasText(fileUsage)) {
            if (required) {
                throw new BusinessException("Attachment file usage is required");
            }
            return;
        }
        if (!ATTACHMENT_USAGES.contains(fileUsage.trim())) {
            throw new BusinessException("Invalid experiment attachment file usage");
        }
    }

    private void validateSubmissionSource(Long courseId, Long projectId) {
        validateSourceSelection(courseId, projectId);
        if (courseId != null) {
            CourseEntity course = recordMapper.selectCourseByIdIncludingDeleted(courseId);
            if (course == null || Objects.equals(course.getIsDeleted(), 1)) {
                throw new ResourceNotFoundException("Course not found");
            }
            if (!Objects.equals(course.getStatus(), 1)) {
                throw new BusinessException(ResultCodeEnum.CONFLICT, "Course is disabled");
            }
            return;
        }
        ResearchProjectEntity project =
                recordMapper.selectProjectByIdIncludingDeleted(projectId);
        if (project == null || Objects.equals(project.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Research project not found");
        }
        if (!Objects.equals(project.getStatus(), 1)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Research project is disabled");
        }
    }

    private void validateWorkflowVersion(Integer requestVersion, Integer entityVersion) {
        if (requestVersion == null || requestVersion < 0) {
            throw new BusinessException("Experiment record version must be zero or greater");
        }
        if (!Objects.equals(requestVersion, entityVersion)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Experiment record version conflict");
        }
    }

    private UserEntity requireCurrentOperator() {
        Long userId = CurrentUserUtils.currentUserId();
        if (userId == null || userId <= 0) {
            throw new ResourceNotFoundException("Current operator not found");
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Current operator not found");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Current operator is disabled");
        }
        return user;
    }

    private void validateRecordedRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Recorded time range is invalid");
        }
    }

    private void validateArchiveStatus(String status) {
        if (StringUtils.hasText(status) && !ARCHIVE_STATUSES.contains(status.trim())) {
            throw new BusinessException("Invalid experiment archive status");
        }
    }

    private void normalizePage(ExperimentRecordQuery query) {
        if (query.getPageNo() == null || query.getPageNo() < 1) {
            query.setPageNo(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        } else if (query.getPageSize() > 100) {
            query.setPageSize(100);
        }
    }

    private void applyScope(ExperimentRecordQuery query) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(role -> "ADMIN".equalsIgnoreCase(role))) {
            query.setScopeAll(true);
        } else {
            query.setScopeAll(false);
            query.setScopeUserId(CurrentUserUtils.currentUserId());
        }
    }

    private void requireRecordOwner(ExperimentRecordEntity entity) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(role -> "ADMIN".equalsIgnoreCase(role))) {
            return;
        }
        if (!Objects.equals(entity.getRecorderId(), CurrentUserUtils.currentUserId())) {
            throw new ForbiddenException("Only the record creator can submit this record");
        }
    }

    private void requireRecordAccess(Long recorderId, Long courseId, Long projectId, boolean manage) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(role -> "ADMIN".equalsIgnoreCase(role))) {
            return;
        }
        Long userId = CurrentUserUtils.currentUserId();
        if (Objects.equals(recorderId, userId)) {
            return;
        }
        if (courseId != null) {
            CourseEntity course = recordMapper.selectCourseByIdIncludingDeleted(courseId);
            if (course != null && Objects.equals(course.getTeacherId(), userId)) {
                return;
            }
        }
        if (projectId != null) {
            ResearchProjectEntity project = recordMapper.selectProjectByIdIncludingDeleted(projectId);
            if (project != null && Objects.equals(project.getLeaderId(), userId)) {
                return;
            }
        }
        throw new ForbiddenException(
                manage ? "Experiment record is outside the current user's scope"
                        : "Experiment record is not accessible to the current user");
    }

    private void recordAudit(String operationType, Long recordId) {
        AuditRecordDTO audit = new AuditRecordDTO();
        audit.setOperationModule(AUDIT_MODULE);
        audit.setOperationType(operationType);
        audit.setBizType(BIZ_TYPE);
        audit.setBizId(recordId);
        auditLogService.record(audit);
    }
}
