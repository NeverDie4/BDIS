package com.bdis.modules.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.user.service.RoleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock private RoleService roleService;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private RoleController controller;

    @Test
    void permissionIdsShouldRequireAssignmentPermission() {
        when(roleService.permissionIds(3L)).thenReturn(List.of(5L, 9L));

        Result<List<Long>> result = controller.permissionIds(3L);

        verify(authorizationService).requirePermission("auth:role:assign-permission");
        verify(roleService).permissionIds(3L);
        assertThat(result.getData()).containsExactly(5L, 9L);
    }
}
