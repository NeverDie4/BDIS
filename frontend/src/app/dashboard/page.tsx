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
import { CircleAlert, KeyRound, MonitorCheck, Users } from "lucide-react";

export default function DashboardHomePage() {
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const summary = useAdminDashboardSummary();
  const data = summary.data;

  const risks: AdminHealthItem[] = [
    risk(
      "disabled-users",
      "停用账号",
      "这些账号当前不能登录，可核对是否仍应保留。",
      data?.disabledUsers,
      "/dashboard/users",
      hasPermission("auth:user:view"),
    ),
    risk(
      "password-change",
      "待首次改密账号",
      "账号必须完成密码修改后才能访问业务功能。",
      data?.mustChangePasswordUsers,
      "/dashboard/users",
      hasPermission("auth:user:view"),
    ),
    risk(
      "failed-login",
      "近 24 小时登录失败",
      "建议在操作审计中核对来源账号、IP 和失败原因。",
      data?.failedLogins24h,
      "/dashboard/audit?tab=login",
      hasPermission("audit:login:view"),
    ),
    risk(
      "failed-operation",
      "近 24 小时失败操作",
      "集中检查权限拒绝和业务处理失败记录。",
      data?.failedOperations24h,
      "/dashboard/audit?tab=operation",
      hasPermission("audit:operation:view"),
    ),
    risk(
      "stale-session",
      "未归档的过期会话",
      "记录已过有效期但状态仍为 active，会在会话访问时自动收敛。",
      data?.staleActiveSessions,
      undefined,
      false,
    ),
  ].filter((item): item is AdminHealthItem => item !== null);

  return (
    <DashboardPage
      eyebrow="ADMINISTRATION OVERVIEW"
      title="管理态势总览"
      description="汇总账号、会话和审计风险。完整功能导航由左侧菜单承担，本页只提供需要关注的状态与处理线索。"
      metrics={
        <>
          <DashboardMetric icon={Users} label="系统账号" value={metric(data?.totalUsers)} />
          <DashboardMetric
            icon={MonitorCheck}
            label="有效会话"
            value={metric(data?.activeSessions)}
          />
          <DashboardMetric
            icon={KeyRound}
            label="待首次改密"
            value={metric(data?.mustChangePasswordUsers)}
          />
          <DashboardMetric
            icon={CircleAlert}
            label="24 小时失败操作"
            value={metric(data?.failedOperations24h)}
          />
        </>
      }
    >
      {summary.isLoading ? <Skeleton active paragraph={{ rows: 5 }} /> : null}
      {summary.isError ? (
        <Alert
          showIcon
          type="error"
          message="管理态势加载失败"
          description={getApiErrorMessage(summary.error, "无法读取管理态势")}
          action={<Button onClick={() => void summary.refetch()}>重新加载</Button>}
        />
      ) : null}
      {summary.isSuccess ? (
        <DashboardPanel
          title="待关注事项"
          description="只展示当前账号有权查看且数量大于零的异常项。"
          flush
        >
          <AdminHealthList items={risks} />
        </DashboardPanel>
      ) : null}
    </DashboardPage>
  );
}

function metric(value?: number) {
  return value === undefined ? "—" : value;
}

function risk(
  key: string,
  title: string,
  description: string,
  count: number | undefined,
  href: string | undefined,
  canLink: boolean,
): AdminHealthItem | null {
  if (count === undefined || count <= 0) {
    return null;
  }
  return { key, title, description, count, href: canLink ? href : undefined };
}
