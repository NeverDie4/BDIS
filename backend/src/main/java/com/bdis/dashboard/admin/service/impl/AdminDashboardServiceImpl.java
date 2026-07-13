package com.bdis.dashboard.admin.service.impl;

import com.bdis.dashboard.admin.service.AdminDashboardService;
import com.bdis.dashboard.admin.vo.AdminDashboardSummaryVO;
import com.bdis.modules.permission.service.AuthorizationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final AuthorizationService authorizationService;

    public AdminDashboardServiceImpl(
            JdbcTemplate jdbcTemplate, AuthorizationService authorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authorizationService = authorizationService;
    }

    @Override
    public AdminDashboardSummaryVO summary() {
        AdminDashboardSummaryVO vo = new AdminDashboardSummaryVO();
        if (authorizationService.hasPermission("auth:user:view")) {
            vo.setTotalUsers(count("select count(*) from sys_user where is_deleted = 0"));
            vo.setEnabledUsers(
                    count("select count(*) from sys_user where is_deleted = 0 and status = 1"));
            vo.setDisabledUsers(
                    count("select count(*) from sys_user where is_deleted = 0 and status = 0"));
            vo.setMustChangePasswordUsers(
                    count(
                            "select count(*) from sys_user where is_deleted = 0 and"
                                    + " must_change_password = 1"));
            vo.setUsersWithoutRole(
                    count(
                            "select count(*) from sys_user u left join rel_user_role relation"
                                    + " on relation.user_id = u.id where u.is_deleted = 0"
                                    + " and relation.id is null"));
        }
        if (authorizationService.hasPermission("auth:role:view")) {
            vo.setTotalRoles(count("select count(*) from auth_role where is_deleted = 0"));
            vo.setRolesWithoutPermission(
                    count(
                            "select count(*) from auth_role r left join rel_role_permission"
                                    + " relation on relation.role_id = r.id where"
                                    + " r.is_deleted = 0 and relation.id is null"));
        }
        if (authorizationService.hasPermission("auth:permission:view")) {
            vo.setTotalPermissions(
                    count("select count(*) from auth_permission where is_deleted = 0"));
            vo.setInactivePermissions(
                    count(
                            "select count(*) from auth_permission where is_deleted = 0 and"
                                    + " status = 0"));
            vo.setMenusWithoutRoute(
                    count(
                            "select count(*) from auth_menu where is_deleted = 0 and status = 1"
                                    + " and visible = 1 and (route_path is null or route_path = '')"));
        }
        if (authorizationService.hasPermission("audit:login:view")) {
            vo.setFailedLogins24h(
                    count(
                            "select count(*) from log_login where login_result = 'FAILED' and"
                                    + " logged_in_at >= now() - interval 1 day"));
            vo.setActiveSessions(
                    count(
                            "select count(*) from auth_user_session where session_status ="
                                    + " 'active' and expires_at > now()"));
            vo.setStaleActiveSessions(
                    count(
                            "select count(*) from auth_user_session where session_status ="
                                    + " 'active' and expires_at <= now()"));
        }
        if (authorizationService.hasPermission("audit:operation:view")) {
            vo.setFailedOperations24h(
                    count(
                            "select count(*) from log_operation where result_status = 'FAILED'"
                                    + " and operation_time >= now() - interval 1 day"));
        }
        return vo;
    }

    private Long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
