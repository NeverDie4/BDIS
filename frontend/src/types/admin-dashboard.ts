export type AdminDashboardSummary = {
  totalUsers?: number;
  enabledUsers?: number;
  disabledUsers?: number;
  mustChangePasswordUsers?: number;
  usersWithoutRole?: number;
  totalRoles?: number;
  rolesWithoutPermission?: number;
  totalPermissions?: number;
  inactivePermissions?: number;
  menusWithoutRoute?: number;
  failedLogins24h?: number;
  failedOperations24h?: number;
  activeSessions?: number;
  staleActiveSessions?: number;
};
