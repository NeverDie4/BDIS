package com.bdis.modules.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.permission.dto.PermissionDTO;
import com.bdis.modules.permission.entity.PermissionEntity;
import com.bdis.modules.permission.entity.RolePermissionEntity;
import com.bdis.modules.permission.mapper.PermissionMapper;
import com.bdis.modules.permission.mapper.RolePermissionMapper;
import com.bdis.modules.permission.query.PermissionQuery;
import com.bdis.modules.permission.service.PermissionService;
import com.bdis.modules.permission.vo.PermissionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    public PermissionServiceImpl(
            PermissionMapper permissionMapper, RolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public PageResult<PermissionVO> page(PermissionQuery query) {
        LambdaQueryWrapper<PermissionEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    condition ->
                            condition
                                    .like(PermissionEntity::getPermissionCode, query.getKeyword())
                                    .or()
                                    .like(PermissionEntity::getPermissionName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getPermissionType())) {
            wrapper.eq(PermissionEntity::getPermissionType, query.getPermissionType());
        }
        if (query.getMenuId() != null) {
            wrapper.eq(PermissionEntity::getMenuId, query.getMenuId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(PermissionEntity::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(PermissionEntity::getId);
        Page<PermissionEntity> page =
                permissionMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(), page);
    }

    @Override
    public PermissionVO detail(Long id) {
        return toVO(requirePermission(id));
    }

    @Override
    public Long create(PermissionDTO dto) {
        ensureCodeAvailable(dto.getPermissionCode());
        PermissionEntity entity = new PermissionEntity();
        apply(entity, dto);
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        permissionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, PermissionDTO dto) {
        PermissionEntity entity = requirePermission(id);
        apply(entity, dto);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        permissionMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requirePermission(id);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermissionEntity>()
                        .eq(RolePermissionEntity::getPermissionId, id));
        permissionMapper.deleteById(id);
    }

    private void apply(PermissionEntity entity, PermissionDTO dto) {
        entity.setPermissionCode(dto.getPermissionCode());
        entity.setPermissionName(dto.getPermissionName());
        entity.setPermissionType(dto.getPermissionType());
        entity.setMenuId(dto.getMenuId());
        entity.setApiPath(dto.getApiPath());
        entity.setRequestMethod(dto.getRequestMethod());
        entity.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private PermissionEntity requirePermission(Long id) {
        PermissionEntity entity = permissionMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("权限不存在");
        }
        return entity;
    }

    private void ensureCodeAvailable(String code) {
        PermissionEntity existed =
                permissionMapper.selectOne(
                        new LambdaQueryWrapper<PermissionEntity>()
                                .eq(PermissionEntity::getPermissionCode, code));
        if (existed != null) {
            throw new DuplicateResourceException("权限编码已存在");
        }
    }

    private PermissionVO toVO(PermissionEntity entity) {
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
