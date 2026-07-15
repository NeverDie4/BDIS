package com.bdis.modules.training.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingPlanItemEntity;
import com.bdis.modules.training.mapper.TrainingPlanItemMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.request.TrainingPlanItemRequest;
import com.bdis.modules.training.service.TrainingPlanItemService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingPlanItemServiceImpl implements TrainingPlanItemService {
    private final TrainingPlanMapper planMapper;
    private final TrainingPlanItemMapper itemMapper;

    public TrainingPlanItemServiceImpl(
            TrainingPlanMapper planMapper, TrainingPlanItemMapper itemMapper) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingPlanItemEntity> list(Long planId) {
        requirePlan(planId);
        return itemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                TrainingPlanItemEntity>()
                        .eq(TrainingPlanItemEntity::getPlanId, planId)
                        .eq(TrainingPlanItemEntity::getIsDeleted, 0)
                        .orderByAsc(TrainingPlanItemEntity::getSortOrder));
    }

    @Override
    @Transactional
    public Long create(Long planId, TrainingPlanItemRequest r) {
        TrainingPlanEntity p = requirePlan(planId);
        manage(p);
        if (r == null) {
            throw new BusinessException("Training item is required");
        }
        TrainingPlanItemEntity e = toEntity(planId, r);
        if (itemMapper.insert(e) == 0) {
            throw new BusinessException("Training item creation failed");
        }
        return e.getId();
    }

    @Override
    @Transactional
    public void update(Long id, TrainingPlanItemRequest r) {
        TrainingPlanItemEntity e = itemMapper.selectById(id);
        if (e == null) {
            throw new ResourceNotFoundException("Training item not found");
        }
        manage(requirePlan(e.getPlanId()));
        e.setItemType(r.getItemType());
        e.setItemTitle(r.getItemTitle());
        e.setDescription(r.getDescription());
        e.setCourseId(r.getCourseId());
        e.setProjectId(r.getProjectId());
        e.setBaseId(r.getBaseId());
        e.setSpeciesId(r.getSpeciesId());
        e.setFileId(r.getFileId());
        e.setIsRequired(r.getIsRequired());
        e.setCompletionWeight(r.getCompletionWeight());
        e.setSortOrder(r.getSortOrder());
        e.setUpdatedAt(LocalDateTime.now());
        if (itemMapper.updateById(e) == 0) {
            throw new BusinessException("Training item update conflict");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrainingPlanItemEntity e = itemMapper.selectById(id);
        if (e == null) {
            throw new ResourceNotFoundException("Training item not found");
        }
        manage(requirePlan(e.getPlanId()));
        if (itemMapper.deleteById(id) == 0) {
            throw new BusinessException("Training item delete failed");
        }
    }

    private TrainingPlanItemEntity toEntity(Long planId, TrainingPlanItemRequest r) {
        TrainingPlanItemEntity e = new TrainingPlanItemEntity();
        e.setPlanId(planId);
        e.setItemType(r.getItemType());
        e.setItemTitle(r.getItemTitle());
        e.setDescription(r.getDescription());
        e.setCourseId(r.getCourseId());
        e.setProjectId(r.getProjectId());
        e.setBaseId(r.getBaseId());
        e.setSpeciesId(r.getSpeciesId());
        e.setFileId(r.getFileId());
        e.setIsRequired(r.getIsRequired());
        e.setCompletionWeight(r.getCompletionWeight());
        e.setSortOrder(r.getSortOrder());
        e.setStatus(1);
        e.setIsDeleted(0);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        e.setCreatedBy(CurrentUserUtils.currentUserId());
        e.setUpdatedBy(CurrentUserUtils.currentUserId());
        e.setVersion(0);
        return e;
    }

    private TrainingPlanEntity requirePlan(Long id) {
        TrainingPlanEntity p = planMapper.selectById(id);
        if (p == null || Objects.equals(p.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Training plan not found");
        }
        return p;
    }

    private void manage(TrainingPlanEntity p) {
        Long u = CurrentUserUtils.currentUserId();
        if (!Objects.equals(u, p.getOwnerId())
                && !Objects.equals(u, p.getTrainerId())
                && !CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(x -> "ADMIN".equalsIgnoreCase(x))) {
            throw new ForbiddenException("Only training manager can manage items");
        }
    }
}
