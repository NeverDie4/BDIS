package com.bdis.audit.aspect;

import com.bdis.audit.annotation.AuditLogAnnotation;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

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
            auditLogService.record(dto);
            return result;
        } catch (Throwable throwable) {
            dto.setOperationResult("FAILED");
            dto.setErrorMessage(throwable.getMessage());
            auditLogService.record(dto);
            throw throwable;
        }
    }
}
