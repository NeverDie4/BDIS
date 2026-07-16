package com.bdis.modules.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.training.constant.TrainingPlanType;
import com.bdis.modules.training.constant.TrainingPublishStatus;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.query.TrainingPlanQuery;
import com.bdis.modules.training.request.TrainingPlanCloseRequest;
import com.bdis.modules.training.request.TrainingPlanCreateRequest;
import com.bdis.modules.training.request.TrainingPlanPublishRequest;
import com.bdis.modules.training.request.TrainingPlanUpdateRequest;
import com.bdis.modules.training.service.TrainingPlanMaterialService;
import com.bdis.modules.training.service.TrainingPlanService;
import com.bdis.modules.training.vo.TrainingPlanDetailVO;
import com.bdis.modules.training.vo.TrainingPlanListVO;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainingPlanServiceImpl implements TrainingPlanService {
    private static final String AUDIT_MODULE = "M15_TRAINING_PLAN";
    private static final String BIZ_TYPE = "edu_training_plan";

    private final TrainingPlanMapper planMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final TrainingPlanMaterialService planMaterialService;
    private final AuditLogService auditLogService;

    public TrainingPlanServiceImpl(
            TrainingPlanMapper planMapper,
            UserMapper userMapper,
            CourseMapper courseMapper,
            TrainingPlanMaterialService planMaterialService,
            AuditLogService auditLogService) {
        this.planMapper = planMapper;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.planMaterialService = planMaterialService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TrainingPlanListVO> page(TrainingPlanQuery query) {
        TrainingPlanQuery safe = query == null ? new TrainingPlanQuery() : query;
        validateQuery(safe);
        Page<TrainingPlanEntity> page =
                planMapper.selectPage(
                        Page.of(safe.getPageNo(), safe.getPageSize()), buildWrapper(safe));
        Lookup lookup = loadLookup(page.getRecords());
        List<TrainingPlanListVO> records =
                page.getRecords().stream().map(entity -> toListVO(entity, lookup)).toList();
        return PageResult.of(records, page);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingPlanDetailVO getDetail(Long id) {
        TrainingPlanEntity entity = requireActive(id);
        requirePlanAccess(entity, false);
        Lookup lookup = loadLookup(List.of(entity));
        TrainingPlanDetailVO vo = new TrainingPlanDetailVO();
        copyListFields(entity, vo, lookup);
        vo.setDescription(entity.getDescription());
        vo.setPublishedAt(entity.getPublishedAt());
        vo.setPublishedBy(entity.getPublishedBy());
        UserEntity publisher = lookup.users().get(entity.getPublishedBy());
        vo.setPublishedByName(publisher == null ? null : publisher.getRealName());
        vo.setParticipantCount(defaultZero(planMapper.countRecords(id)));
        List<TrainingPlanMaterialVO> materials = planMaterialService.list(id);
        if (materials == null) {
            materials = List.of();
        }
        vo.setMaterials(materials);
        vo.setMaterialCount((long) materials.size());
        vo.setRequiredMaterialCount(
                materials.stream().filter(item -> Objects.equals(item.getIsRequired(), 1)).count());
        vo.setRemark(entity.getRemark());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setUpdatedBy(entity.getUpdatedBy());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireViewAccess(Long id) {
        TrainingPlanEntity entity = requireActive(id);
        requirePlanAccess(entity, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TrainingPlanCreateRequest request) {
        validateCreate(request);
        Long operatorId = requireCurrentOperator();
        ensurePlanNoAvailable(request.getPlanNo());
        validateReferences(request.getOwnerId(), request.getTrainerId(), request.getCourseId());

        LocalDateTime now = LocalDateTime.now();
        TrainingPlanEntity entity = new TrainingPlanEntity();
        entity.setPlanNo(request.getPlanNo().trim());
        applyEditableFields(entity, request);
        entity.setPublishStatus(TrainingPublishStatus.DRAFT);
        entity.setPublishedAt(null);
        entity.setPublishedBy(null);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setVersion(0);
        try {
            if (planMapper.insert(entity) == 0) {
                throw conflict("Training plan creation failed");
            }
        } catch (DuplicateKeyException exception) {
            throw conflict("Training plan number already exists");
        }
        recordAudit("CREATE", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TrainingPlanUpdateRequest request) {
        TrainingPlanEntity entity = requireActive(id);
        requirePlanAccess(entity, true);
        requireDraft(entity, "Only draft training plans can be updated");
        if (request == null || request.getVersion() == null) {
            throw new BusinessException("Training plan update request and version are required");
        }
        if (!Objects.equals(request.getVersion(), entity.getVersion())) {
            throw conflict("Training plan version conflict");
        }
        validateEditable(
                request.getPlanName(),
                request.getPlanType(),
                request.getOwnerId(),
                request.getStartedAt(),
                request.getEndedAt());
        Long operatorId = requireCurrentOperator();
        validateReferences(request.getOwnerId(), request.getTrainerId(), request.getCourseId());
        entity.setPlanName(request.getPlanName().trim());
        entity.setPlanType(request.getPlanType().trim());
        entity.setOwnerId(request.getOwnerId());
        entity.setCourseId(request.getCourseId());
        entity.setTrainerId(request.getTrainerId());
        entity.setDescription(request.getDescription());
        entity.setCompletionCriteria(request.getCompletionCriteria());
        entity.setLocation(request.getLocation());
        entity.setStartedAt(request.getStartedAt());
        entity.setEndedAt(request.getEndedAt());
        entity.setRemark(request.getRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(operatorId);
        if (planMapper.updateById(entity) == 0) {
            throw conflict("Training plan version conflict");
        }
        recordAudit("UPDATE", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TrainingPlanEntity entity = requireActive(id);
        requirePlanAccess(entity, true);
        requireDraft(entity, "Only draft training plans can be deleted");
        Long count = planMapper.countRecords(id);
        if (count != null && count > 0) {
            throw conflict("Training plan with participants cannot be deleted");
        }
        Long materialCount = planMapper.countMaterials(id);
        if (materialCount != null && materialCount > 0) {
            throw conflict("Training plan with material bindings cannot be deleted");
        }
        Long operatorId = requireCurrentOperator();
        if (planMapper.logicalDelete(id, entity.getVersion(), LocalDateTime.now(), operatorId)
                == 0) {
            throw conflict("Training plan version conflict");
        }
        recordAudit("DELETE", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, TrainingPlanPublishRequest request) {
        TrainingPlanEntity entity = requireActive(id);
        requirePlanAccess(entity, true);
        requireDraft(entity, "Only draft training plans can be published");
        if (request == null || request.getVersion() == null) {
            throw new BusinessException("Training plan publish version is required");
        }
        if (!Objects.equals(request.getVersion(), entity.getVersion())) {
            throw conflict("Training plan version conflict");
        }
        boolean validCourse = false;
        if (entity.getCourseId() != null) {
            CourseEntity course = courseMapper.selectById(entity.getCourseId());
            validCourse = course != null && Objects.equals(course.getStatus(), 1);
        }
        if (!validCourse
                && !planMaterialService.hasValidMaterial(id)
                && (planMapper.countValidStructuredBindings(id) == null
                        || planMapper.countValidStructuredBindings(id) == 0)) {
            throw conflict(
                    "Training plan requires a course or at least one material before publishing");
        }
        Long operatorId = requireCurrentOperator();
        if (planMapper.publish(id, request.getVersion(), LocalDateTime.now(), operatorId) == 0) {
            throw conflict("Training plan publish state or version conflict");
        }
        recordAudit("PUBLISH", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, TrainingPlanCloseRequest request) {
        TrainingPlanEntity entity = requireActive(id);
        requirePlanAccess(entity, true);
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new BusinessException("Training plan close reason is required");
        }
        if (request.getVersion() == null) {
            throw new BusinessException("Training plan close version is required");
        }
        if (!TrainingPublishStatus.PUBLISHED.equals(entity.getPublishStatus())) {
            throw conflict("Only published training plans can be closed");
        }
        if (!Objects.equals(request.getVersion(), entity.getVersion())) {
            throw conflict("Training plan version conflict");
        }
        Long operatorId = requireCurrentOperator();
        if (planMapper.close(id, request.getVersion(), LocalDateTime.now(), operatorId) == 0) {
            throw conflict("Training plan close state or version conflict");
        }
        recordAudit("CLOSE", id);
    }

    private void validateCreate(TrainingPlanCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getPlanNo())) {
            throw new BusinessException("Training plan number is required");
        }
        validateEditable(
                request.getPlanName(),
                request.getPlanType(),
                request.getOwnerId(),
                request.getStartedAt(),
                request.getEndedAt());
    }

    private void validateEditable(
            String planName,
            String planType,
            Long ownerId,
            LocalDateTime startedAt,
            LocalDateTime endedAt) {
        if (!StringUtils.hasText(planName) || !StringUtils.hasText(planType) || ownerId == null) {
            throw new BusinessException("Training plan name, type and owner are required");
        }
        if (!TrainingPlanType.VALUES.contains(planType)) {
            throw new BusinessException("Unsupported training plan type");
        }
        if (startedAt != null && endedAt != null && !endedAt.isAfter(startedAt)) {
            throw new BusinessException("Training plan end time must be after start time");
        }
    }

    private void validateQuery(TrainingPlanQuery query) {
        if (query.getPageNo() == null || query.getPageNo() < 1) {
            query.setPageNo(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }
        if (query.getPageSize() > 100) {
            query.setPageSize(100);
        }
        if (StringUtils.hasText(query.getPlanType())
                && !TrainingPlanType.VALUES.contains(query.getPlanType())) {
            throw new BusinessException("Unsupported training plan type");
        }
        if (StringUtils.hasText(query.getPublishStatus())
                && !TrainingPublishStatus.VALUES.contains(query.getPublishStatus())) {
            throw new BusinessException("Unsupported training publish status");
        }
        requireRange(query.getStartedFrom(), query.getStartedTo(), "startedAt");
        requireRange(query.getEndedFrom(), query.getEndedTo(), "endedAt");
    }

    private void validateReferences(Long ownerId, Long trainerId, Long courseId) {
        requireActiveUser(ownerId, "Training owner");
        if (trainerId != null) {
            requireActiveUser(trainerId, "Training trainer");
        }
        if (courseId != null) {
            CourseEntity course = courseMapper.selectById(courseId);
            if (course == null || !Objects.equals(course.getStatus(), 1)) {
                throw new ResourceNotFoundException("Related course not found or inactive");
            }
        }
    }

    private Long requireCurrentOperator() {
        Long id = CurrentUserUtils.currentUserId();
        requireActiveUser(id, "Current operator");
        return id;
    }

    private UserEntity requireActiveUser(Long id, String label) {
        if (id == null || id <= 0) {
            throw new ResourceNotFoundException(label + " not found");
        }
        UserEntity user = userMapper.selectById(id);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            throw new ResourceNotFoundException(label + " not found or inactive");
        }
        return user;
    }

    private void ensurePlanNoAvailable(String planNo) {
        if (planMapper.selectByPlanNoIncludingDeleted(planNo.trim()) != null) {
            throw conflict("Training plan number already exists");
        }
    }

    private TrainingPlanEntity requireActive(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Training plan id must be positive");
        }
        TrainingPlanEntity entity = planMapper.selectByIdIncludingDeleted(id);
        if (entity == null || Objects.equals(entity.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Training plan not found");
        }
        return entity;
    }

    private void requireDraft(TrainingPlanEntity entity, String message) {
        if (!TrainingPublishStatus.DRAFT.equals(entity.getPublishStatus())) {
            throw conflict(message);
        }
    }

    private LambdaQueryWrapper<TrainingPlanEntity> buildWrapper(TrainingPlanQuery query) {
        LambdaQueryWrapper<TrainingPlanEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    w ->
                            w.like(TrainingPlanEntity::getPlanNo, query.getKeyword())
                                    .or()
                                    .like(TrainingPlanEntity::getPlanName, query.getKeyword()));
        }
        wrapper.eq(
                StringUtils.hasText(query.getPlanType()),
                TrainingPlanEntity::getPlanType,
                query.getPlanType());
        wrapper.eq(
                StringUtils.hasText(query.getPublishStatus()),
                TrainingPlanEntity::getPublishStatus,
                query.getPublishStatus());
        wrapper.eq(query.getOwnerId() != null, TrainingPlanEntity::getOwnerId, query.getOwnerId());
        wrapper.eq(
                query.getTrainerId() != null,
                TrainingPlanEntity::getTrainerId,
                query.getTrainerId());
        wrapper.ge(
                query.getStartedFrom() != null,
                TrainingPlanEntity::getStartedAt,
                query.getStartedFrom());
        wrapper.le(
                query.getStartedTo() != null,
                TrainingPlanEntity::getStartedAt,
                query.getStartedTo());
        wrapper.ge(
                query.getEndedFrom() != null, TrainingPlanEntity::getEndedAt, query.getEndedFrom());
        wrapper.le(query.getEndedTo() != null, TrainingPlanEntity::getEndedAt, query.getEndedTo());
        applyUserScope(wrapper);
        wrapper.orderByDesc(TrainingPlanEntity::getStartedAt)
                .orderByDesc(TrainingPlanEntity::getId);
        return wrapper;
    }

    private void applyUserScope(LambdaQueryWrapper<TrainingPlanEntity> wrapper) {
        if (!hasScopedIdentity() || isAdmin()) {
            return;
        }
        wrapper.eq(TrainingPlanEntity::getPublishStatus, TrainingPublishStatus.PUBLISHED);
    }

    private void requirePlanAccess(TrainingPlanEntity plan, boolean manage) {
        if (!hasScopedIdentity() || isAdmin()) {
            return;
        }
        if (TrainingPublishStatus.PUBLISHED.equals(plan.getPublishStatus())) {
            return;
        }
        Long userId = CurrentUserUtils.currentUserId();
        if (Objects.equals(userId, plan.getOwnerId())
                || Objects.equals(userId, plan.getTrainerId())) {
            return;
        }
        if (!manage && planMapper.countActiveRecordsForUser(plan.getId(), userId) > 0) {
            return;
        }
        throw new ForbiddenException(
                manage
                        ? "Only the training plan owner can manage this plan"
                        : "Training plan is outside the current user's scope");
    }

    private boolean hasScopedIdentity() {
        return !CurrentUserUtils.currentRoleCodes().isEmpty();
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream()
                .anyMatch(role -> SecurityConstants.ADMIN_ROLE_CODE.equalsIgnoreCase(role));
    }

    private Lookup loadLookup(List<TrainingPlanEntity> entities) {
        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> courseIds = new LinkedHashSet<>();
        for (TrainingPlanEntity entity : entities) {
            if (entity.getOwnerId() != null) {
                userIds.add(entity.getOwnerId());
            }
            if (entity.getTrainerId() != null) {
                userIds.add(entity.getTrainerId());
            }
            if (entity.getPublishedBy() != null) {
                userIds.add(entity.getPublishedBy());
            }
            if (entity.getCourseId() != null) {
                courseIds.add(entity.getCourseId());
            }
        }
        Map<Long, UserEntity> users = new HashMap<>();
        Map<Long, CourseEntity> courses = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(value -> users.put(value.getId(), value));
        }
        if (!courseIds.isEmpty()) {
            courseMapper
                    .selectBatchIds(courseIds)
                    .forEach(value -> courses.put(value.getId(), value));
        }
        return new Lookup(users, courses);
    }

    private TrainingPlanListVO toListVO(TrainingPlanEntity entity, Lookup lookup) {
        TrainingPlanListVO vo = new TrainingPlanListVO();
        copyListFields(entity, vo, lookup);
        return vo;
    }

    private void copyListFields(TrainingPlanEntity entity, TrainingPlanListVO vo, Lookup lookup) {
        vo.setId(entity.getId());
        vo.setPlanNo(entity.getPlanNo());
        vo.setPlanName(entity.getPlanName());
        vo.setPlanType(entity.getPlanType());
        vo.setOwnerId(entity.getOwnerId());
        UserEntity owner = lookup.users().get(entity.getOwnerId());
        vo.setOwnerName(owner == null ? null : owner.getRealName());
        vo.setCourseId(entity.getCourseId());
        CourseEntity course = lookup.courses().get(entity.getCourseId());
        vo.setCourseName(course == null ? null : course.getCourseName());
        vo.setTrainerId(entity.getTrainerId());
        UserEntity trainer = lookup.users().get(entity.getTrainerId());
        vo.setTrainerName(trainer == null ? null : trainer.getRealName());
        vo.setLocation(entity.getLocation());
        vo.setStartedAt(entity.getStartedAt());
        vo.setEndedAt(entity.getEndedAt());
        vo.setPublishStatus(entity.getPublishStatus());
        vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
    }

    private void applyEditableFields(TrainingPlanEntity entity, TrainingPlanCreateRequest request) {
        entity.setPlanName(request.getPlanName().trim());
        entity.setPlanType(request.getPlanType().trim());
        entity.setOwnerId(request.getOwnerId());
        entity.setCourseId(request.getCourseId());
        entity.setTrainerId(request.getTrainerId());
        entity.setDescription(request.getDescription());
        entity.setLocation(request.getLocation());
        entity.setStartedAt(request.getStartedAt());
        entity.setEndedAt(request.getEndedAt());
        entity.setRemark(request.getRemark());
    }

    private void recordAudit(String operationType, Long id) {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule(AUDIT_MODULE);
        dto.setOperationType(operationType);
        dto.setBizType(BIZ_TYPE);
        dto.setBizId(id);
        auditLogService.record(dto);
    }

    private static void requireRange(LocalDateTime from, LocalDateTime to, String name) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(name + " range is invalid");
        }
    }

    private static long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    private static BusinessException conflict(String message) {
        return new BusinessException(ResultCodeEnum.CONFLICT, message);
    }

    private record Lookup(Map<Long, UserEntity> users, Map<Long, CourseEntity> courses) {}
}
