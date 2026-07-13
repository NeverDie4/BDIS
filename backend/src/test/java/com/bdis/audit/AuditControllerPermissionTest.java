package com.bdis.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.audit.controller.AuditLogController;
import com.bdis.audit.controller.DataSyncLogController;
import com.bdis.audit.controller.FileAccessLogController;
import com.bdis.audit.controller.LoginLogController;
import com.bdis.common.security.RequirePermission;
import org.junit.jupiter.api.Test;

class AuditControllerPermissionTest {

    @Test
    void auditControllersUseDedicatedPermissions() {
        assertPermission(AuditLogController.class, "audit:operation:view");
        assertPermission(LoginLogController.class, "audit:login:view");
        assertPermission(FileAccessLogController.class, "audit:file:view");
        assertPermission(DataSyncLogController.class, "audit:data-sync:view");
    }

    private void assertPermission(Class<?> controller, String expected) {
        RequirePermission permission = controller.getAnnotation(RequirePermission.class);
        assertThat(permission).isNotNull();
        assertThat(permission.value()).isEqualTo(expected);
    }
}
