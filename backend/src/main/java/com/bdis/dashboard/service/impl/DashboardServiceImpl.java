package com.bdis.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.dashboard.query.DashboardQuery;
import com.bdis.dashboard.service.DashboardService;
import com.bdis.dashboard.vo.DashboardMapVO;
import com.bdis.dashboard.vo.DashboardRecentGrowthRecordVO;
import com.bdis.dashboard.vo.DashboardSummaryVO;
import com.bdis.dashboard.vo.DashboardTodoVO;
import com.bdis.modules.dashboard.entity.DashboardSnapshotEntity;
import com.bdis.modules.dashboard.mapper.DashboardSnapshotMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
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
    private final DataScopeService dataScopeService;
    private final AuthorizationService authorizationService;

    public DashboardServiceImpl(
            JdbcTemplate jdbcTemplate,
            DashboardSnapshotMapper dashboardSnapshotMapper,
            ObjectMapper objectMapper,
            DataScopeService dataScopeService,
            AuthorizationService authorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dashboardSnapshotMapper = dashboardSnapshotMapper;
        this.objectMapper = objectMapper;
        this.dataScopeService = dataScopeService;
        this.authorizationService = authorizationService;
    }

    @Override
    public DashboardSummaryVO summary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setHerbCount(count("herb_species"));
        vo.setBaseCount(count("herb_base"));
        vo.setMapPointCount(count("herb_distribution"));
        vo.setGrowthRecordCount(
                countScoped("herb_growth_record", "collector_id", "herb_growth_record"));
        vo.setPendingGrowthReviewCount(
                countScopedByStatus(
                        "herb_growth_record",
                        "review_status",
                        "SUBMITTED",
                        "collector_id",
                        "herb_growth_record"));
        vo.setPendingDeclarationReviewCount(
                countScopedByStatus(
                        "eval_application",
                        "review_status",
                        "SUBMITTED",
                        "applicant_id",
                        "eval_application"));
        vo.setPendingPerformanceReviewCount(
                countScopedByStatus(
                        "perf_record", "identify_status", "SUBMITTED", "user_id", "perf_record"));
        vo.setCourseCount(count("edu_course"));
        vo.setFileCount(countVisibleFiles());
        vo.setSoapFailedCount(
                authorizationService.hasPermission("soap:exchange:view")
                        ? countByStatus("soap_sync_task", "sync_status", "FAILED")
                        : 0);
        vo.setTotalPendingTaskCount(
                vo.getPendingGrowthReviewCount()
                        + vo.getPendingDeclarationReviewCount()
                        + vo.getPendingPerformanceReviewCount()
                        + vo.getSoapFailedCount());
        return vo;
    }

    @Override
    public List<DashboardRecentGrowthRecordVO> recentGrowthRecords(DashboardQuery query) {
        if (!tableExists("herb_growth_record")) {
            return Collections.emptyList();
        }
        List<Object> params = new ArrayList<>();
        String sql =
                """
                select r.id,
                       s.herb_name,
                       b.base_name,
                       r.collector_name_snapshot as collector_name,
                       r.review_status,
                       r.collected_at
                from herb_growth_record r
                left join herb_species s on s.id = r.species_id and s.is_deleted = 0
                left join herb_distribution d on d.id = r.distribution_id and d.is_deleted = 0
                left join herb_base b on b.id = d.base_id and b.is_deleted = 0
                where r.is_deleted = 0
                """;
        sql = sql + dataScopeFilter("herb_growth_record", "collector_id", "r.collector_id", params);
        sql = sql + " order by r.collected_at desc limit ?";
        params.add(query.getLimit());
        try {
            return jdbcTemplate.query(sql, this::toRecentGrowthRecordVO, params.toArray());
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
        if ((query.getType() == null || "SOAP_FAILED".equalsIgnoreCase(query.getType()))
                && authorizationService.hasPermission("soap:exchange:view")) {
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
                select r.id, s.herb_name, r.review_status, r.updated_at
                from herb_growth_record r
                left join herb_species s on s.id = r.species_id and s.is_deleted = 0
                where r.is_deleted = 0 and lower(r.review_status) = 'submitted'
                """;
        sql = sql + dataScopeFilter("herb_growth_record", "collector_id", "r.collector_id", params);
        sql = sql + " order by r.updated_at desc limit ?";
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
        sql = sql + dataScopeFilter("eval_application", "applicant_id", "applicant_id", params);
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
        sql = sql + dataScopeFilter("perf_record", "user_id", "user_id", params);
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
                select id, task_name, sync_status, updated_at
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
        vo.setDistrictStatistics(
                queryMapList(
                        """
                        select coalesce(district, '未标注') as district_name,
                               count(*) as point_count
                        from herb_distribution
                        where is_deleted = 0
                        group by district
                        order by point_count desc
                        """,
                        "herb_distribution"));
        vo.setPoints(
                queryMapList(
                        """
                        select d.id,
                               d.species_id,
                               s.herb_name,
                               d.base_id,
                               b.base_name,
                               d.district as district_name,
                               d.longitude,
                               d.latitude
                        from herb_distribution d
                        left join herb_species s on s.id = d.species_id and s.is_deleted = 0
                        left join herb_base b on b.id = d.base_id and b.is_deleted = 0
                        where d.is_deleted = 0
                        limit 200
                        """,
                        "herb_distribution"));
        return vo;
    }

    @Override
    public void refreshSnapshot() {
        writeTodaySnapshot(summary());
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

    private long countScoped(String tableName, String ownerColumn, String resourceType) {
        if (!tableExists(tableName)) {
            return 0;
        }
        List<Object> params = new ArrayList<>();
        String sql =
                "select count(*) from "
                        + tableName
                        + " where coalesce(is_deleted, 0) = 0"
                        + dataScopeFilter(
                                tableName, ownerColumn, ownerColumn, params, resourceType);
        return queryCount(sql, params);
    }

    private long countScopedByStatus(
            String tableName,
            String statusColumn,
            String statusValue,
            String ownerColumn,
            String resourceType) {
        if (!tableExists(tableName)) {
            return 0;
        }
        List<Object> params = new ArrayList<>();
        params.add(statusValue);
        String sql =
                "select count(*) from "
                        + tableName
                        + " where coalesce(is_deleted, 0) = 0 and lower("
                        + statusColumn
                        + ") = lower(?)"
                        + dataScopeFilter(
                                tableName, ownerColumn, ownerColumn, params, resourceType);
        return queryCount(sql, params);
    }

    private long countVisibleFiles() {
        if (!tableExists("sys_file_resource")) {
            return 0;
        }
        if (isAdmin()) {
            return count("sys_file_resource");
        }
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (currentUserId == null || currentUserId <= 0) {
            return 0;
        }
        return queryCount(
                "select count(*) from sys_file_resource where is_deleted = 0 and uploader_id = ?",
                List.of(currentUserId));
    }

    private long queryCount(String sql, List<Object> params) {
        try {
            Long value =
                    params.isEmpty()
                            ? jdbcTemplate.queryForObject(sql, Long.class)
                            : jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
            return value == null ? 0 : value;
        } catch (DataAccessException exception) {
            return 0;
        }
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
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

    private String dataScopeFilter(
            String tableName,
            String ownerColumn,
            String qualifiedOwnerColumn,
            List<Object> params) {
        return dataScopeFilter(tableName, ownerColumn, qualifiedOwnerColumn, params, tableName);
    }

    private String dataScopeFilter(
            String tableName,
            String ownerColumn,
            String qualifiedOwnerColumn,
            List<Object> params,
            String resourceType) {
        DataScopeResultVO scope = dataScopeService.resolveForCurrentUser(resourceType);
        if (scope.isAllIncluded()) {
            return "";
        }
        if (!tableExists(tableName) || !columnExists(tableName, ownerColumn)) {
            return " and 1 = 0";
        }
        List<String> clauses = new ArrayList<>();
        if (scope.isSelfIncluded()) {
            Long currentUserId = CurrentUserUtils.currentUserId();
            if (currentUserId != null && currentUserId > 0) {
                clauses.add(qualifiedOwnerColumn + " = ?");
                params.add(currentUserId);
            }
        }
        if (!scope.getOrganizationIds().isEmpty()) {
            clauses.add(
                    qualifiedOwnerColumn
                            + " in (select id from sys_user where is_deleted = 0 and organization_id in ("
                            + placeholders(scope.getOrganizationIds().size())
                            + "))");
            params.addAll(scope.getOrganizationIds());
        }
        if (!scope.getDepartmentIds().isEmpty()) {
            clauses.add(
                    qualifiedOwnerColumn
                            + " in (select id from sys_user where is_deleted = 0 and department_id in ("
                            + placeholders(scope.getDepartmentIds().size())
                            + "))");
            params.addAll(scope.getDepartmentIds());
        }
        if (clauses.isEmpty()) {
            return " and 1 = 0";
        }
        return " and (" + String.join(" or ", clauses) + ")";
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
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
        entity.setPendingReviewCount(toInt(summary.getTotalPendingTaskCount()));
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
        vo.setRoute("/performance?performanceId=" + rs.getLong("id"));
        return vo;
    }

    private DashboardTodoVO toSoapTodoVO(ResultSet rs, int rowNum) throws SQLException {
        DashboardTodoVO vo = new DashboardTodoVO();
        vo.setTodoType("SOAP_FAILED");
        vo.setBizId(rs.getLong("id"));
        vo.setTitle("SOAP 同步失败：" + rs.getString("task_name"));
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
