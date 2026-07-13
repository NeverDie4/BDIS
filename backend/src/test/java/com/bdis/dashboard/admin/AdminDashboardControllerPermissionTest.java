package com.bdis.dashboard.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.common.security.RequirePermission;
import com.bdis.dashboard.admin.controller.AdminDashboardController;
import org.junit.jupiter.api.Test;

class AdminDashboardControllerPermissionTest {

    @Test
    void controllerRequiresDashboardPermission() {
        RequirePermission permission =
                AdminDashboardController.class.getAnnotation(RequirePermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).isEqualTo("auth:dashboard:view");
    }
}
