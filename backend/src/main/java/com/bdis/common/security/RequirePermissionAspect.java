package com.bdis.common.security;

import com.bdis.modules.permission.service.AuthorizationService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequirePermissionAspect {

    private final AuthorizationService authorizationService;

    public RequirePermissionAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Before("@annotation(requirePermission)")
    public void require(RequirePermission requirePermission) {
        authorizationService.requirePermission(requirePermission.value());
    }
}
