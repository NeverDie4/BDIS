package com.bdis.modules.experiment.service.impl;

import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.service.FileBusinessService;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.experiment.constant.ExperimentArchiveStatus;
import com.bdis.modules.experiment.constant.ExperimentRecordBusinessType;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.entity.ExperimentRecordVersionEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.experiment.mapper.ExperimentRecordVersionMapper;
import com.bdis.modules.experiment.request.ExperimentRecordVersionRequest;
import com.bdis.modules.experiment.service.ExperimentRecordVersionService;
import com.bdis.modules.research.constant.ResearchProjectStatus;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExperimentRecordVersionServiceImpl implements ExperimentRecordVersionService {
    private static final String BIZ_TYPE = ExperimentRecordBusinessType.EXPERIMENT_RECORD;
    private final ExperimentRecordMapper recordMapper;
    private final ExperimentRecordVersionMapper versionMapper;
    private final FileBusinessService fileBusinessService;

    public ExperimentRecordVersionServiceImpl(
            ExperimentRecordMapper recordMapper,
            ExperimentRecordVersionMapper versionMapper,
            FileBusinessService fileBusinessService) {
        this.recordMapper = recordMapper;
        this.versionMapper = versionMapper;
        this.fileBusinessService = fileBusinessService;
    }

    @Override
    @Transactional
    public ExperimentRecordVersionEntity create(Long id, ExperimentRecordVersionRequest request) {
        Long userId = requireUser();
        ExperimentRecordEntity record = requireRecord(id);
        if (!Objects.equals(userId, record.getRecorderId())) {
            throw new ForbiddenException("Only record owner can create a version");
        }
        if (!ExperimentArchiveStatus.RETURNED.equals(record.getArchiveStatus())
                && !ExperimentArchiveStatus.DRAFT.equals(record.getArchiveStatus())) {
            throw new BusinessException("Only draft or returned records can create a version");
        }
        validateSubmission(record, request);
        validateSource(record);
        validateReportFileBinding(id, request.getReportFileId());
        List<ExperimentRecordVersionEntity> all = versionMapper.selectByRecordId(id);
        LocalDateTime now = LocalDateTime.now();
        ExperimentRecordVersionEntity version = new ExperimentRecordVersionEntity();
        version.setRecordId(id);
        version.setVersionNo(all.size() + 1);
        version.setReportFileId(request.getReportFileId());
        version.setExperimentTitle(request.getExperimentTitle());
        version.setExperimentProcess(request.getExperimentProcess());
        version.setExperimentResult(request.getExperimentResult());
        version.setSubmittedBy(userId);
        version.setSubmittedAt(now);
        version.setStatus("submitted");
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        if (versionMapper.insert(version) == 0) {
            throw new BusinessException("Experiment report version creation failed");
        }
        record.setExperimentTitle(request.getExperimentTitle());
        record.setExperimentProcess(request.getExperimentProcess());
        record.setExperimentResult(request.getExperimentResult());
        record.setReportFileId(request.getReportFileId());
        record.setArchiveComment(null);
        if (recordMapper.resubmitVersionByIdAndVersion(
                        id,
                        record.getVersion(),
                        userId,
                        now,
                        request.getExperimentTitle(),
                        request.getExperimentProcess(),
                        request.getExperimentResult(),
                        request.getReportFileId())
                == 0) {
            throw new BusinessException("Experiment record resubmission conflict");
        }
        return version;
    }

    private void validateSubmission(
            ExperimentRecordEntity record, ExperimentRecordVersionRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getExperimentTitle())
                || !StringUtils.hasText(request.getExperimentProcess())
                || !StringUtils.hasText(request.getExperimentResult())
                || record.getRecordedAt() == null) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Experiment title, process, result and recorded time are required before submission");
        }
    }

    private void validateSource(ExperimentRecordEntity record) {
        Long courseId = record.getCourseId();
        Long projectId = record.getProjectId();
        if ((courseId == null) == (projectId == null)) {
            throw new BusinessException("Exactly one of courseId and projectId is required");
        }
        if (courseId != null) {
            validateCourseSource(courseId);
            return;
        }
        validateProjectSource(projectId);
    }

    private void validateCourseSource(Long courseId) {
        CourseEntity course = recordMapper.selectCourseByIdIncludingDeleted(courseId);
        if (course == null || Objects.equals(course.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Course not found");
        }
        if (!Objects.equals(course.getStatus(), 1)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Course is disabled");
        }
        Long userId = CurrentUserUtils.currentUserId();
        boolean courseOwner =
                Objects.equals(userId, course.getCreatedBy())
                        || Objects.equals(userId, course.getTeacherId());
        boolean student =
                CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(role -> "STUDENT".equalsIgnoreCase(role));
        if (hasScopedIdentity()
                && !isAdmin()
                && !courseOwner
                && (!student || !"published".equalsIgnoreCase(course.getPublishStatus()))) {
            throw new ForbiddenException("Course is outside the current user's scope");
        }
    }

    private void validateProjectSource(Long projectId) {
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
        Long userId = CurrentUserUtils.currentUserId();
        if (hasScopedIdentity()
                && !isAdmin()
                && !Objects.equals(userId, project.getLeaderId())
                && !recordMapper.existsActiveProjectMember(projectId, userId)) {
            throw new ForbiddenException("Research project is outside the current user's scope");
        }
    }

    private void validateReportFileBinding(Long recordId, Long reportFileId) {
        if (reportFileId == null) {
            return;
        }
        boolean boundAsReport =
                fileBusinessService.listBindingsByBusiness(BIZ_TYPE, recordId).stream()
                        .anyMatch(
                                binding ->
                                        Objects.equals(reportFileId, binding.getFileId())
                                                && "report".equals(binding.getFileUsage()));
        if (!boundAsReport) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Report file must be bound to the experiment record as report");
        }
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream()
                .anyMatch(SecurityConstants.ADMIN_ROLE_CODE::equalsIgnoreCase);
    }

    private boolean hasScopedIdentity() {
        return !CurrentUserUtils.currentRoleCodes().isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentRecordVersionEntity> list(Long id) {
        ExperimentRecordEntity record = requireRecord(id);
        requireReadAccess(record);
        return versionMapper.selectByRecordId(id);
    }

    private ExperimentRecordEntity requireRecord(Long id) {
        ExperimentRecordEntity record = recordMapper.selectById(id);
        if (record == null || Objects.equals(record.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Experiment record not found");
        }
        return record;
    }

    private Long requireUser() {
        Long userId = CurrentUserUtils.currentUserId();
        if (userId == null) {
            throw new ForbiddenException("Authentication is required");
        }
        return userId;
    }

    private void requireReadAccess(ExperimentRecordEntity record) {
        Long userId = requireUser();
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase)
                || Objects.equals(userId, record.getRecorderId())) {
            return;
        }
        if (record.getCourseId() != null) {
            CourseEntity course =
                    recordMapper.selectCourseByIdIncludingDeleted(record.getCourseId());
            if (course != null
                    && !Objects.equals(course.getIsDeleted(), 1)
                    && Objects.equals(userId, course.getTeacherId())) {
                return;
            }
        }
        if (record.getProjectId() != null) {
            ResearchProjectEntity project =
                    recordMapper.selectProjectByIdIncludingDeleted(record.getProjectId());
            if (project != null
                    && !Objects.equals(project.getIsDeleted(), 1)
                    && (Objects.equals(userId, project.getLeaderId())
                            || recordMapper.existsActiveProjectMember(
                                    record.getProjectId(), userId))) {
                return;
            }
        }
        throw new ForbiddenException("Experiment record is outside the current user's scope");
    }
}
