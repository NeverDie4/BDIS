package com.bdis.modules.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceParticipantRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceParticipantMapper;
import com.bdis.modules.performance.service.PerformanceParticipantService;
import com.bdis.modules.performance.vo.PerformanceParticipantUserVO;
import com.bdis.modules.performance.vo.PerformanceParticipantVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PerformanceParticipantServiceImpl implements PerformanceParticipantService {

    private static final long DEFAULT_CANDIDATE_PAGE_SIZE = 20L;
    private static final long MAX_CANDIDATE_PAGE_SIZE = 50L;

    private final PerformanceMapper performanceMapper;
    private final PerformanceParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final BusinessAccessService accessService;

    @Override
    public List<PerformanceParticipantVO> listParticipants(Long performanceId) {
        PerformanceEntity performance =
                requirePerformance(performanceId, "performance:record:view");
        List<PerformanceParticipantEntity> participants =
                participantMapper.selectList(
                        new LambdaQueryWrapper<PerformanceParticipantEntity>()
                                .eq(
                                        PerformanceParticipantEntity::getPerformanceId,
                                        performance.getId())
                                .orderByDesc(PerformanceParticipantEntity::getIsPrimary)
                                .orderByAsc(PerformanceParticipantEntity::getSortOrder)
                                .orderByAsc(PerformanceParticipantEntity::getId));
        Map<Long, UserEntity> usersById =
                participants.isEmpty()
                        ? Map.of()
                        : userMapper
                                .selectBatchIds(
                                        participants.stream()
                                                .map(PerformanceParticipantEntity::getUserId)
                                                .distinct()
                                                .toList())
                                .stream()
                                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return participants.stream()
                .map(
                        participant ->
                                PerformanceParticipantVO.from(
                                        participant, usersById.get(participant.getUserId())))
                .toList();
    }

    @Override
    public IPage<PerformanceParticipantUserVO> listParticipantUsers(
            Long performanceId, String keyword, Long pageNum, Long pageSize) {
        PerformanceEntity performance = requireEditablePerformance(performanceId);
        UserEntity owner = requireParticipantScopeOwner(performance);
        LambdaQueryWrapper<UserEntity> wrapper =
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getStatus, 1)
                        .orderByAsc(UserEntity::getUsername);
        applyParticipantBusinessScope(wrapper, owner);
        wrapper.and(
                StringUtils.hasText(keyword),
                query ->
                        query.like(UserEntity::getRealName, keyword)
                                .or()
                                .like(UserEntity::getUsername, keyword));
        IPage<UserEntity> users =
                userMapper.selectPage(
                        new Page<>(safePageNum(pageNum), safePageSize(pageSize)), wrapper);
        Page<PerformanceParticipantUserVO> candidates =
                new Page<>(users.getCurrent(), users.getSize(), users.getTotal());
        candidates.setRecords(
                users.getRecords().stream().map(PerformanceParticipantUserVO::from).toList());
        return candidates;
    }

    @Override
    @Transactional
    public PerformanceParticipantEntity addParticipant(
            Long performanceId, PerformanceParticipantRequest request) {
        PerformanceEntity performance = requireEditablePerformance(performanceId);
        requireActiveParticipant(performance, request.getUserId());
        if (existsParticipant(performanceId, request.getUserId())) {
            throw new IllegalArgumentException("参与人已存在");
        }
        if (Boolean.TRUE.equals(request.getPrimary())
                && !performance.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("负责人必须与业绩归属人一致");
        }
        boolean primary = Boolean.TRUE.equals(request.getPrimary());
        validateParticipantRole(request.getParticipantRole(), primary);
        PerformanceParticipantEntity entity = new PerformanceParticipantEntity();
        entity.setPerformanceId(performanceId);
        entity.setUserId(request.getUserId());
        entity.setParticipantRole(request.getParticipantRole().trim());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setIsPrimary(primary ? 1 : 0);
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
        boolean primary = Integer.valueOf(1).equals(entity.getIsPrimary());
        validateParticipantRole(request.getParticipantRole(), primary);
        requireActiveParticipant(performance, request.getUserId());
        if (!entity.getUserId().equals(request.getUserId())
                && existsParticipant(performanceId, request.getUserId())) {
            throw new IllegalArgumentException("参与人已存在");
        }
        entity.setUserId(request.getUserId());
        entity.setParticipantRole(request.getParticipantRole().trim());
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

    private void requireActiveParticipant(PerformanceEntity performance, Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("参与人不存在或已停用");
        }
        if (!isInParticipantBusinessScope(requireParticipantScopeOwner(performance), user)) {
            throw new ForbiddenException("参与人必须与业绩负责人属于同一部门或机构");
        }
    }

    private UserEntity requireParticipantScopeOwner(PerformanceEntity performance) {
        UserEntity owner = userMapper.selectById(performance.getUserId());
        if (owner == null || !Integer.valueOf(1).equals(owner.getStatus())) {
            throw new IllegalArgumentException("业绩负责人不存在或已停用，不能维护参与人");
        }
        return owner;
    }

    private void applyParticipantBusinessScope(
            LambdaQueryWrapper<UserEntity> wrapper, UserEntity owner) {
        if (owner.getOrganizationId() != null) {
            wrapper.eq(UserEntity::getOrganizationId, owner.getOrganizationId());
        }
        if (owner.getDepartmentId() != null) {
            wrapper.eq(UserEntity::getDepartmentId, owner.getDepartmentId());
        }
        if (owner.getOrganizationId() == null && owner.getDepartmentId() == null) {
            wrapper.eq(UserEntity::getId, owner.getId());
        }
    }

    private boolean isInParticipantBusinessScope(UserEntity owner, UserEntity participant) {
        if (owner.getDepartmentId() != null
                && !owner.getDepartmentId().equals(participant.getDepartmentId())) {
            return false;
        }
        if (owner.getOrganizationId() != null
                && !owner.getOrganizationId().equals(participant.getOrganizationId())) {
            return false;
        }
        return owner.getDepartmentId() != null
                || owner.getOrganizationId() != null
                || owner.getId().equals(participant.getId());
    }

    private long safePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long safePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_CANDIDATE_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_CANDIDATE_PAGE_SIZE);
    }

    private void validateParticipantRole(String participantRole, boolean primary) {
        String role = participantRole == null ? "" : participantRole.trim();
        if (primary && !"owner".equals(role)) {
            throw new IllegalArgumentException("负责人参与记录的角色必须为 owner");
        }
        if (!primary && "owner".equals(role)) {
            throw new IllegalArgumentException("owner 是负责人保留角色，普通参与人不能使用");
        }
    }

    private void updateOrThrow(PerformanceParticipantEntity entity) {
        if (participantMapper.updateById(entity) != 1) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, "参与人已被其他请求修改，请刷新后重试");
        }
    }
}
