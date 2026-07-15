package com.bdis.modules.experiment.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.experiment.constant.ExperimentArchiveStatus;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.entity.ExperimentRecordVersionEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.experiment.mapper.ExperimentRecordVersionMapper;
import com.bdis.modules.experiment.request.ExperimentRecordVersionRequest;
import com.bdis.modules.experiment.service.ExperimentRecordVersionService;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperimentRecordVersionServiceImpl implements ExperimentRecordVersionService {
    private final ExperimentRecordMapper recordMapper;
    private final ExperimentRecordVersionMapper versionMapper;

    public ExperimentRecordVersionServiceImpl(
            ExperimentRecordMapper recordMapper, ExperimentRecordVersionMapper versionMapper) {
        this.recordMapper = recordMapper;
        this.versionMapper = versionMapper;
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
        if (request == null
                || request.getExperimentTitle() == null
                || request.getExperimentTitle().isBlank()) {
            throw new BusinessException("Experiment title is required");
        }
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
        if (recordMapper.submitByIdAndVersion(id, record.getVersion(), userId, now) == 0) {
            throw new BusinessException("Experiment record resubmission conflict");
        }
        return version;
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
