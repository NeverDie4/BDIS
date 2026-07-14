package com.bdis.modules.experiment.file;

import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ExperimentRecordFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final ExperimentRecordMapper recordMapper;
    private final ResearchProjectMapper projectMapper;
    private final AuthorizationService authorizationService;

    public ExperimentRecordFileBusinessAccessPolicy(
            ExperimentRecordMapper recordMapper,
            ResearchProjectMapper projectMapper,
            AuthorizationService authorizationService) {
        this.recordMapper = recordMapper;
        this.projectMapper = projectMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "edu_experiment_record";
    }

    @Override
    public boolean exists(Long bizId) {
        return recordMapper.existsActiveReferenceById(bizId);
    }

    @Override
    public boolean canView(Long bizId) {
        return isAllowed(bizId, "edu:experiment-record:detail", false);
    }

    @Override
    public boolean canAttach(Long bizId) {
        return isAllowed(bizId, "edu:experiment-record:update", true);
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return isAllowed(bizId, "edu:experiment-record:archive", true);
    }

    private boolean isAllowed(Long bizId, String permission, boolean manage) {
        ExperimentRecordEntity record = recordMapper.selectById(bizId);
        if (record == null || !authorizationService.hasPermission(permission)) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        Long userId = CurrentUserUtils.currentUserId();
        if (Objects.equals(userId, record.getRecorderId())) {
            return true;
        }
        if (record.getCourseId() != null) {
            var course = recordMapper.selectCourseByIdIncludingDeleted(record.getCourseId());
            if (course != null && Objects.equals(userId, course.getTeacherId())) {
                return !manage;
            }
        }
        if (record.getProjectId() != null) {
            var project = recordMapper.selectProjectByIdIncludingDeleted(record.getProjectId());
            if (project != null && Objects.equals(userId, project.getLeaderId())) {
                return true;
            }
            return !manage && projectMapper.existsActiveMember(record.getProjectId(), userId);
        }
        return false;
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream()
                .anyMatch(SecurityConstants.ADMIN_ROLE_CODE::equalsIgnoreCase);
    }
}
