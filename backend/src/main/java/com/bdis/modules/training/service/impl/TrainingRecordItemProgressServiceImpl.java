package com.bdis.modules.training.service.impl;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.training.constant.TrainingStatus;
import com.bdis.modules.training.entity.TrainingPlanItemEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.entity.TrainingRecordItemEntity;
import com.bdis.modules.training.mapper.TrainingPlanItemMapper;
import com.bdis.modules.training.mapper.TrainingRecordItemMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.request.TrainingRecordItemProgressRequest;
import com.bdis.modules.training.service.TrainingRecordItemProgressService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingRecordItemProgressServiceImpl implements TrainingRecordItemProgressService {
    private static final Set<String> EDITABLE_TRAINING_STATUSES =
            Set.of(TrainingStatus.NOT_STARTED, TrainingStatus.LEARNING, TrainingStatus.MAKEUP);

    private final TrainingRecordMapper recordMapper;
    private final TrainingPlanItemMapper itemMapper;
    private final TrainingRecordItemMapper progressMapper;

    public TrainingRecordItemProgressServiceImpl(
            TrainingRecordMapper recordMapper,
            TrainingPlanItemMapper itemMapper,
            TrainingRecordItemMapper progressMapper) {
        this.recordMapper = recordMapper;
        this.itemMapper = itemMapper;
        this.progressMapper = progressMapper;
    }

    @Override
    @Transactional
    public TrainingRecordItemEntity save(
            Long recordId, Long itemId, TrainingRecordItemProgressRequest request) {
        Long userId = CurrentUserUtils.currentUserId();
        if (userId == null) {
            throw new ForbiddenException("Authentication is required");
        }
        TrainingRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new ResourceNotFoundException("Training record not found");
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new ForbiddenException("Only the participant can update training progress");
        }
        if (!EDITABLE_TRAINING_STATUSES.contains(record.getTrainingStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Training progress cannot be updated in "
                            + record.getTrainingStatus()
                            + " status");
        }
        TrainingPlanItemEntity item = itemMapper.selectActiveById(itemId);
        if (item == null || !Objects.equals(item.getPlanId(), record.getPlanId())) {
            throw new BusinessException("Training item is not part of this plan");
        }
        if (request == null || request.getCompleted() == null) {
            throw new BusinessException("Completion state is required");
        }
        LocalDateTime now = LocalDateTime.now();
        TrainingRecordItemEntity entity = progressMapper.selectActive(recordId, itemId);
        if (entity == null) {
            entity = new TrainingRecordItemEntity();
            entity.setTrainingRecordId(recordId);
            entity.setPlanItemId(itemId);
            entity.setStatus(1);
            entity.setIsDeleted(0);
            entity.setCreatedAt(now);
            entity.setCreatedBy(userId);
            entity.setVersion(0);
        }
        entity.setProgress(
                request.getProgress() == null
                        ? (request.getCompleted() ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                        : request.getProgress());
        entity.setCompleted(request.getCompleted() ? 1 : 0);
        entity.setSubmittedFileId(request.getSubmittedFileId());
        entity.setStartedAt(entity.getStartedAt() == null ? now : entity.getStartedAt());
        entity.setCompletedAt(request.getCompleted() ? now : null);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(userId);
        int affected =
                entity.getId() == null
                        ? progressMapper.insert(entity)
                        : progressMapper.updateById(entity);
        if (affected == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Training item progress save conflict");
        }
        boolean startedLearning = TrainingStatus.NOT_STARTED.equals(record.getTrainingStatus());
        if (startedLearning) {
            record.setTrainingStatus(TrainingStatus.LEARNING);
            record.setStartedAt(record.getStartedAt() == null ? now : record.getStartedAt());
        }
        int required = progressMapper.countRequiredItems(record.getPlanId());
        int completed = progressMapper.countCompletedRequiredItems(recordId);
        if (required > 0) {
            record.setProgress(
                    BigDecimal.valueOf(completed * 100.0 / required)
                            .setScale(2, RoundingMode.HALF_UP));
        }
        if (startedLearning || required > 0) {
            record.setUpdatedAt(now);
            if (recordMapper.updateById(record) == 0) {
                throw new BusinessException("Training record progress update conflict");
            }
        }
        return entity;
    }
}
