package com.bdis.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.dashboard.entity.DashboardSnapshotEntity;
import com.bdis.dashboard.mapper.DashboardSnapshotMapper;
import com.bdis.dashboard.query.DashboardQuery;
import com.bdis.dashboard.service.DashboardService;
import com.bdis.dashboard.vo.DashboardMapVO;
import com.bdis.dashboard.vo.DashboardRecentGrowthRecordVO;
import com.bdis.dashboard.vo.DashboardSummaryVO;
import com.bdis.dashboard.vo.DashboardTodoVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final DashboardSnapshotMapper dashboardSnapshotMapper;
    private final ObjectMapper objectMapper;

    public DashboardServiceImpl(
            JdbcTemplate jdbcTemplate,
            DashboardSnapshotMapper dashboardSnapshotMapper,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.dashboardSnapshotMapper = dashboardSnapshotMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardSummaryVO summary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setHerbCount(count("herb_species"));
        vo.setBaseCount(count("herb_base"));
        vo.setMapPointCount(count("herb_distribution"));
        vo.setGrowthRecordCount(count("herb_growth_record"));
        vo.setPendingGrowthReviewCount(countByStatus("herb_growth_record", "review_status", "SUBMITTED"));
        vo.setPendingDeclarationReviewCount(countByStatus("eval_application", "review_status", "SUBMITTED"));
        vo.setPendingPerformanceReviewCount(countByStatus("perf_record", "identify_status", "SUBMITTED"));
        vo.setCourseCount(count("edu_course"));
        vo.setFileCount(count("sys_file_resource"));
        vo.setSoapFailedCount(countByStatus("soap_sync_task", "sync_status", "FAILED"));
        vo.setTotalPendingTaskCount(
                vo.getPendingGrowthReviewCount()
                        + vo.getPendingDeclarationReviewCount()
                        + vo.getPendingPerformanceReviewCount()
                        + vo.getSoapFailedCount());
        writeTodaySnapshot(vo);
        return vo;
    }

    @Override
    public List<DashboardRecentGrowthRecordVO> recentGrowthRecords(DashboardQuery query) {
        if (!tableExists("herb_growth_record")) {
            return Collections.emptyList();
        }
        String sql =
                """
                select id, herb_name, base_name, collector_name, review_status, collected_at
                from herb_growth_record
                where coalesce(is_deleted, 0) = 0
                order by collected_at desc
                limit ?
                """;
        try {
            return jdbcTemplate.query(sql, this::toRecentGrowthRecordVO, query.getLimit());
        } catch (DataAccessException exception) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<DashboardTodoVO> pendingTasks(DashboardQuery query) {
        int limit = query.getLimit() == null ? 10 : query.getLimit();
        List<DashboardTodoVO> todos = new ArrayList<>();
        if (query.getType() == null || "GROWTH_REVIEW".equalsIgnoreCase(query.getType())) {
            todos.addAll(pendingGrowthReviewTasks(query, limit));
        }
        if (query.getType() == null || "DECLARATION_REVIEW".equalsIgnoreCase(query.getType())) {
            todos.addAll(pendingDeclarationReviewTasks(query, limit));
        }
        if (query.getType() == null || "PERFORMANCE_REVIEW".equalsIgnoreCase(query.getType())) {
            todos.addAll(pendingPerformanceReviewTasks(query, limit));
        }
        if (query.getType() == null || "SOAP_FAILED".equalsIgnoreCase(query.getType())) {
            todos.addAll(pendingSoapFailedTasks(limit));
        }
        return todos.stream().limit(limit).toList();
    }

    private List<DashboardTodoVO> pendingGrowthReviewTasks(DashboardQuery query, Integer limit) {
        if (!tableExists("herb_growth_record")) {
            return Collections.emptyList();
        }
        List<Object> params = new ArrayList<>();
        String sql =
                """
                select id, herb_name, review_status, updated_at
                from herb_growth_record
                where coalesce(is_deleted, 0) = 0 and lower(review_status) = 'submitted'
                """;
        sql = sql + ownerFilter("herb_growth_record", "collector_id", query, params);
        sql = sql + " order by updated_at desc limit ?";
        params.add(limit);
        try {
            return jdbcTemplate.query(sql, this::toGrowthTodoVO, params.toArray());
        } catch (DataAccessException exception) {
            return Collections.emptyList();
        }
    }

    private List<DashboardTodoVO> pendingDeclarationReviewTasks(
            DashboardQuery query, Integer limit) {
        if (!tableExists("eval_application")) {
            return Collections.emptyList();
        }
        List<Object> params = new ArrayList<>();
        String sql =
                """
                select id, application_title, review_status, submitted_at, updated_at
                from eval_application
                where coalesce(is_deleted, 0) = 0 and lower(review_status) = 'submitted'
                """;
        sql = sql + ownerFilter("eval_application", "applicant_id", query, params);
        sql = sql + " order by coalesce(submitted_at, updated_at) desc limit ?";
        params.add(limit);
        try {
            return jdbcTemplate.query(sql, this::toDeclarationTodoVO, params.toArray());
        } catch (DataAccessException exception) {
            return Collections.emptyList();
        }
    }

    private List<DashboardTodoVO> pendingPerformanceReviewTasks(
            DashboardQuery query, Integer limit) {
        if (!tableExists("perf_record")) {
            return Collections.emptyList();
        }
        List<Object> params = new ArrayList<>();
        String sql =
                """
                select id, performance_title, identify_status, submitted_at, updated_at
                from perf_record
                where coalesce(is_deleted, 0) = 0 and lower(identify_status) = 'submitted'
                """;
        sql = sql + ownerFilter("perf_record", "user_id", query, params);
        sql = sql + " order by coalesce(submitted_at, updated_at) desc limit ?";
        params.add(limit);
        try {
            return jdbcTemplate.query(sql, this::toPerformanceTodoVO, params.toArray());
        } catch (DataAccessException exception) {
            return Collections.emptyList();
        }
    }

    private List<DashboardTodoVO> pendingSoapFailedTasks(Integer limit) {
        if (!tableExists("soap_sync_task")) {
            return Collections.emptyList();
        }
        String sql =
                """
                select id, resource_type, sync_status, updated_at
                from soap_sync_task
                where coalesce(is_deleted, 0) = 0 and lower(sync_status) = 'failed'
                order by updated_at desc
                limit ?
                """;
        try {
            return jdbcTemplate.query(sql, this::toSoapTodoVO, limit);
        } catch (DataAccessException exception) {
            return Collections.emptyList();
        }
    }

    @Override
    public DashboardMapVO mapOverview() {
        DashboardMapVO vo = new DashboardMapVO();
        vo.setPointCount(count("herb_distribution"));
        vo.setDistrictStatistics(queryMapList(
                """
                select district_code, district_name, count(*) as point_count
                from herb_distribution
                where coalesce(is_deleted, 0) = 0
                group by district_code, district_name
                order by point_count desc
                """,
                "herb_distribution"));
        vo.setPoints(queryMapList(
                """
                select id, herb_id, herb_name, base_id, base_name, district_name, longitude, latitude
                from herb_distribution
                where coalesce(is_deleted, 0) = 0
                limit 200
                """,
                "herb_distribution"));
        return vo;
    }

    private long count(String tableName) {
        if (!tableExists(tableName)) {
            return 0;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "select count(*) from " + tableName + " where coalesce(is_deleted, 0) = 0",
                    Long.class);
        } catch (DataAccessException exception) {
            try {
                return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
            } catch (DataAccessException ignored) {
                return 0;
            }
        }
    }

    private long countByStatus(String tableName, String statusColumn, String statusValue) {
        if (!tableExists(tableName)) {
            return 0;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "select count(*) from "
                            + tableName
                            + " where coalesce(is_deleted, 0) = 0 and "
                            + "lower("
                            + statusColumn
                            + ") = lower(?)",
                    Long.class,
                    statusValue);
        } catch (DataAccessException exception) {
            return 0;
        }
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count =
                    jdbcTemplate.queryForObject(
                            "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                            Integer.class,
                            tableName);
            return count != null && count > 0;
        } catch (DataAccessException exception) {
            return false;
        }
    }

    private List<Map<String, Object>> queryMapList(String sql, String tableName) {
        if (!tableExists(tableName)) {
            return Collections.emptyList();
        }
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (DataAccessException exception) {
            return Collections.emptyList();
        }
    }

    private String ownerFilter(
            String tableName, String ownerColumn, DashboardQuery query, List<Object> params) {
        if (isPrivilegedRole(query.getRoleType())) {
            return "";
        }
        if (!tableExists(tableName) || !columnExists(tableName, ownerColumn)) {
            return "";
        }
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (currentUserId == null || currentUserId <= 0) {
            return "";
        }
        params.add(currentUserId);
        return " and " + ownerColumn + " = ?";
    }

    private boolean isPrivilegedRole(String roleType) {
        if (roleType == null || roleType.isBlank()) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(roleType)
                || "REVIEWER".equalsIgnoreCase(roleType)
                || "AUDITOR".equalsIgnoreCase(roleType)
                || "TEACHER".equalsIgnoreCase(roleType);
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            Integer count =
                    jdbcTemplate.queryForObject(
                            "select count(*) from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                            Integer.class,
                            tableName,
                            columnName);
            return count != null && count > 0;
        } catch (DataAccessException exception) {
            return false;
        }
    }

    private void writeTodaySnapshot(DashboardSummaryVO summary) {
        if (!tableExists("stat_dashboard_snapshot")) {
            return;
        }
        LocalDate today = LocalDate.now();
        DashboardSnapshotEntity entity =
                dashboardSnapshotMapper.selectOne(
                        new LambdaQueryWrapper<DashboardSnapshotEntity>()
                                .eq(DashboardSnapshotEntity::getSnapshotDate, today)
                                .last("limit 1"));
        if (entity == null) {
            entity = new DashboardSnapshotEntity();
            entity.setSnapshotDate(today);
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setHerbCount(toInt(summary.getHerbCount()));
        entity.setBaseCount(toInt(summary.getBaseCount()));
        entity.setDistributionCount(toInt(summary.getMapPointCount()));
        entity.setGrowthRecordCount(toInt(summary.getGrowthRecordCount()));
        entity.setPendingTaskCount(toInt(summary.getTotalPendingTaskCount()));
        entity.setDashboardData(toJson(summary));
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getId() == null) {
            dashboardSnapshotMapper.insert(entity);
        } else {
            dashboardSnapshotMapper.updateById(entity);
        }
    }

    private Integer toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private String toJson(DashboardSummaryVO summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private DashboardRecentGrowthRecordVO toRecentGrowthRecordVO(ResultSet rs, int rowNum)
            throws SQLException {
        DashboardRecentGrowthRecordVO vo = new DashboardRecentGrowthRecordVO();
        vo.setRecordId(rs.getLong("id"));
        vo.setHerbName(rs.getString("herb_name"));
        vo.setBaseName(rs.getString("base_name"));
        vo.setCollectorName(rs.getString("collector_name"));
        vo.setReviewStatus(rs.getString("review_status"));
        vo.setCollectedAt(toLocalDateTime(rs, "collected_at"));
        return vo;
    }

    private DashboardTodoVO toGrowthTodoVO(ResultSet rs, int rowNum) throws SQLException {
        DashboardTodoVO vo = new DashboardTodoVO();
        vo.setTodoType("GROWTH_REVIEW");
        vo.setBizId(rs.getLong("id"));
        vo.setTitle("待审核采集记录：" + rs.getString("herb_name"));
        vo.setStatus(rs.getString("review_status"));
        vo.setSubmittedAt(toLocalDateTime(rs, "updated_at"));
        vo.setRoute("/growth-records/" + rs.getLong("id"));
        return vo;
    }

    private DashboardTodoVO toDeclarationTodoVO(ResultSet rs, int rowNum) throws SQLException {
        DashboardTodoVO vo = new DashboardTodoVO();
        vo.setTodoType("DECLARATION_REVIEW");
        vo.setBizId(rs.getLong("id"));
        vo.setTitle("待审核申报：" + rs.getString("application_title"));
        vo.setStatus(rs.getString("review_status"));
        vo.setSubmittedAt(firstLocalDateTime(rs, "submitted_at", "updated_at"));
        vo.setRoute("/declarations/" + rs.getLong("id"));
        return vo;
    }

    private DashboardTodoVO toPerformanceTodoVO(ResultSet rs, int rowNum) throws SQLException {
        DashboardTodoVO vo = new DashboardTodoVO();
        vo.setTodoType("PERFORMANCE_REVIEW");
        vo.setBizId(rs.getLong("id"));
        vo.setTitle("待认定业绩：" + rs.getString("performance_title"));
        vo.setStatus(rs.getString("identify_status"));
        vo.setSubmittedAt(firstLocalDateTime(rs, "submitted_at", "updated_at"));
        vo.setRoute("/performances/" + rs.getLong("id"));
        return vo;
    }

    private DashboardTodoVO toSoapTodoVO(ResultSet rs, int rowNum) throws SQLException {
        DashboardTodoVO vo = new DashboardTodoVO();
        vo.setTodoType("SOAP_FAILED");
        vo.setBizId(rs.getLong("id"));
        vo.setTitle("SOAP 同步失败：" + rs.getString("resource_type"));
        vo.setStatus(rs.getString("sync_status"));
        vo.setSubmittedAt(toLocalDateTime(rs, "updated_at"));
        vo.setRoute("/soap-exchange-jobs/" + rs.getLong("id"));
        return vo;
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        if (rs.getTimestamp(column) == null) {
            return null;
        }
        return rs.getTimestamp(column).toLocalDateTime();
    }

    private LocalDateTime firstLocalDateTime(ResultSet rs, String first, String second)
            throws SQLException {
        LocalDateTime firstValue = toLocalDateTime(rs, first);
        return firstValue == null ? toLocalDateTime(rs, second) : firstValue;
    }
}
