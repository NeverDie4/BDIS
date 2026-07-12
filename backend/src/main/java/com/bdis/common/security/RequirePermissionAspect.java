package com.bdis.common.security;

import com.bdis.modules.permission.service.AuthorizationService;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequirePermissionAspect {

    private final AuthorizationService authorizationService;

    public RequirePermissionAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Before(
            "within(@org.springframework.web.bind.annotation.RestController *)"
                    + " && execution(public * *(..))")
    public void require(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequirePermission requirePermission =
                AnnotationUtils.findAnnotation(method, RequirePermission.class);
        if (requirePermission == null) {
            requirePermission =
                    AnnotationUtils.findAnnotation(
                            joinPoint.getTarget().getClass(), RequirePermission.class);
        }
        if (requirePermission != null) {
            authorizationService.requirePermission(requirePermission.value());
        }
    }
}
