package com.bdis.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.dashboard.mapper.DashboardSnapshotMapper;
import com.bdis.dashboard.service.impl.DashboardServiceImpl;
import com.bdis.dashboard.vo.DashboardSummaryVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private DashboardSnapshotMapper dashboardSnapshotMapper;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private DashboardServiceImpl dashboardService;

    @Test
    void summaryShouldAggregateCountsFromBusinessTables() throws Exception {
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
                        eq("select count(*) from herb_distribution where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(5L);
        when(jdbcTemplate.queryForObject(
                        eq("select count(*) from herb_growth_record where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(8L);
        when(jdbcTemplate.queryForObject(
                        eq("select count(*) from edu_course where coalesce(is_deleted, 0) = 0"),
                        eq(Long.class)))
                .thenReturn(4L);
        when(jdbcTemplate.queryForObject(
                        eq("select count(*) from sys_file_resource where coalesce(is_deleted, 0) = 0"),
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
        when(dashboardSnapshotMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(objectMapper.writeValueAsString(any(DashboardSummaryVO.class))).thenReturn("{}");

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
