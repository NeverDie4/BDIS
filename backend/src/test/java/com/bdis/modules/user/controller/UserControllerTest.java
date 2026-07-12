package com.bdis.modules.user.controller;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.user.dto.UserCreateDTO;
import com.bdis.modules.user.dto.UserUpdateDTO;
import com.bdis.modules.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private UserController controller;

    @Test
    void createShouldRequireRoleAssignmentPermission() {
        UserCreateDTO dto = new UserCreateDTO();

        controller.create(dto);

        InOrder order = inOrder(authorizationService, userService);
        order.verify(authorizationService).requirePermission("auth:user:create");
        order.verify(authorizationService).requirePermission("auth:user:assign-role");
        order.verify(userService).create(dto);
    }

    @Test
    void updateShouldRequireRoleAssignmentPermissionWhenRoleIdsArePresent() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setRoleIds(List.of(1L, 2L));

        controller.update(8L, dto);

        InOrder order = inOrder(authorizationService, userService);
        order.verify(authorizationService).requirePermission("auth:user:update");
        order.verify(authorizationService).requirePermission("auth:user:assign-role");
        order.verify(userService).update(8L, dto);
    }

    @Test
    void updateShouldNotRequireRoleAssignmentPermissionWhenRolesAreOmitted() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setRealName("测试用户");

        controller.update(8L, dto);

        verify(authorizationService).requirePermission("auth:user:update");
        verify(userService).update(8L, dto);
        verifyNoMoreInteractions(authorizationService);
    }

    @Test
    void patchShouldAlsoProtectRoleAssignment() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setRoleIds(List.of());

        controller.patch(8L, dto);

        verify(authorizationService).requirePermission("auth:user:update");
        verify(authorizationService).requirePermission("auth:user:assign-role");
        verify(userService).patch(8L, dto);
    }
}
