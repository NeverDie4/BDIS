package com.bdis.modules.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.modules.permission.entity.RolePermissionEntity;
import com.bdis.modules.permission.mapper.DataScopeMapper;
import com.bdis.modules.permission.mapper.PermissionMapper;
import com.bdis.modules.permission.mapper.RolePermissionMapper;
import com.bdis.modules.user.dto.RoleCreateDTO;
import com.bdis.modules.user.dto.RoleUpdateDTO;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import com.bdis.modules.user.mapper.UserRoleMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private RolePermissionMapper rolePermissionMapper;
    @Mock private DataScopeMapper dataScopeMapper;

    @InjectMocks private RoleServiceImpl roleService;

    @Test
    void createShouldRejectSystemRole() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setRoleCode("CUSTOM_SYSTEM");
        dto.setRoleName("自定义系统角色");
        dto.setRoleType("system");

        assertThatThrownBy(() -> roleService.create(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("系统角色只能由系统初始化");

        verify(roleMapper, never()).insert(any(RoleEntity.class));
    }

    @Test
    void updateShouldRejectDisablingSystemRole() {
        RoleEntity role = role(1L, "ADMIN", "system", "all");
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setStatus(0);
        when(roleMapper.selectById(1L)).thenReturn(role);

        assertThatThrownBy(() -> roleService.update(1L, dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("系统角色不能停用");

        verify(roleMapper, never()).updateById(any(RoleEntity.class));
    }

    @Test
    void deleteShouldRejectSystemRole() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "ADMIN", "system", "all"));

        assertThatThrownBy(() -> roleService.delete(1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("系统角色不能删除");

        verify(userRoleMapper, never()).selectCount(any());
        verify(roleMapper, never()).deleteById(1L);
    }

    @Test
    void deleteShouldRemoveUnusedBusinessRole() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "RESEARCHER", "business", "self"));
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        roleService.delete(2L);

        verify(rolePermissionMapper).delete(any());
        verify(dataScopeMapper).delete(any());
        verify(roleMapper).deleteById(2L);
    }

    @Test
    void permissionIdsShouldReturnAssignedPermissionIds() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "RESEARCHER", "business", "self"));
        RolePermissionEntity first = new RolePermissionEntity();
        first.setPermissionId(11L);
        RolePermissionEntity second = new RolePermissionEntity();
        second.setPermissionId(18L);
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(first, second));

        assertThat(roleService.permissionIds(2L)).containsExactly(11L, 18L);
    }

    private RoleEntity role(Long id, String code, String type, String dataScope) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleType(type);
        role.setDataScope(dataScope);
        role.setStatus(1);
        return role;
    }
}
