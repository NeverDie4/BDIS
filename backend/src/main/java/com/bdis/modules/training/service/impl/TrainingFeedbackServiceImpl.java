package com.bdis.modules.training.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.training.constant.TrainingPublishStatus;
import com.bdis.modules.training.entity.TrainingFeedbackEntity;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingFeedbackMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.query.TrainingFeedbackQuery;
import com.bdis.modules.training.request.TrainingFeedbackCreateRequest;
import com.bdis.modules.training.request.TrainingFeedbackUpdateRequest;
import com.bdis.modules.training.service.TrainingFeedbackService;
import com.bdis.modules.training.vo.TrainingFeedbackDetailVO;
import com.bdis.modules.training.vo.TrainingFeedbackListVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingFeedbackServiceImpl implements TrainingFeedbackService {
    private static final String AUDIT_MODULE = "M15_TRAINING_FEEDBACK";
    private static final String BIZ_TYPE = "edu_training_feedback";
    private static final BigDecimal MIN_RATING = BigDecimal.ONE;
    private static final BigDecimal MAX_RATING = new BigDecimal("5");
    private static final Set<String> SORT_FIELDS =
            Set.of("submittedAt", "rating", "createdAt", "updatedAt");

    private final TrainingFeedbackMapper feedbackMapper;
    private final TrainingRecordMapper recordMapper;
    private final TrainingPlanMapper planMapper;
    private final AuditLogService auditLogService;

    public TrainingFeedbackServiceImpl(
            TrainingFeedbackMapper feedbackMapper,
            TrainingRecordMapper recordMapper,
            TrainingPlanMapper planMapper,
            AuditLogService auditLogService) {
        this.feedbackMapper = feedbackMapper;
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TrainingFeedbackListVO> page(TrainingFeedbackQuery query) {
        TrainingFeedbackQuery safe = query == null ? new TrainingFeedbackQuery() : query;
        validateQuery(safe);
        applyScope(safe);
        Page<TrainingFeedbackListVO> page =
                feedbackMapper.selectPageVO(Page.of(safe.getPageNo(), safe.getPageSize()), safe);
        return PageResult.of(page.getRecords(), page);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingFeedbackDetailVO getDetail(Long id) {
        requirePositive(id, "Training feedback id");
        TrainingFeedbackDetailVO detail = feedbackMapper.selectDetailById(id);
        if (detail == null) {
            throw new ResourceNotFoundException("Training feedback not found");
        }
        requireFeedbackAccess(detail.getUserId(), detail.getTrainingRecordId());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TrainingFeedbackCreateRequest request) {
        if (request == null || request.getTrainingRecordId() == null) {
            throw new BusinessException("Training record and rating are required");
        }
        validateRating(request.getRating());
        Long currentUserId = requireCurrentUser();
        TrainingRecordEntity record = requireRecord(request.getTrainingRecordId());
        requireOwner(record.getUserId(), currentUserId);
        TrainingPlanEntity plan = requireActivePlan(record.getPlanId());
        if (TrainingPublishStatus.DRAFT.equals(plan.getPublishStatus())) {
            throw conflict("Draft training plans cannot receive feedback");
        }
        if (feedbackMapper.selectByRecordAndUser(record.getId(), currentUserId) != null) {
            throw conflict("Training feedback already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        TrainingFeedbackEntity entity = new TrainingFeedbackEntity();
        entity.setTrainingRecordId(record.getId());
        entity.setUserId(currentUserId);
        entity.setRating(request.getRating());
        entity.setFeedbackContent(request.getFeedbackContent());
        entity.setSubmittedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setRemark(request.getRemark());
        try {
            if (feedbackMapper.insert(entity) == 0) {
                throw conflict("Training feedback creation failed");
            }
        } catch (DuplicateKeyException exception) {
            throw conflict("Training feedback already exists");
        }
        recordAudit("CREATE_FEEDBACK", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TrainingFeedbackUpdateRequest request) {
        requirePositive(id, "Training feedback id");
        if (request == null) {
            throw new BusinessException("Training feedback update request is required");
        }
        validateRating(request.getRating());
        Long currentUserId = requireCurrentUser();
        TrainingFeedbackEntity entity = feedbackMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Training feedback not found");
        }
        requireOwner(entity.getUserId(), currentUserId);
        TrainingRecordEntity record = requireRecord(entity.getTrainingRecordId());
        TrainingPlanEntity plan = requireActivePlan(record.getPlanId());
        if (TrainingPublishStatus.DRAFT.equals(plan.getPublishStatus())) {
            throw conflict("Draft training plans cannot receive feedback");
        }
        entity.setRating(request.getRating());
        entity.setFeedbackContent(request.getFeedbackContent());
        entity.setRemark(request.getRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        if (feedbackMapper.updateById(entity) == 0) {
            throw conflict("Training feedback update conflict");
        }
        recordAudit("UPDATE_FEEDBACK", id);
    }

    private void validateQuery(TrainingFeedbackQuery query) {
        if (query.getPageNo() == null || query.getPageNo() < 1) {
            query.setPageNo(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }
        if (query.getPageSize() > 100) {
            query.setPageSize(100);
        }
        if (query.getRating() != null) {
            validateRating(query.getRating());
        }
        if (query.getSubmittedFrom() != null
                && query.getSubmittedTo() != null
                && query.getSubmittedFrom().isAfter(query.getSubmittedTo())) {
            throw new BusinessException("Feedback submitted time range is invalid");
        }
        if (!SORT_FIELDS.contains(query.getSortField())) {
            throw new BusinessException("Unsupported feedback sort field");
        }
        if (!Set.of("asc", "desc").contains(query.getSortOrder().toLowerCase())) {
            throw new BusinessException("Unsupported feedback sort order");
        }
        query.setSortOrder(query.getSortOrder().toLowerCase());
    }

    private void validateRating(BigDecimal rating) {
        if (rating == null
                || rating.compareTo(MIN_RATING) < 0
                || rating.compareTo(MAX_RATING) > 0) {
            throw new BusinessException("Feedback rating must be between 1 and 5");
        }
    }

    private TrainingRecordEntity requireRecord(Long id) {
        TrainingRecordEntity record = recordMapper.selectById(id);
        if (record == null) {
            throw new ResourceNotFoundException("Training record not found");
        }
        return record;
    }

    private TrainingPlanEntity requireActivePlan(Long id) {
        TrainingPlanEntity plan = planMapper.selectByIdIncludingDeleted(id);
        if (plan == null
                || Objects.equals(plan.getIsDeleted(), 1)
                || !Objects.equals(plan.getStatus(), 1)) {
            throw new ResourceNotFoundException("Training plan not found or inactive");
        }
        return plan;
    }

    private Long requireCurrentUser() {
        Long id = CurrentUserUtils.currentUserId();
        if (id == null || id <= 0) {
            throw new ForbiddenException("Authenticated user is required");
        }
        return id;
    }

    private void requireOwner(Long ownerId, Long currentUserId) {
        if (!Objects.equals(ownerId, currentUserId)) {
            throw new ForbiddenException("Only the participant can submit or update feedback");
        }
    }

    private void applyScope(TrainingFeedbackQuery query) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(role -> "ADMIN".equalsIgnoreCase(role))) {
            query.setScopeAll(true);
        } else {
            query.setScopeAll(false);
            query.setScopeUserId(CurrentUserUtils.currentUserId());
        }
    }

    private void requireFeedbackAccess(Long feedbackUserId, Long recordId) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(role -> "ADMIN".equalsIgnoreCase(role))) {
            return;
        }
        Long userId = CurrentUserUtils.currentUserId();
        TrainingRecordEntity record = requireRecord(recordId);
        TrainingPlanEntity plan = requireActivePlan(record.getPlanId());
        if (Objects.equals(feedbackUserId, userId)
                || Objects.equals(plan.getOwnerId(), userId)
                || Objects.equals(plan.getTrainerId(), userId)) {
            return;
        }
        throw new ForbiddenException("Training feedback is outside the current user's scope");
    }

    private void requirePositive(Long id, String label) {
        if (id == null || id <= 0) {
            throw new BusinessException(label + " must be positive");
        }
    }

    private void recordAudit(String operationType, Long id) {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule(AUDIT_MODULE);
        dto.setOperationType(operationType);
        dto.setBizType(BIZ_TYPE);
        dto.setBizId(id);
        auditLogService.record(dto);
    }

    private static BusinessException conflict(String message) {
        return new BusinessException(ResultCodeEnum.CONFLICT, message);
    }
}
