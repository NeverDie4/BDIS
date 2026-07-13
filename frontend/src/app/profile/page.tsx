"use client";

import { UserOutlined } from "@ant-design/icons";
import { App, Avatar, Button, Card, Descriptions, List, Tabs, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { DataTable } from "@/components/common/DataTable";
import { InfoCard } from "@/components/common/InfoCard";
import { MetricCard } from "@/components/common/MetricCard";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { apiGet, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "@/styles/mockPages.module.css";
import type { CurrentUser, PageResult } from "@/types/api";

type AuditLog = {
  id: number;
  operatorName?: string;
  operationModule?: string;
  operationType?: string;
  bizType?: string;
  operationResult?: string;
  operationTime?: string;
};

type LoginLog = {
  id: number;
  username?: string;
  loginResult?: string;
  failureReason?: string;
  ipAddress?: string;
  loggedInAt?: string;
};

type FileAccessLog = {
  id: number;
  fileId?: number;
  operatorName?: string;
  accessType?: string;
  accessResult?: string;
  operationTime?: string;
};

export default function ProfilePage() {
  const { message } = App.useApp();
  const storedUser = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(storedUser);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loginLogs, setLoginLogs] = useState<LoginLog[]>([]);
  const [fileLogs, setFileLogs] = useState<FileAccessLog[]>([]);
  const canViewAudit = Boolean(currentUser?.permissions.includes("audit:log:view") || currentUser?.permissions.includes("*"));

  const load = useCallback(async () => {
    try {
      const user = await apiGet<CurrentUser>("/auth/me");
      setCurrentUser(user);
      setUser(user);
      if (user.permissions.includes("audit:log:view") || user.permissions.includes("*")) {
        const [operations, logins, files] = await Promise.all([
          apiGet<PageResult<AuditLog>>("/audit-logs", { page: 1, size: 10 }),
          apiGet<PageResult<LoginLog>>("/login-logs", { page: 1, size: 10 }),
          apiGet<PageResult<FileAccessLog>>("/file-access-logs", { page: 1, size: 10 }),
        ]);
        setAuditLogs(operations.records);
        setLoginLogs(logins.records);
        setFileLogs(files.records);
      }
    } catch (error) {
      if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "个人信息加载失败"));
    }
  }, [message, setUser]);

  useEffect(() => { void load(); }, [load]);

  const auditColumns = useMemo<TableProps<AuditLog>["columns"]>(() => [
    { title: "操作人", dataIndex: "operatorName", render: (value) => value || "系统" },
    { title: "模块", dataIndex: "operationModule" },
    { title: "动作", dataIndex: "operationType" },
    { title: "业务对象", dataIndex: "bizType" },
    { title: "结果", dataIndex: "operationResult", render: (value) => <Tag color={value === "SUCCESS" ? "green" : "red"}>{value}</Tag> },
    { title: "时间", dataIndex: "operationTime", render: (value) => value ? new Date(value).toLocaleString() : "-" },
  ], []);

  const loginColumns = useMemo<TableProps<LoginLog>["columns"]>(() => [
    { title: "账号", dataIndex: "username" },
    { title: "结果", dataIndex: "loginResult", render: (value) => <Tag color={value === "SUCCESS" ? "green" : "red"}>{value}</Tag> },
    { title: "失败原因", dataIndex: "failureReason", render: (value) => value || "-" },
    { title: "IP", dataIndex: "ipAddress" },
    { title: "时间", dataIndex: "loggedInAt", render: (value) => value ? new Date(value).toLocaleString() : "-" },
  ], []);

  const fileColumns = useMemo<TableProps<FileAccessLog>["columns"]>(() => [
    { title: "用户", dataIndex: "operatorName", render: (value) => value || "系统" },
    { title: "文件 ID", dataIndex: "fileId" },
    { title: "操作", dataIndex: "accessType" },
    { title: "结果", dataIndex: "accessResult" },
    { title: "时间", dataIndex: "operationTime", render: (value) => value ? new Date(value).toLocaleString() : "-" },
  ], []);

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner sealText="PERSONAL DESK" title="个人主页" subtitle="当前用户、角色、权限和审计信息均由登录会话与权限服务实时提供。" />
        <section className={styles.contentGrid}>
          <Card className={styles.panel} variant="borderless">
            <div className={styles.panelBody}>
              <Avatar icon={<UserOutlined />} size={72} />
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="姓名">{currentUser?.realName || "-"}</Descriptions.Item>
                <Descriptions.Item label="账号">{currentUser?.username || "-"}</Descriptions.Item>
                <Descriptions.Item label="组织 ID">{currentUser?.organizationId || "-"}</Descriptions.Item>
                <Descriptions.Item label="部门 ID">{currentUser?.departmentId || "-"}</Descriptions.Item>
                <Descriptions.Item label="角色">{currentUser?.roleCodes.join("、") || "-"}</Descriptions.Item>
              </Descriptions>
            </div>
          </Card>
          <div className={styles.metricGrid}>
            <MetricCard description="当前会话包含的角色。" title="角色数" value={currentUser?.roleCodes.length ?? 0} />
            <MetricCard description="当前用户已获授权限。" title="权限数" value={currentUser?.permissions.length ?? 0} />
            <MetricCard description="最近加载的操作日志。" title="操作日志" value={auditLogs.length} />
            <MetricCard description="最近加载的文件访问记录。" title="文件日志" value={fileLogs.length} />
          </div>
        </section>
        <section className={styles.contentGrid}>
          <InfoCard title="当前权限">
            <List
              dataSource={currentUser?.permissions || []}
              locale={{ emptyText: "暂无权限数据" }}
              renderItem={(permission) => <List.Item><Tag>{permission}</Tag></List.Item>}
            />
          </InfoCard>
          <InfoCard title="管理入口">
            <div className={styles.sectionStack}>
              <Typography.Paragraph type="secondary">用户、角色、组织和权限的维护集中在后台管理，避免个人页重复提供管理入口。</Typography.Paragraph>
              <Link href="/dashboard"><Button type="primary">进入后台管理</Button></Link>
            </div>
          </InfoCard>
        </section>
        {canViewAudit ? <Tabs items={[
          { key: "operation", label: "操作日志", children: <DataTable<AuditLog> columns={auditColumns} dataSource={auditLogs} pagination={false} rowKey="id" /> },
          { key: "login", label: "登录日志", children: <DataTable<LoginLog> columns={loginColumns} dataSource={loginLogs} pagination={false} rowKey="id" /> },
          { key: "file", label: "文件访问日志", children: <DataTable<FileAccessLog> columns={fileColumns} dataSource={fileLogs} pagination={false} rowKey="id" /> },
        ]} /> : <InfoCard title="审计日志"><Typography.Text type="secondary">当前角色没有审计日志查看权限。</Typography.Text></InfoCard>}
      </div>
    </SiteLayout>
  );
}
