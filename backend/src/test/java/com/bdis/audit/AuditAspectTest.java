package com.bdis.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.audit.aspect.AuditAspect;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock private AuditLogService auditLogService;

    @Mock private ProceedingJoinPoint joinPoint;

    @Mock private Signature signature;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void loginRequestUsesDedicatedLoginLogOnly() throws Throwable {
        bindRequest("POST", "/api/auth/sessions");
        when(joinPoint.proceed()).thenReturn("login-result");
        AuditAspect aspect = new AuditAspect(auditLogService);

        Object result = aspect.aroundControllerWrite(joinPoint);

        assertThat(result).isEqualTo("login-result");
        verify(joinPoint).proceed();
        verifyNoInteractions(auditLogService);
    }

    @Test
    void bootstrapRequestUsesDedicatedLoginLogOnly() throws Throwable {
        bindRequest("POST", "/api/auth/bootstrap-admin");
        when(joinPoint.proceed()).thenReturn("bootstrap-result");
        AuditAspect aspect = new AuditAspect(auditLogService);

        Object result = aspect.aroundControllerWrite(joinPoint);

        assertThat(result).isEqualTo("bootstrap-result");
        verify(joinPoint).proceed();
        verifyNoInteractions(auditLogService);
    }

    @Test
    void ordinaryControllerWriteStillCreatesOperationAudit() throws Throwable {
        bindRequest("PUT", "/api/me/settings/common");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("updateSetting");
        when(joinPoint.proceed()).thenReturn("updated");
        AuditAspect aspect = new AuditAspect(auditLogService);

        Object result = aspect.aroundControllerWrite(joinPoint);

        assertThat(result).isEqualTo("updated");
        ArgumentCaptor<AuditRecordDTO> captor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(captor.capture());
        assertThat(captor.getValue().getOperationModule()).isEqualTo("M02_PERSONAL_SETTINGS");
        assertThat(captor.getValue().getOperationType()).isEqualTo("PUT:updateSetting");
        assertThat(captor.getValue().getOperationResult()).isEqualTo("SUCCESS");
    }

    private void bindRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
