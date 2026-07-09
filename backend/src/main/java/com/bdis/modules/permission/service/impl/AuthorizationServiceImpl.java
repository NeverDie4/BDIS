package com.bdis.modules.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.permission.dto.AuthorizationDecisionDTO;
import com.bdis.modules.permission.entity.MenuEntity;
import com.bdis.modules.permission.entity.PermissionEntity;
import com.bdis.modules.permission.mapper.MenuMapper;
import com.bdis.modules.permission.mapper.PermissionMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.AuthorizationDecisionVO;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.permission.vo.MenuVO;
import com.bdis.modules.permission.vo.PermissionVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private final PermissionMapper permissionMapper;

    private final MenuMapper menuMapper;

    private final DataScopeService dataScopeService;

    public AuthorizationServiceImpl(
            PermissionMapper permissionMapper, MenuMapper menuMapper, DataScopeService dataScopeService) {
        this.permissionMapper = permissionMapper;
        this.menuMapper = menuMapper;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public boolean hasPermission(String permissionCode) {
        CurrentUser user = SecurityUtils.currentUser();
        return user.getRoleCodes().contains(SecurityConstants.ADMIN_ROLE_CODE)
                || user.getPermissions().contains("*")
                || user.getPermissions().contains(permissionCode);
    }

    @Override
    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new ForbiddenException("缺少权限：" + permissionCode);
        }
    }

    @Override
    public AuthorizationDecisionVO decide(AuthorizationDecisionDTO dto) {
        AuthorizationDecisionVO vo = new AuthorizationDecisionVO();
        String permissionCode =
                StringUtils.hasText(dto.getPermissionCode())
                        ? dto.getPermissionCode()
                        : dto.getResourceType() + ":" + dto.getAction();
        boolean allowed = hasPermission(permissionCode);
        vo.setAllowed(allowed);
        vo.setReason(allowed ? "允许访问" : "缺少权限：" + permissionCode);
        vo.setDataScope(dataScopeService.resolveForCurrentUser(dto.getResourceType()));
        return vo;
    }

    @Override
    public List<MenuVO> currentMenus() {
        CurrentUser user = SecurityUtils.currentUser();
        Set<String> permissions = user.getPermissions();
        boolean admin =
                user.getRoleCodes().contains(SecurityConstants.ADMIN_ROLE_CODE)
                        || permissions.contains("*");
        List<PermissionEntity> menuPermissions =
                permissionMapper.selectList(
                        new LambdaQueryWrapper<PermissionEntity>()
                                .eq(PermissionEntity::getPermissionType, "menu")
                                .eq(PermissionEntity::getStatus, 1));
        List<Long> menuIds = new ArrayList<>();
        for (PermissionEntity permission : menuPermissions) {
            if (permission.getMenuId() != null
                    && (admin || permissions.contains(permission.getPermissionCode()))) {
                menuIds.add(permission.getMenuId());
            }
        }
        if (menuIds.isEmpty() && !admin) {
            return List.of();
        }
        List<MenuEntity> menus =
                admin
                        ? menuMapper.selectList(
                                new LambdaQueryWrapper<MenuEntity>()
                                        .eq(MenuEntity::getStatus, 1)
                                        .eq(MenuEntity::getVisible, 1)
                                        .orderByAsc(MenuEntity::getSortOrder)
                                        .orderByAsc(MenuEntity::getId))
                        : menuMapper.selectList(
                                new LambdaQueryWrapper<MenuEntity>()
                                        .in(MenuEntity::getId, menuIds)
                                        .eq(MenuEntity::getStatus, 1)
                                        .eq(MenuEntity::getVisible, 1)
                                        .orderByAsc(MenuEntity::getSortOrder)
                                        .orderByAsc(MenuEntity::getId));
        return buildTree(menus);
    }

    @Override
    public List<PermissionVO> currentButtons(String menuCode) {
        MenuEntity menu =
                StringUtils.hasText(menuCode)
                        ? menuMapper.selectOne(
                                new LambdaQueryWrapper<MenuEntity>()
                                        .eq(MenuEntity::getMenuCode, menuCode))
                        : null;
        LambdaQueryWrapper<PermissionEntity> wrapper =
                new LambdaQueryWrapper<PermissionEntity>()
                        .eq(PermissionEntity::getPermissionType, "button")
                        .eq(PermissionEntity::getStatus, 1);
        if (menu != null) {
            wrapper.eq(PermissionEntity::getMenuId, menu.getId());
        }
        List<PermissionEntity> permissions = permissionMapper.selectList(wrapper);
        CurrentUser user = SecurityUtils.currentUser();
        boolean admin =
                user.getRoleCodes().contains(SecurityConstants.ADMIN_ROLE_CODE)
                        || user.getPermissions().contains("*");
        return permissions.stream()
                .filter(permission -> admin || user.getPermissions().contains(permission.getPermissionCode()))
                .map(this::toPermissionVO)
                .toList();
    }

    private List<MenuVO> buildTree(List<MenuEntity> menus) {
        Map<Long, MenuVO> byId = new LinkedHashMap<>();
        for (MenuEntity menu : menus) {
            byId.put(menu.getId(), toMenuVO(menu));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO menu : byId.values()) {
            if (menu.getParentId() == null || menu.getParentId() == 0 || !byId.containsKey(menu.getParentId())) {
                roots.add(menu);
            } else {
                byId.get(menu.getParentId()).getChildren().add(menu);
            }
        }
        return roots;
    }

    private MenuVO toMenuVO(MenuEntity entity) {
        MenuVO vo = new MenuVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setMenuCode(entity.getMenuCode());
        vo.setMenuName(entity.getMenuName());
        vo.setRoutePath(entity.getRoutePath());
        vo.setComponentPath(entity.getComponentPath());
        vo.setIcon(entity.getIcon());
        vo.setVisible(entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private PermissionVO toPermissionVO(PermissionEntity entity) {
        PermissionVO vo = new PermissionVO();
        vo.setId(entity.getId());
        vo.setPermissionCode(entity.getPermissionCode());
        vo.setPermissionName(entity.getPermissionName());
        vo.setPermissionType(entity.getPermissionType());
        vo.setMenuId(entity.getMenuId());
        vo.setApiPath(entity.getApiPath());
        vo.setRequestMethod(entity.getRequestMethod());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
