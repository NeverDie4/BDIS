package com.bdis.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.permission.entity.DataScopeEntity;
import com.bdis.modules.permission.entity.PermissionEntity;
import com.bdis.modules.permission.entity.RolePermissionEntity;
import com.bdis.modules.permission.mapper.DataScopeMapper;
import com.bdis.modules.permission.mapper.PermissionMapper;
import com.bdis.modules.permission.mapper.RolePermissionMapper;
import com.bdis.modules.user.dto.RoleCreateDTO;
import com.bdis.modules.user.dto.RolePermissionAssignDTO;
import com.bdis.modules.user.dto.RoleUpdateDTO;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.entity.UserRoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import com.bdis.modules.user.mapper.UserRoleMapper;
import com.bdis.modules.user.query.RoleQuery;
import com.bdis.modules.user.service.RoleService;
import com.bdis.modules.user.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final DataScopeMapper dataScopeMapper;

    public RoleServiceImpl(
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PermissionMapper permissionMapper,
            RolePermissionMapper rolePermissionMapper,
            DataScopeMapper dataScopeMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.dataScopeMapper = dataScopeMapper;
    }

    @Override
    public PageResult<RoleVO> page(RoleQuery query) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    condition ->
                            condition
                                    .like(RoleEntity::getRoleCode, query.getKeyword())
                                    .or()
                                    .like(RoleEntity::getRoleName, query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(RoleEntity::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(RoleEntity::getSortOrder).orderByDesc(RoleEntity::getId);
        Page<RoleEntity> page =
                roleMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(), page);
    }

    @Override
    public RoleVO detail(Long id) {
        return toVO(requireRole(id));
    }

    @Override
    public Long create(RoleCreateDTO dto) {
        ensureCodeAvailable(dto.getRoleCode(), null);
        RoleEntity role = new RoleEntity();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setRoleType(dto.getRoleType());
        role.setDataScope(StringUtils.hasText(dto.getDataScope()) ? dto.getDataScope() : "self");
        role.setDescription(dto.getDescription());
        role.setSortOrder(dto.getSortOrder());
        role.setStatus(1);
        role.setCreatedBy(SecurityUtils.currentUser().getUserId());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void update(Long id, RoleUpdateDTO dto) {
        RoleEntity role = requireRole(id);
        if (dto.getRoleName() != null) {
            role.setRoleName(dto.getRoleName());
        }
        if (dto.getRoleType() != null) {
            role.setRoleType(dto.getRoleType());
        }
        if (dto.getDataScope() != null) {
            role.setDataScope(dto.getDataScope());
        }
        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }
        if (dto.getSortOrder() != null) {
            role.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        role.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireRole(id);
        Long usedCount =
                userRoleMapper.selectCount(
                        new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getRoleId, id));
        if (usedCount > 0) {
            throw new DuplicateResourceException("角色已被用户使用，不能删除");
        }
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermissionEntity>()
                        .eq(RolePermissionEntity::getRoleId, id));
        dataScopeMapper.delete(
                new LambdaQueryWrapper<DataScopeEntity>().eq(DataScopeEntity::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long id, RolePermissionAssignDTO dto) {
        requireRole(id);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermissionEntity>()
                        .eq(RolePermissionEntity::getRoleId, id));
        if (CollectionUtils.isEmpty(dto.getPermissionIds())) {
            return;
        }
        Long operatorId = SecurityUtils.currentUser().getUserId();
        for (Long permissionId : dto.getPermissionIds()) {
            PermissionEntity permission = permissionMapper.selectById(permissionId);
            if (permission == null
                    || permission.getStatus() == null
                    || permission.getStatus() != 1) {
                throw new ResourceNotFoundException("权限不存在或已停用：" + permissionId);
            }
            RolePermissionEntity relation = new RolePermissionEntity();
            relation.setRoleId(id);
            relation.setPermissionId(permissionId);
            relation.setCreatedBy(operatorId);
            rolePermissionMapper.insert(relation);
        }
    }

    private RoleEntity requireRole(Long id) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new ResourceNotFoundException("角色不存在");
        }
        return role;
    }

    private void ensureCodeAvailable(String code, Long excludeId) {
        RoleEntity existed =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getRoleCode, code));
        if (existed != null && (excludeId == null || !existed.getId().equals(excludeId))) {
            throw new DuplicateResourceException("角色编码已存在");
        }
    }

    private RoleVO toVO(RoleEntity role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleType(role.getRoleType());
        vo.setDataScope(role.getDataScope());
        vo.setDescription(role.getDescription());
        vo.setSortOrder(role.getSortOrder());
        vo.setStatus(role.getStatus());
        return vo;
    }
}
