"use client";

import { AdminHealthList, type AdminHealthItem } from "@/components/dashboard/AdminHealthList";
import {
  DashboardMetric,
  DashboardPage,
  DashboardPanel,
} from "@/components/dashboard/DashboardPage";
import { useAdminDashboardSummary } from "@/hooks/dashboard/useAdminDashboardSummary";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import { Alert, Button, Skeleton } from "antd";
import { IdCard, KeyRound, Network, Users } from "lucide-react";

export default function AuthorizationCenterPage() {
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const summary = useAdminDashboardSummary();
  const data = summary.data;
  const issues: AdminHealthItem[] = [
    issue(
      "users-without-role",
      "未分配角色的账号",
      "账号没有角色时通常无法获得任何业务权限。",
      data?.usersWithoutRole,
      "/dashboard/users",
      hasPermission("auth:user:view"),
    ),
    issue(
      "roles-without-permission",
      "未配置权限的角色",
      "这些角色已存在，但尚未承载任何可执行权限。",
      data?.rolesWithoutPermission,
      "/dashboard/roles",
      hasPermission("auth:role:view"),
    ),
    issue(
      "inactive-permission",
      "停用的权限点",
      "停用权限不会参与有效授权，请核对是否为有意停用。",
      data?.inactivePermissions,
      "/dashboard/permissions",
      hasPermission("auth:permission:view"),
    ),
    issue(
      "menus-without-route",
      "缺少路由的可见菜单",
      "菜单已启用并展示，但没有可导航的前端路由。",
      data?.menusWithoutRoute,
      "/dashboard/permissions",
      hasPermission("auth:permission:view"),
    ),
  ].filter((item): item is AdminHealthItem => item !== null);

  return (
    <DashboardPage
      eyebrow="AUTHORIZATION GOVERNANCE"
      title="权限与组织治理"
      description="检查账号、角色、权限点和菜单配置的一致性。具体维护页面仍从左侧菜单进入，异常项可直接定位到相关模块。"
      metrics={
        <>
          <DashboardMetric icon={Users} label="账号总数" value={metric(data?.totalUsers)} />
          <DashboardMetric icon={IdCard} label="角色总数" value={metric(data?.totalRoles)} />
          <DashboardMetric
            icon={KeyRound}
            label="权限点总数"
            value={metric(data?.totalPermissions)}
          />
          <DashboardMetric
            icon={Network}
            label="未分配角色账号"
            value={metric(data?.usersWithoutRole)}
          />
        </>
      }
    >
      {summary.isLoading ? <Skeleton active paragraph={{ rows: 5 }} /> : null}
      {summary.isError ? (
        <Alert
          showIcon
          type="error"
          message="权限治理状态加载失败"
          description={getApiErrorMessage(summary.error, "无法读取权限治理状态")}
          action={<Button onClick={() => void summary.refetch()}>重新加载</Button>}
        />
      ) : null}
      {summary.isSuccess ? (
        <DashboardPanel
          title="配置一致性检查"
          description="没有对应查看权限的统计不会返回，也不会在此展示。"
          flush
        >
          <AdminHealthList items={issues} />
        </DashboardPanel>
      ) : null}
    </DashboardPage>
  );
}

function metric(value?: number) {
  return value === undefined ? "—" : value;
}

function issue(
  key: string,
  title: string,
  description: string,
  count: number | undefined,
  href: string,
  canLink: boolean,
): AdminHealthItem | null {
  if (count === undefined || count <= 0) {
    return null;
  }
  return { key, title, description, count, href: canLink ? href : undefined };
}
