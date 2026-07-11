package com.bdis.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.dashboard.service.impl.DashboardServiceImpl;
import com.bdis.dashboard.vo.DashboardSummaryVO;
import com.bdis.modules.dashboard.mapper.DashboardSnapshotMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private DashboardSnapshotMapper dashboardSnapshotMapper;
    @Mock private ObjectMapper objectMapper;
    @Mock private DataScopeService dataScopeService;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUpCurrentUser() {
        CurrentUser user =
                new CurrentUser(
                        1L,
                        "admin",
                        "Administrator",
                        null,
                        null,
                        Set.of("ADMIN"),
                        Set.of(1L),
                        Set.of("*"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void clearCurrentUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void summaryShouldAggregateCountsFromBusinessTables() throws Exception {
        DataScopeResultVO scope = new DataScopeResultVO();
        scope.setAllIncluded(true);
        when(dataScopeService.resolveForCurrentUser(anyString())).thenReturn(scope);
        when(authorizationService.hasPermission("soap:exchange:view")).thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        startsWith("select count(*) from information_schema.tables"),
                        eq(Integer.class),
                        anyString()))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                        eq("select count(*) from herb_species where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(3L);
        when(jdbcTemplate.queryForObject(
                        eq("select count(*) from herb_base where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(2L);
        when(jdbcTemplate.queryForObject(
                        eq(
                                "select count(*) from herb_distribution where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(5L);
        when(jdbcTemplate.queryForObject(
                        eq(
                                "select count(*) from herb_growth_record where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(8L);
        when(jdbcTemplate.queryForObject(
                        eq("select count(*) from edu_course where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(4L);
        when(jdbcTemplate.queryForObject(
                        eq(
                                "select count(*) from sys_file_resource where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(7L);
        when(jdbcTemplate.queryForObject(
                        eq(
                                "select count(*) from herb_growth_record where coalesce(is_deleted, 0) = 0 and lower(review_status) = lower(?)"),
                        eq(Long.class),
                        eq("SUBMITTED")))
                .thenReturn(1L);
        when(jdbcTemplate.queryForObject(
                        eq(
                                "select count(*) from eval_application where coalesce(is_deleted, 0) = 0 and lower(review_status) = lower(?)"),
                        eq(Long.class),
                        eq("SUBMITTED")))
                .thenReturn(3L);
        when(jdbcTemplate.queryForObject(
                        eq(
                                "select count(*) from perf_record where coalesce(is_deleted, 0) = 0 and lower(identify_status) = lower(?)"),
                        eq(Long.class),
                        eq("SUBMITTED")))
                .thenReturn(4L);
        when(jdbcTemplate.queryForObject(
                        eq(
                                "select count(*) from soap_sync_task where coalesce(is_deleted, 0) = 0 and lower(sync_status) = lower(?)"),
                        eq(Long.class),
                        eq("FAILED")))
                .thenReturn(2L);
        DashboardSummaryVO summary = dashboardService.summary();

        assertThat(summary.getHerbCount()).isEqualTo(3L);
        assertThat(summary.getBaseCount()).isEqualTo(2L);
        assertThat(summary.getMapPointCount()).isEqualTo(5L);
        assertThat(summary.getGrowthRecordCount()).isEqualTo(8L);
        assertThat(summary.getPendingGrowthReviewCount()).isEqualTo(1L);
        assertThat(summary.getPendingDeclarationReviewCount()).isEqualTo(3L);
        assertThat(summary.getPendingPerformanceReviewCount()).isEqualTo(4L);
        assertThat(summary.getTotalPendingTaskCount()).isEqualTo(10L);
        assertThat(summary.getCourseCount()).isEqualTo(4L);
        assertThat(summary.getFileCount()).isEqualTo(7L);
        assertThat(summary.getSoapFailedCount()).isEqualTo(2L);
    }
}
