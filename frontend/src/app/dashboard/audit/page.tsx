"use client";

import { Tabs, Typography } from "antd";
import {
  DataSyncAuditPanel,
  FileAccessAuditPanel,
  LoginAuditPanel,
  OperationAuditPanel,
} from "@/components/audit/AuditPanels";
import { DashboardPage, DashboardPanel } from "@/components/dashboard/DashboardPage";
import { useAuthStore } from "@/stores/auth-store";
import { useState } from "react";

export default function AuditPage() {
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [requestedTab, setRequestedTab] = useState(() => {
    if (typeof window === "undefined") return "operation";
    return new URLSearchParams(window.location.search).get("tab") || "operation";
  });
  const items = [
    hasPermission("audit:operation:view")
      ? { key: "operation", label: "操作日志", children: <OperationAuditPanel /> }
      : null,
    hasPermission("audit:login:view")
      ? { key: "login", label: "登录日志", children: <LoginAuditPanel /> }
      : null,
    hasPermission("audit:file:view")
      ? { key: "file", label: "文件访问日志", children: <FileAccessAuditPanel /> }
      : null,
    hasPermission("audit:data-sync:view")
      ? { key: "data-sync", label: "数据同步日志", children: <DataSyncAuditPanel /> }
      : null,
  ].filter((item): item is NonNullable<typeof item> => item !== null);
  const activeKey = items.some((item) => item.key === requestedTab) ? requestedTab : items[0]?.key;

  return (
    <DashboardPage
      eyebrow="OPERATION AUDIT"
      title="操作审计"
      description="集中查询系统操作、账号登录和文件访问记录。日志类型依据审计权限分别开放。"
    >
      {items.length ? (
        <Tabs
          activeKey={activeKey}
          items={items}
          destroyOnHidden
          onChange={(tab) => {
            setRequestedTab(tab);
            const url = new URL(window.location.href);
            url.searchParams.set("tab", tab);
            window.history.replaceState(window.history.state, "", `${url.pathname}${url.search}`);
          }}
        />
      ) : (
        <DashboardPanel>
          <Typography.Text type="secondary">当前账号没有可查询的审计日志类型。</Typography.Text>
        </DashboardPanel>
      )}
    </DashboardPage>
  );
}
