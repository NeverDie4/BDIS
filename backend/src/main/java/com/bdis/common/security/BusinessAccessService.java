package com.bdis.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.modules.permission.dto.AuthorizationDecisionDTO;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.AuthorizationDecisionVO;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessAccessService {

    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;
    private final UserMapper userMapper;

    public Long currentUserId() {
        return SecurityUtils.currentUser().getUserId();
    }

    public void requirePermission(String permissionCode) {
        authorizationService.requirePermission(permissionCode);
    }

    public <T> void applyOwnerScope(
            LambdaQueryWrapper<T> wrapper, String resourceType, SFunction<T, ?> ownerField) {
        DataScopeResultVO scope = dataScopeService.resolveForCurrentUser(resourceType);
        if (scope.isAllIncluded()) {
            return;
        }
        Set<Long> ownerIds = new HashSet<>();
        if (scope.isSelfIncluded()) {
            ownerIds.add(currentUserId());
        }
        if (!scope.getOrganizationIds().isEmpty() || !scope.getDepartmentIds().isEmpty()) {
            ownerIds.addAll(
                    userMapper
                            .selectList(
                                    new LambdaQueryWrapper<UserEntity>()
                                            .in(
                                                    !scope.getOrganizationIds().isEmpty(),
                                                    UserEntity::getOrganizationId,
                                                    scope.getOrganizationIds())
                                            .or(
                                                    !scope.getDepartmentIds().isEmpty(),
                                                    condition ->
                                                            condition.in(
                                                                    UserEntity::getDepartmentId,
                                                                    scope.getDepartmentIds())))
                            .stream()
                            .map(UserEntity::getId)
                            .collect(java.util.stream.Collectors.toSet()));
        }
        if (ownerIds.isEmpty()) {
            wrapper.apply("1 = 0");
        } else {
            wrapper.in(ownerField, ownerIds);
        }
    }

    public void requireResourceAccess(
            String resourceType, Long resourceId, String permissionCode, Long ownerUserId) {
        AuthorizationDecisionDTO decisionRequest = new AuthorizationDecisionDTO();
        decisionRequest.setResourceType(resourceType);
        decisionRequest.setResourceId(resourceId);
        decisionRequest.setAction(permissionCode.substring(permissionCode.lastIndexOf(':') + 1));
        decisionRequest.setPermissionCode(permissionCode);
        decisionRequest.setOwnerUserId(ownerUserId);
        UserEntity owner = ownerUserId == null ? null : userMapper.selectById(ownerUserId);
        if (owner != null) {
            decisionRequest.setOrganizationId(owner.getOrganizationId());
            decisionRequest.setDepartmentId(owner.getDepartmentId());
        }
        AuthorizationDecisionVO decision = authorizationService.decide(decisionRequest);
        if (!decision.isAllowed()) {
            throw new ForbiddenException(decision.getReason());
        }
    }
}
