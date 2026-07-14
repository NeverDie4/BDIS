package com.bdis.modules.training.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.service.TrainingPlanService;
import com.bdis.modules.training.service.TrainingSummaryService;
import com.bdis.modules.training.vo.TrainingSummaryVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingSummaryServiceImpl implements TrainingSummaryService {
    private final TrainingPlanMapper planMapper;
    private final TrainingRecordMapper recordMapper;
    private final TrainingPlanService planService;

    public TrainingSummaryServiceImpl(
            TrainingPlanMapper planMapper,
            TrainingRecordMapper recordMapper,
            TrainingPlanService planService) {
        this.planMapper = planMapper;
        this.recordMapper = recordMapper;
        this.planService = planService;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingSummaryVO getSummary(Long planId) {
        if (planId == null || planId <= 0) {
            throw new BusinessException("Training plan id must be positive");
        }
        planService.requireViewAccess(planId);
        TrainingPlanEntity plan = planMapper.selectByIdIncludingDeleted(planId);
        if (plan == null || Objects.equals(plan.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Training plan not found");
        }
        TrainingSummaryVO summary = recordMapper.selectSummary(planId);
        if (summary == null) {
            summary = new TrainingSummaryVO();
            summary.setPlanId(plan.getId());
            summary.setPlanNo(plan.getPlanNo());
            summary.setPlanName(plan.getPlanName());
            summary.setPublishStatus(plan.getPublishStatus());
        }
        normalize(summary);
        return summary;
    }

    private void normalize(TrainingSummaryVO summary) {
        summary.setTotalParticipantCount(zero(summary.getTotalParticipantCount()));
        summary.setCompletedCount(zero(summary.getCompletedCount()));
        summary.setFailedCount(zero(summary.getFailedCount()));
        summary.setMakeupCount(zero(summary.getMakeupCount()));
        summary.setNotStartedCount(zero(summary.getNotStartedCount()));
        summary.setLearningCount(zero(summary.getLearningCount()));
        summary.setPendingAttendanceCount(zero(summary.getPendingAttendanceCount()));
        summary.setPresentCount(zero(summary.getPresentCount()));
        summary.setLateCount(zero(summary.getLateCount()));
        summary.setAbsentCount(zero(summary.getAbsentCount()));
        summary.setLeaveCount(zero(summary.getLeaveCount()));
        summary.setScoredParticipantCount(zero(summary.getScoredParticipantCount()));
        summary.setFeedbackCount(zero(summary.getFeedbackCount()));
        if (summary.getAverageProgress() == null) {
            summary.setAverageProgress(BigDecimal.ZERO.setScale(2));
        } else {
            summary.setAverageProgress(
                    summary.getAverageProgress().setScale(2, RoundingMode.HALF_UP));
        }
        if (summary.getAverageScore() != null) {
            summary.setAverageScore(summary.getAverageScore().setScale(2, RoundingMode.HALF_UP));
        }
        if (summary.getAverageRating() != null) {
            summary.setAverageRating(summary.getAverageRating().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private long zero(Long value) {
        return value == null ? 0L : value;
    }
}
