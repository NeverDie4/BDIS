package com.bdis.modules.collection.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.entity.UserRoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import com.bdis.modules.user.mapper.UserMapper;
import com.bdis.modules.user.mapper.UserRoleMapper;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CollectionAccessService {

    private static final String RESOURCE_TYPE = "herb_growth_record";

    private final DataScopeService dataScopeService;
    private final UserMapper userMapper;
    private final HerbCollectionTaskMapper herbCollectionTaskMapper;
    private RoleMapper roleMapper;
    private UserRoleMapper userRoleMapper;

    public CollectionAccessService(
            DataScopeService dataScopeService,
            UserMapper userMapper,
            HerbCollectionTaskMapper herbCollectionTaskMapper) {
        this.dataScopeService = dataScopeService;
        this.userMapper = userMapper;
        this.herbCollectionTaskMapper = herbCollectionTaskMapper;
    }

    @Autowired
    void setRoleAssignmentMappers(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public List<AssignableCollector> listAssignableCollectors() {
        RoleEntity collectorRole =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<RoleEntity>()
                                .eq(RoleEntity::getRoleCode, "COLLECTOR")
                                .eq(RoleEntity::getStatus, 1));
        if (collectorRole == null) {
            return List.of();
        }
        Set<Long> collectorIds = new LinkedHashSet<>();
        userRoleMapper
                .selectList(
                        new LambdaQueryWrapper<UserRoleEntity>()
                                .eq(UserRoleEntity::getRoleId, collectorRole.getId()))
                .stream()
                .map(UserRoleEntity::getUserId)
                .forEach(collectorIds::add);
        CollectionAccessScope scope = currentScope();
        if (!scope.isAllIncluded()) {
            collectorIds.retainAll(scope.getOwnerIds());
        }
        if (collectorIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(collectorIds).stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .sorted(Comparator.comparing(this::collectorDisplayName))
                .map(user -> new AssignableCollector(user.getId(), collectorDisplayName(user)))
                .toList();
    }

    private String collectorDisplayName(UserEntity user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    public record AssignableCollector(Long id, String name) {}

    public CollectionAccessScope currentScope() {
        CurrentUser current = SecurityUtils.currentUser();
        DataScopeResultVO scope = dataScopeService.resolveForCurrentUser(RESOURCE_TYPE);
        Set<Long> ownerIds = new LinkedHashSet<>();
        ownerIds.add(current.getUserId());
        if (!scope.isAllIncluded()
                && (!scope.getOrganizationIds().isEmpty() || !scope.getDepartmentIds().isEmpty())) {
            LambdaQueryWrapper<UserEntity> wrapper =
                    new LambdaQueryWrapper<UserEntity>()
                            .eq(UserEntity::getStatus, 1)
                            .and(
                                    condition -> {
                                        boolean applied = false;
                                        if (!scope.getOrganizationIds().isEmpty()) {
                                            condition.in(
                                                    UserEntity::getOrganizationId,
                                                    scope.getOrganizationIds());
                                            applied = true;
                                        }
                                        if (!scope.getDepartmentIds().isEmpty()) {
                                            if (applied) {
                                                condition.or();
                                            }
                                            condition.in(
                                                    UserEntity::getDepartmentId,
                                                    scope.getDepartmentIds());
                                        }
                                    });
            userMapper.selectList(wrapper).stream()
                    .map(UserEntity::getId)
                    .filter(id -> id != null)
                    .forEach(ownerIds::add);
        }
        return new CollectionAccessScope(scope.isAllIncluded(), List.copyOf(ownerIds));
    }

    public Long currentUserId() {
        return SecurityUtils.currentUser().getUserId();
    }

    public String currentUserDisplayName() {
        CurrentUser current = SecurityUtils.currentUser();
        return StringUtils.hasText(current.getRealName())
                ? current.getRealName()
                : current.getUsername();
    }

    public String requireAssignableCollector(Long collectorId) {
        if (collectorId == null) {
            return null;
        }
        UserEntity collector = userMapper.selectById(collectorId);
        if (collector == null || collector.getStatus() == null || collector.getStatus() != 1) {
            throw new ResourceNotFoundException("采集人不存在或已停用");
        }
        RoleEntity collectorRole =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<RoleEntity>()
                                .eq(RoleEntity::getRoleCode, "COLLECTOR")
                                .eq(RoleEntity::getStatus, 1));
        if (collectorRole == null
                || userRoleMapper.selectCount(
                                new LambdaQueryWrapper<UserRoleEntity>()
                                        .eq(UserRoleEntity::getUserId, collectorId)
                                        .eq(UserRoleEntity::getRoleId, collectorRole.getId()))
                        == 0) {
            throw new ForbiddenException("只能向采集员角色用户分配采集任务");
        }
        CollectionAccessScope scope = currentScope();
        if (!scope.isAllIncluded() && !scope.getOwnerIds().contains(collectorId)) {
            throw new ForbiddenException("不能向数据范围外的用户分配采集任务");
        }
        return collectorDisplayName(collector);
    }

    public void requireTaskAccess(HerbCollectionTaskEntity task) {
        requireScopedAccess(taskOwnerIds(task), "采集任务超出当前数据范围");
    }

    public void requireTaskManage(HerbCollectionTaskEntity task) {
        CurrentUser current = SecurityUtils.currentUser();
        Long managerId = task.getCreatedBy() == null ? task.getCollectorId() : task.getCreatedBy();
        if (current.getUserId().equals(managerId) || currentScope().isAllIncluded()) {
            return;
        }
        throw new ForbiddenException("只能管理本人创建的采集任务");
    }

    public void requireTaskExecution(HerbCollectionTaskEntity task) {
        CurrentUser current = SecurityUtils.currentUser();
        if (current.getUserId().equals(task.getCollectorId())
                || current.getUserId().equals(task.getCreatedBy())
                || currentScope().isAllIncluded()) {
            return;
        }
        throw new ForbiddenException("只能执行分配给本人或本人创建的采集任务");
    }

    public void requireBatchAccess(HerbBatchEntity batch) {
        requireScopedAccess(batchOwnerIds(batch), "采集批次超出当前数据范围");
    }

    public void requireBatchOwner(HerbBatchEntity batch) {
        CurrentUser current = SecurityUtils.currentUser();
        if (batchOwnerIds(batch).contains(current.getUserId()) || currentScope().isAllIncluded()) {
            return;
        }
        throw new ForbiddenException("只能操作本人负责的采集批次");
    }

    public void requireBatchReview(HerbBatchEntity batch) {
        requireBatchAccess(batch);
    }

    private void requireScopedAccess(List<Long> owners, String message) {
        CollectionAccessScope scope = currentScope();
        if (scope.isAllIncluded() || owners.stream().anyMatch(scope.getOwnerIds()::contains)) {
            return;
        }
        throw new ForbiddenException(message);
    }

    private List<Long> taskOwnerIds(HerbCollectionTaskEntity task) {
        Set<Long> owners = new LinkedHashSet<>();
        if (task.getCreatedBy() != null) {
            owners.add(task.getCreatedBy());
        }
        if (task.getCollectorId() != null) {
            owners.add(task.getCollectorId());
        }
        return List.copyOf(owners);
    }

    private List<Long> batchOwnerIds(HerbBatchEntity batch) {
        Set<Long> owners = new LinkedHashSet<>();
        if (batch.getCreatedBy() != null) {
            owners.add(batch.getCreatedBy());
        }
        if (batch.getTaskId() != null) {
            HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(batch.getTaskId());
            if (task != null) {
                owners.addAll(taskOwnerIds(task));
            }
        }
        return List.copyOf(owners);
    }
}
