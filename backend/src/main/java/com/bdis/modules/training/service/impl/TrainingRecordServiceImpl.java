package com.bdis.modules.training.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.training.constant.AttendanceStatus;
import com.bdis.modules.training.constant.TrainingPublishStatus;
import com.bdis.modules.training.constant.TrainingStatus;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.mapper.TrainingFeedbackMapper;
import com.bdis.modules.training.query.TrainingRecordQuery;
import com.bdis.modules.training.request.TrainingRecordCreateRequest;
import com.bdis.modules.training.request.TrainingRecordUpdateRequest;
import com.bdis.modules.training.request.TrainingParticipantBatchRequest;
import com.bdis.modules.training.service.TrainingRecordService;
import com.bdis.modules.training.vo.TrainingRecordDetailVO;
import com.bdis.modules.training.vo.TrainingRecordListVO;
import com.bdis.modules.training.vo.TrainingParticipantBatchResultVO;
import com.bdis.modules.training.vo.TrainingParticipantFailureVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainingRecordServiceImpl implements TrainingRecordService {
    private static final String AUDIT_MODULE = "M15_TRAINING_RECORD";
    private static final String BIZ_TYPE = "edu_training_record";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final TrainingRecordMapper recordMapper;
    private final TrainingFeedbackMapper feedbackMapper;
    private final TrainingPlanMapper planMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    public TrainingRecordServiceImpl(
            TrainingRecordMapper recordMapper,
            TrainingFeedbackMapper feedbackMapper,
            TrainingPlanMapper planMapper,
            UserMapper userMapper,
            AuditLogService auditLogService) {
        this.recordMapper = recordMapper;
        this.feedbackMapper = feedbackMapper;
        this.planMapper = planMapper;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TrainingRecordListVO> page(TrainingRecordQuery query) {
        TrainingRecordQuery safe = query == null ? new TrainingRecordQuery() : query;
        validateQuery(safe);
        Page<TrainingRecordListVO> page =
                recordMapper.selectPageVO(
                        Page.of(safe.getPageNo(), safe.getPageSize()), safe);
        return PageResult.of(page.getRecords(), page);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingRecordDetailVO getDetail(Long id) {
        if (id == null || id <= 0) throw new BusinessException("Training record id must be positive");
        TrainingRecordDetailVO vo = recordMapper.selectDetailById(id);
        if (vo == null) throw new ResourceNotFoundException("Training record not found");
        vo.setFeedback(feedbackMapper.selectDetailByRecordId(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TrainingRecordCreateRequest request) {
        if (request == null || request.getPlanId() == null || request.getUserId() == null) {
            throw new BusinessException("Training plan and participant are required");
        }
        requireCurrentOperator();
        TrainingPlanEntity plan = requireActivePlan(request.getPlanId());
        requireWritablePlan(plan);
        requireActiveUser(request.getUserId(), "Participant");
        if (recordMapper.selectByPlanAndUser(request.getPlanId(), request.getUserId()) != null) {
            throw conflict("Participant already exists in this training plan");
        }

        LocalDateTime now = LocalDateTime.now();
        TrainingRecordEntity entity = new TrainingRecordEntity();
        entity.setPlanId(plan.getId());
        entity.setCourseId(plan.getCourseId());
        entity.setUserId(request.getUserId());
        entity.setProgress(ZERO);
        entity.setTrainingStatus(TrainingStatus.NOT_STARTED);
        entity.setAttendanceStatus(AttendanceStatus.PENDING);
        entity.setScore(null);
        entity.setStartedAt(null);
        entity.setCheckedInAt(null);
        entity.setCompletedAt(null);
        entity.setResultComment(null);
        entity.setRemark(request.getRemark());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            if (recordMapper.insert(entity) == 0) throw conflict("Training record creation failed");
        } catch (DuplicateKeyException exception) {
            throw conflict("Participant already exists in this training plan");
        }
        recordAudit("CREATE", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainingParticipantBatchResultVO batchCreate(
            Long planId, TrainingParticipantBatchRequest request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BusinessException("Participant user ids are required");
        }
        requireCurrentOperator();
        TrainingPlanEntity plan = requireActivePlan(planId);
        requireWritablePlan(plan);

        List<Long> requested = request.getUserIds();
        LinkedHashMap<Long, Integer> frequency = new LinkedHashMap<>();
        for (Long userId : requested) {
            if (userId == null || userId <= 0) {
                throw new BusinessException("Participant user id must be positive");
            }
            frequency.merge(userId, 1, Integer::sum);
        }
        List<Long> uniqueIds = new ArrayList<>(frequency.keySet());
        Map<Long, UserEntity> users = new LinkedHashMap<>();
        for (UserEntity user : recordMapper.selectUsersIncludingDeleted(uniqueIds)) {
            users.put(user.getId(), user);
        }
        Set<Long> existingIds = new LinkedHashSet<>();
        for (TrainingRecordEntity record : recordMapper.selectByPlanAndUsers(planId, uniqueIds)) {
            existingIds.add(record.getUserId());
        }

        TrainingParticipantBatchResultVO result = new TrainingParticipantBatchResultVO();
        result.setRequestedCount(requested.size());
        result.setUniqueUserCount(uniqueIds.size());
        frequency.forEach((userId, count) -> {
            if (count > 1) result.getDuplicateUserIds().add(userId);
        });

        LocalDateTime now = LocalDateTime.now();
        for (Long userId : uniqueIds) {
            if (existingIds.contains(userId)) {
                if (!result.getDuplicateUserIds().contains(userId)) {
                    result.getDuplicateUserIds().add(userId);
                }
                continue;
            }
            UserEntity user = users.get(userId);
            if (user == null) {
                result.getFailures().add(new TrainingParticipantFailureVO(userId, "User not found"));
                continue;
            }
            if (Objects.equals(user.getIsDeleted(), 1)) {
                result.getFailures().add(new TrainingParticipantFailureVO(userId, "User is deleted"));
                continue;
            }
            if (!Objects.equals(user.getStatus(), 1)) {
                result.getFailures().add(new TrainingParticipantFailureVO(userId, "User is disabled"));
                continue;
            }
            TrainingRecordEntity entity = newDefaultRecord(
                    plan, userId, request.getRemark(), now);
            try {
                if (recordMapper.insert(entity) == 0) {
                    throw conflict("Training record batch creation failed");
                }
                result.getSuccessUserIds().add(userId);
            } catch (DuplicateKeyException exception) {
                if (!result.getDuplicateUserIds().contains(userId)) {
                    result.getDuplicateUserIds().add(userId);
                }
            }
        }
        result.setSuccessCount(result.getSuccessUserIds().size());
        result.setDuplicateCount(
                requested.size() - uniqueIds.size()
                        + existingIds.stream().mapToInt(id -> frequency.containsKey(id) ? 1 : 0).sum()
                        + result.getDuplicateUserIds().stream()
                                .filter(id -> !existingIds.contains(id) && frequency.getOrDefault(id, 0) == 1)
                                .mapToInt(id -> 1).sum());
        result.setFailureCount(result.getFailures().size());
        recordAudit("BATCH_ADD_PARTICIPANTS", planId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TrainingRecordUpdateRequest request) {
        if (request == null) throw new BusinessException("Training record update request is required");
        requireCurrentOperator();
        TrainingRecordEntity entity = requireRecord(id);
        TrainingPlanEntity plan = requireActivePlan(entity.getPlanId());
        requireWritablePlan(plan);
        if (TrainingStatus.COMPLETED.equals(entity.getTrainingStatus())) {
            throw conflict("Completed training records are read-only");
        }

        String trainingStatus =
                StringUtils.hasText(request.getTrainingStatus())
                        ? request.getTrainingStatus()
                        : entity.getTrainingStatus();
        String attendanceStatus =
                StringUtils.hasText(request.getAttendanceStatus())
                        ? request.getAttendanceStatus()
                        : entity.getAttendanceStatus();
        validateStatus(trainingStatus, attendanceStatus);
        validateRange(request.getProgress(), "progress");
        validateRange(request.getScore(), "score");
        if (TrainingPublishStatus.DRAFT.equals(plan.getPublishStatus())
                && (TrainingStatus.COMPLETED.equals(trainingStatus)
                        || TrainingStatus.FAILED.equals(trainingStatus))) {
            throw conflict("Draft training plans cannot have completed or failed records");
        }

        entity.setTrainingStatus(trainingStatus);
        entity.setAttendanceStatus(attendanceStatus);
        if (request.getProgress() != null) entity.setProgress(request.getProgress());
        if (request.getScore() != null) entity.setScore(request.getScore());
        if (request.getStartedAt() != null) entity.setStartedAt(request.getStartedAt());
        if (request.getCheckedInAt() != null) entity.setCheckedInAt(request.getCheckedInAt());
        if (request.getCompletedAt() != null) entity.setCompletedAt(request.getCompletedAt());
        if (request.getResultComment() != null) entity.setResultComment(request.getResultComment());
        if (request.getRemark() != null) entity.setRemark(request.getRemark());
        LocalDateTime now = LocalDateTime.now();
        if (TrainingStatus.LEARNING.equals(trainingStatus) && entity.getStartedAt() == null) {
            entity.setStartedAt(now);
        }
        if (TrainingStatus.COMPLETED.equals(trainingStatus) && entity.getCompletedAt() == null) {
            entity.setCompletedAt(now);
        }
        if ((AttendanceStatus.PRESENT.equals(attendanceStatus)
                        || AttendanceStatus.LATE.equals(attendanceStatus))
                && entity.getCheckedInAt() == null) {
            entity.setCheckedInAt(now);
        }
        entity.setUpdatedAt(now);
        if (recordMapper.updateById(entity) == 0) {
            throw conflict("Training record update failed");
        }
        recordAudit("UPDATE", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireCurrentOperator();
        TrainingRecordEntity record = requireRecord(id);
        TrainingPlanEntity plan = requireActivePlan(record.getPlanId());
        requireWritablePlan(plan);
        if (!isPristine(record) || defaultZero(feedbackMapper.countByRecordId(id)) > 0) {
            throw conflict("Training record has process data or feedback and cannot be removed");
        }
        if (recordMapper.deletePristine(id) == 0) {
            throw conflict("Training record changed concurrently and cannot be removed");
        }
        recordAudit("REMOVE_PARTICIPANT", id);
    }

    private TrainingRecordEntity newDefaultRecord(
            TrainingPlanEntity plan, Long userId, String remark, LocalDateTime now) {
        TrainingRecordEntity entity = new TrainingRecordEntity();
        entity.setPlanId(plan.getId());
        entity.setCourseId(plan.getCourseId());
        entity.setUserId(userId);
        entity.setProgress(ZERO);
        entity.setTrainingStatus(TrainingStatus.NOT_STARTED);
        entity.setAttendanceStatus(AttendanceStatus.PENDING);
        entity.setScore(null);
        entity.setStartedAt(null);
        entity.setCheckedInAt(null);
        entity.setCompletedAt(null);
        entity.setResultComment(null);
        entity.setRemark(remark);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private boolean isPristine(TrainingRecordEntity record) {
        return (record.getAttendanceStatus() == null
                        || AttendanceStatus.PENDING.equals(record.getAttendanceStatus()))
                && (record.getProgress() == null || record.getProgress().compareTo(ZERO) == 0)
                && (record.getTrainingStatus() == null
                        || TrainingStatus.NOT_STARTED.equals(record.getTrainingStatus()))
                && record.getScore() == null
                && record.getCheckedInAt() == null
                && record.getCompletedAt() == null;
    }

    private long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    private void validateQuery(TrainingRecordQuery query) {
        if (query.getPageNo() == null || query.getPageNo() < 1) query.setPageNo(1);
        if (query.getPageSize() == null || query.getPageSize() < 1) query.setPageSize(10);
        if (query.getPageSize() > 100) query.setPageSize(100);
        if (StringUtils.hasText(query.getTrainingStatus())
                && !TrainingStatus.VALUES.contains(query.getTrainingStatus())) {
            throw new BusinessException("Unsupported training status");
        }
        if (StringUtils.hasText(query.getAttendanceStatus())
                && !AttendanceStatus.VALUES.contains(query.getAttendanceStatus())) {
            throw new BusinessException("Unsupported attendance status");
        }
        if (query.getRecordedFrom() != null
                && query.getRecordedTo() != null
                && query.getRecordedFrom().isAfter(query.getRecordedTo())) {
            throw new BusinessException("Training record time range is invalid");
        }
    }

    private void validateStatus(String trainingStatus, String attendanceStatus) {
        if (!TrainingStatus.VALUES.contains(trainingStatus)) {
            throw new BusinessException("Unsupported training status");
        }
        if (!AttendanceStatus.VALUES.contains(attendanceStatus)) {
            throw new BusinessException("Unsupported attendance status");
        }
    }

    private void validateRange(BigDecimal value, String label) {
        if (value != null && (value.compareTo(ZERO) < 0 || value.compareTo(HUNDRED) > 0)) {
            throw new BusinessException(label + " must be between 0 and 100");
        }
    }

    private TrainingPlanEntity requireActivePlan(Long id) {
        TrainingPlanEntity plan = planMapper.selectByIdIncludingDeleted(id);
        if (plan == null || Objects.equals(plan.getIsDeleted(), 1) || !Objects.equals(plan.getStatus(), 1)) {
            throw new ResourceNotFoundException("Training plan not found or inactive");
        }
        return plan;
    }

    private void requireWritablePlan(TrainingPlanEntity plan) {
        if (TrainingPublishStatus.CLOSED.equals(plan.getPublishStatus())) {
            throw conflict("Closed training plans are read-only");
        }
    }

    private TrainingRecordEntity requireRecord(Long id) {
        if (id == null || id <= 0) throw new BusinessException("Training record id must be positive");
        TrainingRecordEntity entity = recordMapper.selectById(id);
        if (entity == null) throw new ResourceNotFoundException("Training record not found");
        return entity;
    }

    private void requireCurrentOperator() {
        Long id = CurrentUserUtils.currentUserId();
        requireActiveUser(id, "Current operator");
    }

    private UserEntity requireActiveUser(Long id, String label) {
        if (id == null || id <= 0) throw new ResourceNotFoundException(label + " not found");
        UserEntity user = userMapper.selectById(id);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            throw new ResourceNotFoundException(label + " not found or inactive");
        }
        return user;
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
