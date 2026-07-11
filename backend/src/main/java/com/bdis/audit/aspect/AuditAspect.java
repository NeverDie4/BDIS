package com.bdis.audit.aspect;

import com.bdis.audit.annotation.AuditLogAnnotation;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Pointcut("@annotation(auditLogAnnotation)")
    public void auditPointcut(AuditLogAnnotation auditLogAnnotation) {}

    @Around("auditPointcut(auditLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLogAnnotation auditLogAnnotation)
            throws Throwable {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule(auditLogAnnotation.module());
        dto.setOperationType(auditLogAnnotation.operationType());
        dto.setBizType(auditLogAnnotation.bizType());
        try {
            Object result = joinPoint.proceed();
            dto.setOperationResult("SUCCESS");
            recordSafely(dto);
            return result;
        } catch (Throwable throwable) {
            dto.setOperationResult("FAILED");
            dto.setErrorMessage(throwable.getMessage());
            recordSafely(dto);
            throw throwable;
        }
    }

    @Around(
            "within(@org.springframework.web.bind.annotation.RestController *)"
                    + " && execution(public * *(..))"
                    + " && !@annotation(com.bdis.audit.annotation.AuditLogAnnotation)")
    public Object aroundControllerWrite(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null
                || "GET".equalsIgnoreCase(request.getMethod())
                || isSelfAudited(request.getRequestURI())) {
            return joinPoint.proceed();
        }
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule(resolveModule(request.getRequestURI()));
        dto.setOperationType(request.getMethod() + ":" + joinPoint.getSignature().getName());
        dto.setBizType(resolveBizType(request.getRequestURI()));
        try {
            Object result = joinPoint.proceed();
            dto.setOperationResult("SUCCESS");
            recordSafely(dto);
            return result;
        } catch (Throwable throwable) {
            dto.setOperationResult("FAILED");
            dto.setErrorMessage(throwable.getMessage());
            recordSafely(dto);
            throw throwable;
        }
    }

    private void recordSafely(AuditRecordDTO dto) {
        try {
            auditLogService.record(dto);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to persist audit log", exception);
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String resolveModule(String uri) {
        if (uri.contains("/files") || uri.contains("/file-relations")) {
            return "M05_FILE";
        }
        if (uri.contains("/soap-")) {
            return "M19_SOAP";
        }
        if (uri.contains("/herb/atlas") || uri.contains("/identification")) {
            return "M11_SPECTRUM";
        }
        if (uri.contains("/herb/") || uri.contains("/map-points")) {
            return "M07_M10_HERB";
        }
        if (uri.contains("/courses")) {
            return "M12_COURSE";
        }
        if (uri.contains("/dictionaries") || uri.contains("/regions")) {
            return "M04_DICTIONARY";
        }
        if (uri.contains("/users")
                || uri.contains("/roles")
                || uri.contains("/permissions")
                || uri.contains("/menus")) {
            return "M02_M03_AUTH";
        }
        return "SYSTEM";
    }

    private String resolveBizType(String uri) {
        String path = uri.replaceFirst("^/api/?", "");
        int separator = path.indexOf('/');
        return separator < 0 ? path : path.substring(0, separator);
    }

    private boolean isSelfAudited(String uri) {
        return uri.contains("/files")
                || uri.contains("/file-relations")
                || uri.contains("/soap-exchange-jobs");
    }
}
