package com.bdis.modules.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.permission.dto.MenuDTO;
import com.bdis.modules.permission.entity.MenuEntity;
import com.bdis.modules.permission.entity.PermissionEntity;
import com.bdis.modules.permission.mapper.MenuMapper;
import com.bdis.modules.permission.mapper.PermissionMapper;
import com.bdis.modules.permission.query.MenuQuery;
import com.bdis.modules.permission.service.MenuService;
import com.bdis.modules.permission.vo.MenuVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    private final PermissionMapper permissionMapper;

    public MenuServiceImpl(MenuMapper menuMapper, PermissionMapper permissionMapper) {
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<MenuVO> tree(MenuQuery query) {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    condition ->
                            condition
                                    .like(MenuEntity::getMenuCode, query.getKeyword())
                                    .or()
                                    .like(MenuEntity::getMenuName, query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(MenuEntity::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(MenuEntity::getSortOrder).orderByAsc(MenuEntity::getId);
        return buildTree(menuMapper.selectList(wrapper));
    }

    @Override
    public MenuVO detail(Long id) {
        return toVO(requireMenu(id));
    }

    @Override
    public Long create(MenuDTO dto) {
        ensureCodeAvailable(dto.getMenuCode());
        MenuEntity entity = new MenuEntity();
        apply(entity, dto);
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        menuMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, MenuDTO dto) {
        MenuEntity entity = requireMenu(id);
        apply(entity, dto);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        menuMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        requireMenu(id);
        Long childCount =
                menuMapper.selectCount(
                        new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getParentId, id));
        if (childCount > 0) {
            throw new DuplicateResourceException("菜单存在子菜单，不能删除");
        }
        Long permissionCount =
                permissionMapper.selectCount(
                        new LambdaQueryWrapper<PermissionEntity>()
                                .eq(PermissionEntity::getMenuId, id));
        if (permissionCount > 0) {
            throw new DuplicateResourceException("菜单已绑定权限点，不能删除");
        }
        menuMapper.deleteById(id);
    }

    private List<MenuVO> buildTree(List<MenuEntity> entities) {
        Map<Long, MenuVO> byId = new LinkedHashMap<>();
        for (MenuEntity entity : entities) {
            byId.put(entity.getId(), toVO(entity));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO item : byId.values()) {
            if (item.getParentId() == null
                    || item.getParentId() == 0
                    || !byId.containsKey(item.getParentId())) {
                roots.add(item);
            } else {
                byId.get(item.getParentId()).getChildren().add(item);
            }
        }
        return roots;
    }

    private void apply(MenuEntity entity, MenuDTO dto) {
        entity.setParentId(dto.getParentId() == null ? 0 : dto.getParentId());
        entity.setMenuCode(dto.getMenuCode());
        entity.setMenuName(dto.getMenuName());
        entity.setRoutePath(dto.getRoutePath());
        entity.setComponentPath(dto.getComponentPath());
        entity.setIcon(dto.getIcon());
        entity.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
        entity.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private MenuEntity requireMenu(Long id) {
        MenuEntity entity = menuMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("菜单不存在");
        }
        return entity;
    }

    private void ensureCodeAvailable(String code) {
        MenuEntity existed =
                menuMapper.selectOne(
                        new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getMenuCode, code));
        if (existed != null) {
            throw new DuplicateResourceException("菜单编码已存在");
        }
    }

    private MenuVO toVO(MenuEntity entity) {
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
}
