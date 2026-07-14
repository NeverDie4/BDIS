package com.bdis.modules.training.service.impl;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.training.constant.TrainingPublishStatus;
import com.bdis.modules.training.entity.TrainingMaterialEntity;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingPlanMaterialEntity;
import com.bdis.modules.training.mapper.TrainingMaterialMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingPlanMaterialMapper;
import com.bdis.modules.training.request.TrainingPlanMaterialBindRequest;
import com.bdis.modules.training.service.TrainingPlanMaterialService;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingPlanMaterialServiceImpl implements TrainingPlanMaterialService {
    private static final String AUDIT_MODULE = "M15_TRAINING_PLAN_MATERIAL";
    private static final String BIZ_TYPE = "rel_training_plan_material";

    private final TrainingPlanMapper planMapper;
    private final TrainingMaterialMapper materialMapper;
    private final TrainingPlanMaterialMapper relationMapper;
    private final FileResourceMapper fileMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    public TrainingPlanMaterialServiceImpl(
            TrainingPlanMapper planMapper,
            TrainingMaterialMapper materialMapper,
            TrainingPlanMaterialMapper relationMapper,
            FileResourceMapper fileMapper,
            UserMapper userMapper,
            AuditLogService auditLogService) {
        this.planMapper = planMapper;
        this.materialMapper = materialMapper;
        this.relationMapper = relationMapper;
        this.fileMapper = fileMapper;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingPlanMaterialVO> list(Long planId) {
        TrainingPlanEntity plan = requireActivePlan(planId);
        requirePlanAccess(plan, false);
        return relationMapper.selectListVO(planId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bind(Long planId, TrainingPlanMaterialBindRequest request) {
        TrainingPlanEntity plan = requireEditablePlan(planId);
        requirePlanAccess(plan, true);
        if (request == null || request.getMaterialId() == null || request.getMaterialId() <= 0) {
            throw new BusinessException("Training material id is required");
        }
        TrainingMaterialEntity material = requireEnabledMaterial(request.getMaterialId());
        requireActiveFile(material.getFileId());
        if (relationMapper.selectByPlanAndMaterial(planId, request.getMaterialId()) != null) {
            throw conflict("Training material is already bound to this plan");
        }
        Long operatorId = requireCurrentOperator();
        LocalDateTime now = LocalDateTime.now();
        TrainingPlanMaterialEntity relation = new TrainingPlanMaterialEntity();
        relation.setPlanId(planId);
        relation.setMaterialId(request.getMaterialId());
        relation.setIsRequired(request.getIsRequired() == null ? 1 : request.getIsRequired());
        relation.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        relation.setCreatedAt(now);
        relation.setCreatedBy(operatorId);
        relation.setRemark(request.getRemark());
        try {
            if (relationMapper.insert(relation) == 0) {
                throw conflict("Training material binding failed");
            }
        } catch (DuplicateKeyException exception) {
            throw conflict("Training material is already bound to this plan");
        }
        if (materialMapper.incrementReuseCount(material.getId()) == 0) {
            throw conflict("Training material reuse count update failed");
        }
        recordAudit("BIND", relation.getId());
        return relation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long planId, Long materialId) {
        TrainingPlanEntity plan = requireEditablePlan(planId);
        requirePlanAccess(plan, true);
        if (materialId == null || materialId <= 0) {
            throw new BusinessException("Training material id must be positive");
        }
        TrainingPlanMaterialEntity relation =
                relationMapper.selectByPlanAndMaterial(planId, materialId);
        if (relation == null) {
            throw new ResourceNotFoundException("Training plan material binding not found");
        }
        requireCurrentOperator();
        if (relationMapper.deleteById(relation.getId()) == 0) {
            throw conflict("Training material unbind failed");
        }
        if (materialMapper.decrementReuseCount(materialId) == 0) {
            throw conflict("Training material reuse count is already zero or material is invalid");
        }
        recordAudit("UNBIND", relation.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidMaterial(Long planId) {
        Long count = relationMapper.countValidMaterialsForPublish(planId);
        return count != null && count > 0;
    }

    private TrainingPlanEntity requireActivePlan(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Training plan id must be positive");
        }
        TrainingPlanEntity plan = planMapper.selectByIdIncludingDeleted(id);
        if (plan == null || Objects.equals(plan.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Training plan not found");
        }
        return plan;
    }

    private TrainingPlanEntity requireEditablePlan(Long id) {
        TrainingPlanEntity plan = requireActivePlan(id);
        if (!TrainingPublishStatus.DRAFT.equals(plan.getPublishStatus())) {
            throw conflict("Only draft training plans can modify materials");
        }
        return plan;
    }

    private void requirePlanAccess(TrainingPlanEntity plan, boolean manage) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(
                                role -> SecurityConstants.ADMIN_ROLE_CODE.equalsIgnoreCase(role))) {
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
                        ? "Only the training plan owner can manage materials"
                        : "Training plan materials are outside the current user's scope");
    }

    private TrainingMaterialEntity requireEnabledMaterial(Long id) {
        TrainingMaterialEntity material = materialMapper.selectByIdIncludingDeleted(id);
        if (material == null || Objects.equals(material.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Training material not found");
        }
        if (!Objects.equals(material.getStatus(), 1)) {
            throw conflict("Disabled training material cannot be bound");
        }
        return material;
    }

    private FileResourceEntity requireActiveFile(Long id) {
        FileResourceEntity file = id == null ? null : fileMapper.selectById(id);
        if (file == null
                || Objects.equals(file.getIsDeleted(), 1)
                || !Objects.equals(file.getStatus(), 1)) {
            throw new ResourceNotFoundException("Training material file not found or inactive");
        }
        return file;
    }

    private Long requireCurrentOperator() {
        Long id = CurrentUserUtils.currentUserId();
        UserEntity user = id == null || id <= 0 ? null : userMapper.selectById(id);
        if (user == null
                || Objects.equals(user.getIsDeleted(), 1)
                || !Objects.equals(user.getStatus(), 1)) {
            throw new ResourceNotFoundException("Current operator not found or inactive");
        }
        return id;
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
