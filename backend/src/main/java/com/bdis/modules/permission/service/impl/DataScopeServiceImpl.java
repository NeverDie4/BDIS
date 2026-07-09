package com.bdis.modules.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.permission.dto.DataScopeDTO;
import com.bdis.modules.permission.entity.DataScopeEntity;
import com.bdis.modules.permission.mapper.DataScopeMapper;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.permission.vo.DataScopeVO;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DataScopeServiceImpl implements DataScopeService {

    private static final String ALWAYS_FALSE_SQL = "1 = 0";

    private final DataScopeMapper dataScopeMapper;

    private final RoleMapper roleMapper;

    public DataScopeServiceImpl(DataScopeMapper dataScopeMapper, RoleMapper roleMapper) {
        this.dataScopeMapper = dataScopeMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public DataScopeResultVO resolveForCurrentUser(String resourceType) {
        CurrentUser user = SecurityUtils.currentUser();
        DataScopeResultVO result = new DataScopeResultVO();
        result.setScopeType("none");
        if (user.getRoleCodes().contains(SecurityConstants.ADMIN_ROLE_CODE)) {
            result.setAllIncluded(true);
            result.setScopeType("all");
            return result;
        }
        List<DataScopeEntity> scopedRules =
                dataScopeMapper.selectList(
                        new LambdaQueryWrapper<DataScopeEntity>()
                                .in(DataScopeEntity::getRoleId, user.getRoleIds())
                                .eq(DataScopeEntity::getResourceType, resourceType)
                                .eq(DataScopeEntity::getStatus, 1));
        if (!scopedRules.isEmpty()) {
            mergeRules(result, scopedRules);
            return result;
        }
        List<RoleEntity> roles = roleMapper.selectBatchIds(user.getRoleIds());
        for (RoleEntity role : roles) {
            mergeRoleDefault(result, role, user);
        }
        if (result.isAllIncluded()) {
            result.setScopeType("all");
        } else if (!result.getCustomRules().isEmpty()) {
            result.setScopeType("custom");
        } else if (!result.getOrganizationIds().isEmpty()) {
            result.setScopeType("organization");
        } else if (!result.getDepartmentIds().isEmpty()) {
            result.setScopeType("department");
        } else if (result.isSelfIncluded()) {
            result.setScopeType("self");
        }
        return result;
    }

    @Override
    public <T> DataScopeResultVO applyToQuery(
            LambdaQueryWrapper<T> wrapper,
            String resourceType,
            SFunction<T, ?> ownerField,
            SFunction<T, ?> organizationField,
            SFunction<T, ?> departmentField) {
        DataScopeResultVO scope = resolveForCurrentUser(resourceType);
        if (scope.isAllIncluded()) {
            return scope;
        }
        CurrentUser user = SecurityUtils.currentUser();
        wrapper.and(
                condition -> {
                    boolean[] applied = {false};
                    if (scope.isSelfIncluded() && ownerField != null) {
                        appendOr(condition, applied);
                        condition.eq(ownerField, user.getUserId());
                    }
                    if (!scope.getOrganizationIds().isEmpty() && organizationField != null) {
                        appendOr(condition, applied);
                        condition.in(organizationField, scope.getOrganizationIds());
                    }
                    if (!scope.getDepartmentIds().isEmpty() && departmentField != null) {
                        appendOr(condition, applied);
                        condition.in(departmentField, scope.getDepartmentIds());
                    }
                    if (!applied[0]) {
                        condition.apply(ALWAYS_FALSE_SQL);
                    }
                });
        return scope;
    }

    @Override
    public List<DataScopeVO> listByRole(Long roleId) {
        return dataScopeMapper
                .selectList(
                        new LambdaQueryWrapper<DataScopeEntity>()
                                .eq(DataScopeEntity::getRoleId, roleId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public Long save(DataScopeDTO dto) {
        RoleEntity role = roleMapper.selectById(dto.getRoleId());
        if (role == null || role.getStatus() == null || role.getStatus() != 1) {
            throw new ResourceNotFoundException("角色不存在或已停用");
        }
        DataScopeEntity entity =
                dataScopeMapper.selectOne(
                        new LambdaQueryWrapper<DataScopeEntity>()
                                .eq(DataScopeEntity::getRoleId, dto.getRoleId())
                                .eq(DataScopeEntity::getResourceType, dto.getResourceType()));
        if (entity == null) {
            entity = new DataScopeEntity();
            entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        } else {
            entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        }
        entity.setRoleId(dto.getRoleId());
        entity.setResourceType(dto.getResourceType());
        entity.setScopeType(dto.getScopeType());
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setCustomRule(dto.getCustomRule());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (entity.getId() == null) {
            dataScopeMapper.insert(entity);
        } else {
            dataScopeMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        if (dataScopeMapper.selectById(id) == null) {
            throw new ResourceNotFoundException("数据范围配置不存在");
        }
        dataScopeMapper.deleteById(id);
    }

    private <T> void appendOr(LambdaQueryWrapper<T> wrapper, boolean[] applied) {
        if (applied[0]) {
            wrapper.or();
            return;
        }
        applied[0] = true;
    }

    private void mergeRules(DataScopeResultVO result, List<DataScopeEntity> rules) {
        CurrentUser user = SecurityUtils.currentUser();
        for (DataScopeEntity rule : rules) {
            switch (rule.getScopeType()) {
                case "all" -> result.setAllIncluded(true);
                case "organization" -> {
                    if (rule.getOrganizationId() != null) {
                        result.getOrganizationIds().add(rule.getOrganizationId());
                    } else if (user.getOrganizationId() != null) {
                        result.getOrganizationIds().add(user.getOrganizationId());
                    }
                }
                case "department" -> {
                    if (rule.getDepartmentId() != null) {
                        result.getDepartmentIds().add(rule.getDepartmentId());
                    } else if (user.getDepartmentId() != null) {
                        result.getDepartmentIds().add(user.getDepartmentId());
                    }
                }
                case "custom" -> {
                    if (StringUtils.hasText(rule.getCustomRule())) {
                        result.getCustomRules().add(rule.getCustomRule());
                    }
                }
                case "self" -> result.setSelfIncluded(true);
                default -> {
                    // Unknown data scope values are ignored to avoid granting accidental access.
                }
            }
        }
        if (result.isAllIncluded()) {
            result.setScopeType("all");
        } else if (!result.getCustomRules().isEmpty()) {
            result.setScopeType("custom");
        } else if (!result.getOrganizationIds().isEmpty()) {
            result.setScopeType("organization");
        } else if (!result.getDepartmentIds().isEmpty()) {
            result.setScopeType("department");
        } else if (result.isSelfIncluded()) {
            result.setScopeType("self");
        }
    }

    private void mergeRoleDefault(DataScopeResultVO result, RoleEntity role, CurrentUser user) {
        if (role == null || role.getStatus() == null || role.getStatus() != 1) {
            return;
        }
        String scope = StringUtils.hasText(role.getDataScope()) ? role.getDataScope() : "self";
        switch (scope) {
            case "all" -> result.setAllIncluded(true);
            case "organization" -> {
                if (user.getOrganizationId() != null) {
                    result.getOrganizationIds().add(user.getOrganizationId());
                }
            }
            case "department" -> {
                if (user.getDepartmentId() != null) {
                    result.getDepartmentIds().add(user.getDepartmentId());
                }
            }
            case "custom" -> result.setSelfIncluded(true);
            case "self" -> result.setSelfIncluded(true);
            default -> {
                // Unknown role default scope values are ignored.
            }
        }
    }

    private DataScopeVO toVO(DataScopeEntity entity) {
        DataScopeVO vo = new DataScopeVO();
        vo.setId(entity.getId());
        vo.setRoleId(entity.getRoleId());
        vo.setResourceType(entity.getResourceType());
        vo.setScopeType(entity.getScopeType());
        vo.setOrganizationId(entity.getOrganizationId());
        vo.setDepartmentId(entity.getDepartmentId());
        vo.setCustomRule(entity.getCustomRule());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
