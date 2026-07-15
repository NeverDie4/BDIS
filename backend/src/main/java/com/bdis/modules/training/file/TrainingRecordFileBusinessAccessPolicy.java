package com.bdis.modules.training.file;

import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TrainingRecordFileBusinessAccessPolicy implements FileBusinessAccessPolicy {
    private final TrainingRecordMapper recordMapper;
    private final TrainingPlanMapper planMapper;
    private final AuthorizationService authorizationService;

    public TrainingRecordFileBusinessAccessPolicy(
            TrainingRecordMapper recordMapper,
            TrainingPlanMapper planMapper,
            AuthorizationService authorizationService) {
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "edu_training_record";
    }

    @Override
    public boolean exists(Long bizId) {
        return recordMapper.selectActiveById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return authorizationService.hasPermission("edu:training-record:detail")
                && canRead(requireRecord(bizId));
    }

    @Override
    public boolean canAttach(Long bizId) {
        return authorizationService.hasPermission("edu:training-record:review")
                && canManage(requireRecord(bizId));
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return canAttach(bizId);
    }

    private TrainingRecordEntity requireRecord(Long id) {
        return recordMapper.selectActiveById(id);
    }

    private boolean canRead(TrainingRecordEntity record) {
        if (record == null) {
            return false;
        }
        return isAdmin()
                || Objects.equals(CurrentUserUtils.currentUserId(), record.getUserId())
                || canManage(record);
    }

    private boolean canManage(TrainingRecordEntity record) {
        if (record == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        TrainingPlanEntity plan = planMapper.selectById(record.getPlanId());
        Long userId = CurrentUserUtils.currentUserId();
        return plan != null
                && (Objects.equals(userId, plan.getOwnerId())
                        || Objects.equals(userId, plan.getTrainerId()));
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }
}
