package com.bdis.common.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.permission.service.AuthorizationService;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class RequirePermissionAspectTest {

    @Mock private AuthorizationService authorizationService;
    @Mock private JoinPoint joinPoint;
    @Mock private MethodSignature methodSignature;

    @Test
    void classPermissionAppliesWhenMethodDoesNotOverrideIt() throws Exception {
        Method method = SecuredController.class.getMethod("list");
        prepare(method);
        when(joinPoint.getTarget()).thenReturn(new SecuredController());

        new RequirePermissionAspect(authorizationService).require(joinPoint);

        verify(authorizationService).requirePermission("resource:view");
    }

    @Test
    void methodPermissionOverridesClassPermission() throws Exception {
        Method method = SecuredController.class.getMethod("create");
        prepare(method);

        new RequirePermissionAspect(authorizationService).require(joinPoint);

        verify(authorizationService).requirePermission("resource:create");
    }

    private void prepare(Method method) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    @RestController
    @RequirePermission("resource:view")
    static class SecuredController {

        public void list() {}

        @RequirePermission("resource:create")
        public void create() {}
    }
}
