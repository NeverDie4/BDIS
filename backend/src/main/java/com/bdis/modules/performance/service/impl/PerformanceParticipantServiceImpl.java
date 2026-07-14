package com.bdis.modules.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceParticipantRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceParticipantMapper;
import com.bdis.modules.performance.service.PerformanceParticipantService;
import com.bdis.modules.performance.vo.PerformanceParticipantUserVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceParticipantServiceImpl implements PerformanceParticipantService {

    private final PerformanceMapper performanceMapper;
    private final PerformanceParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final BusinessAccessService accessService;

    @Override
    public List<PerformanceParticipantEntity> listParticipants(Long performanceId) {
        PerformanceEntity performance =
                requirePerformance(performanceId, "performance:record:view");
        return participantMapper.selectList(
                new LambdaQueryWrapper<PerformanceParticipantEntity>()
                        .eq(PerformanceParticipantEntity::getPerformanceId, performance.getId())
                        .orderByDesc(PerformanceParticipantEntity::getIsPrimary)
                        .orderByAsc(PerformanceParticipantEntity::getSortOrder)
                        .orderByAsc(PerformanceParticipantEntity::getId));
    }

    @Override
    public List<PerformanceParticipantUserVO> listParticipantUsers(Long performanceId) {
        requireEditablePerformance(performanceId);
        return userMapper
                .selectList(
                        new LambdaQueryWrapper<UserEntity>()
                                .eq(UserEntity::getStatus, 1)
                                .orderByAsc(UserEntity::getUsername))
                .stream()
                .map(PerformanceParticipantUserVO::from)
                .toList();
    }

    @Override
    @Transactional
    public PerformanceParticipantEntity addParticipant(
            Long performanceId, PerformanceParticipantRequest request) {
        PerformanceEntity performance = requireEditablePerformance(performanceId);
        requireActiveUser(request.getUserId());
        if (existsParticipant(performanceId, request.getUserId())) {
            throw new IllegalArgumentException("参与人已存在");
        }
        if (Boolean.TRUE.equals(request.getPrimary())
                && !performance.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("负责人必须与业绩归属人一致");
        }
        PerformanceParticipantEntity entity = new PerformanceParticipantEntity();
        entity.setPerformanceId(performanceId);
        entity.setUserId(request.getUserId());
        entity.setParticipantRole(request.getParticipantRole());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setIsPrimary(Boolean.TRUE.equals(request.getPrimary()) ? 1 : 0);
        entity.setStatus(1);
        entity.setCreatedBy(accessService.currentUserId());
        entity.setRemark(request.getRemark());
        try {
            participantMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, "参与人已存在，请刷新后重试");
        }
        return participantMapper.selectById(entity.getId());
    }

    @Override
    @Transactional
    public PerformanceParticipantEntity updateParticipant(
            Long performanceId, Long participantId, PerformanceParticipantRequest request) {
        PerformanceEntity performance = requireEditablePerformance(performanceId);
        PerformanceParticipantEntity entity = requireParticipant(performanceId, participantId);
        if (Integer.valueOf(1).equals(entity.getIsPrimary())) {
            if (!performance.getUserId().equals(request.getUserId())
                    || !Boolean.TRUE.equals(request.getPrimary())) {
                throw new IllegalArgumentException("负责人参与记录不能变更归属人或取消负责人标识");
            }
        } else if (Boolean.TRUE.equals(request.getPrimary())) {
            throw new IllegalArgumentException("负责人已由业绩归属人固定维护");
        }
        requireActiveUser(request.getUserId());
        if (!entity.getUserId().equals(request.getUserId())
                && existsParticipant(performanceId, request.getUserId())) {
            throw new IllegalArgumentException("参与人已存在");
        }
        entity.setUserId(request.getUserId());
        entity.setParticipantRole(request.getParticipantRole());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setUpdatedBy(accessService.currentUserId());
        entity.setRemark(request.getRemark());
        updateOrThrow(entity);
        return participantMapper.selectById(participantId);
    }

    @Override
    @Transactional
    public void removeParticipant(Long performanceId, Long participantId) {
        requireEditablePerformance(performanceId);
        PerformanceParticipantEntity entity = requireParticipant(performanceId, participantId);
        if (Integer.valueOf(1).equals(entity.getIsPrimary())) {
            throw new IllegalArgumentException("不能删除业绩负责人参与记录");
        }
        entity.setDeletedBy(accessService.currentUserId());
        entity.setDeletedAt(LocalDateTime.now());
        updateOrThrow(entity);
        if (participantMapper.deleteById(participantId) != 1) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, "参与人已被其他请求修改，请刷新后重试");
        }
    }

    private PerformanceEntity requireEditablePerformance(Long performanceId) {
        PerformanceEntity performance =
                requirePerformance(performanceId, "performance:record:update");
        if (!"draft".equals(performance.getIdentifyStatus())
                && !"rejected".equals(performance.getIdentifyStatus())) {
            throw new IllegalArgumentException("只有草稿或退回状态的业绩可以维护参与人");
        }
        return performance;
    }

    private PerformanceEntity requirePerformance(Long performanceId, String permission) {
        PerformanceEntity performance = performanceMapper.selectById(performanceId);
        if (performance == null) {
            throw new IllegalArgumentException("业绩记录不存在");
        }
        accessService.requireResourceAccess(
                "perf_record", performanceId, permission, performance.getUserId());
        return performance;
    }

    private PerformanceParticipantEntity requireParticipant(
            Long performanceId, Long participantId) {
        PerformanceParticipantEntity entity = participantMapper.selectById(participantId);
        if (entity == null || !performanceId.equals(entity.getPerformanceId())) {
            throw new IllegalArgumentException("参与人记录不存在");
        }
        return entity;
    }

    private boolean existsParticipant(Long performanceId, Long userId) {
        return participantMapper.selectCount(
                        new LambdaQueryWrapper<PerformanceParticipantEntity>()
                                .eq(PerformanceParticipantEntity::getPerformanceId, performanceId)
                                .eq(PerformanceParticipantEntity::getUserId, userId))
                > 0;
    }

    private void requireActiveUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("参与人不存在或已停用");
        }
    }

    private void updateOrThrow(PerformanceParticipantEntity entity) {
        if (participantMapper.updateById(entity) != 1) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, "参与人已被其他请求修改，请刷新后重试");
        }
    }
}
