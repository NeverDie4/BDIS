package com.bdis.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.exception.UnauthorizedException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.auth.service.CurrentUserService;
import com.bdis.modules.permission.entity.PermissionEntity;
import com.bdis.modules.permission.entity.RolePermissionEntity;
import com.bdis.modules.permission.mapper.PermissionMapper;
import com.bdis.modules.permission.mapper.RolePermissionMapper;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.entity.UserRoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import com.bdis.modules.user.mapper.UserMapper;
import com.bdis.modules.user.mapper.UserRoleMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final RoleMapper roleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final PermissionMapper permissionMapper;

    public CurrentUserServiceImpl(
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            RoleMapper roleMapper,
            RolePermissionMapper rolePermissionMapper,
            PermissionMapper permissionMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public CurrentUser load(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("账号不存在或已停用");
        }
        List<UserRoleEntity> userRoles =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRoleEntity>()
                                .eq(UserRoleEntity::getUserId, userId));
        Set<Long> roleIds = new LinkedHashSet<>();
        for (UserRoleEntity userRole : userRoles) {
            roleIds.add(userRole.getRoleId());
        }
        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();
        if (!roleIds.isEmpty()) {
            List<RoleEntity> roles = roleMapper.selectBatchIds(roleIds);
            for (RoleEntity role : roles) {
                if (role.getStatus() != null && role.getStatus() == 1) {
                    roleCodes.add(role.getRoleCode());
                }
            }
            permissions.addAll(loadPermissions(roleIds));
        }
        if (roleCodes.contains(SecurityConstants.ADMIN_ROLE_CODE)) {
            permissions.add("*");
            permissions.addAll(loadAllPermissions());
        }
        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getOrganizationId(),
                user.getDepartmentId(),
                roleCodes,
                roleIds,
                permissions);
    }

    private Set<String> loadPermissions(Set<Long> roleIds) {
        Set<String> permissionCodes = new LinkedHashSet<>();
        List<RolePermissionEntity> rolePermissions =
                rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermissionEntity>()
                                .in(RolePermissionEntity::getRoleId, roleIds));
        Set<Long> permissionIds = new LinkedHashSet<>();
        for (RolePermissionEntity rolePermission : rolePermissions) {
            permissionIds.add(rolePermission.getPermissionId());
        }
        if (permissionIds.isEmpty()) {
            return permissionCodes;
        }
        List<PermissionEntity> permissions = permissionMapper.selectBatchIds(permissionIds);
        for (PermissionEntity permission : permissions) {
            if (permission.getStatus() != null && permission.getStatus() == 1) {
                permissionCodes.add(permission.getPermissionCode());
            }
        }
        return permissionCodes;
    }

    private Set<String> loadAllPermissions() {
        Set<String> permissionCodes = new LinkedHashSet<>();
        List<PermissionEntity> permissions =
                permissionMapper.selectList(
                        new LambdaQueryWrapper<PermissionEntity>()
                                .eq(PermissionEntity::getStatus, 1));
        for (PermissionEntity permission : permissions) {
            permissionCodes.add(permission.getPermissionCode());
        }
        return permissionCodes;
    }
}
