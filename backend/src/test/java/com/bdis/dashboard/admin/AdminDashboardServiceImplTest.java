package com.bdis.dashboard.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.dashboard.admin.service.impl.AdminDashboardServiceImpl;
import com.bdis.dashboard.admin.vo.AdminDashboardSummaryVO;
import com.bdis.modules.permission.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @Mock private AuthorizationService authorizationService;

    @InjectMocks private AdminDashboardServiceImpl service;

    @Test
    void summaryOnlyQueriesStatisticsCoveredByCurrentPermissions() {
        when(authorizationService.hasPermission("auth:user:view")).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(4L);

        AdminDashboardSummaryVO summary = service.summary();

        assertThat(summary.getTotalUsers()).isEqualTo(4L);
        assertThat(summary.getUsersWithoutRole()).isEqualTo(4L);
        assertThat(summary.getTotalRoles()).isNull();
        assertThat(summary.getFailedLogins24h()).isNull();
        verify(authorizationService).hasPermission("auth:role:view");
        verify(authorizationService).hasPermission("auth:permission:view");
        verify(authorizationService).hasPermission("audit:login:view");
        verify(authorizationService).hasPermission("audit:operation:view");
    }

    @Test
    void summaryWithoutChildPermissionsDoesNotQueryDatabase() {
        AdminDashboardSummaryVO summary = service.summary();

        assertThat(summary.getTotalUsers()).isNull();
        assertThat(summary.getTotalPermissions()).isNull();
        assertThat(summary.getActiveSessions()).isNull();
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }
}
