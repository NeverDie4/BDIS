package com.bdis.dashboard.admin.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDashboardSummaryVO {

    private Long totalUsers;
    private Long enabledUsers;
    private Long disabledUsers;
    private Long mustChangePasswordUsers;
    private Long usersWithoutRole;
    private Long totalRoles;
    private Long rolesWithoutPermission;
    private Long totalPermissions;
    private Long inactivePermissions;
    private Long menusWithoutRoute;
    private Long failedLogins24h;
    private Long failedOperations24h;
    private Long activeSessions;
    private Long staleActiveSessions;
}
