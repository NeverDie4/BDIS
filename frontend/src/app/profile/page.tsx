"use client";

import { App, Button, Card, Collapse, Descriptions, Tag, Typography } from "antd";
import { ArrowRight, Settings, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { InfoCard } from "@/components/common/InfoCard";
import { MetricCard } from "@/components/common/MetricCard";
import { UserAvatar } from "@/components/common/UserAvatar";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { authAdminRoutes } from "@/config/routes/auth-admin";
import { hasUserPermission } from "@/config/routes/types";
import { apiGet, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { CurrentUser } from "@/types/api";
import pageStyles from "./profile.module.css";
import styles from "@/styles/mockPages.module.css";

const permissionAreas = [
  { prefixes: ["auth:"], label: "账号与权限" },
  { prefixes: ["dashboard:"], label: "业务看板" },
  { prefixes: ["herb:"], label: "药材与图谱" },
  { prefixes: ["map:"], label: "地图与基地" },
  { prefixes: ["growth:"], label: "生长采集" },
  { prefixes: ["file:"], label: "文件资源" },
  { prefixes: ["dictionary:"], label: "数据字典" },
  { prefixes: ["soap:"], label: "数据交换" },
  { prefixes: ["audit:"], label: "操作审计" },
];

const managementDescriptions: Record<string, string> = {
  "/dashboard/auth": "进入权限管理中心。",
  "/dashboard/users": "维护用户账号及其角色关系。",
  "/dashboard/roles": "维护角色、权限和数据范围。",
  "/dashboard/permissions": "维护菜单结构和权限点。",
  "/dashboard/organizations": "维护机构主体信息。",
  "/dashboard/departments": "维护部门层级与归属。",
  "/dashboard/audit": "查询系统操作、登录和文件访问日志。",
};

export default function ProfilePage() {
  const { message } = App.useApp();
  const storedUser = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(storedUser);

  const load = useCallback(async () => {
    try {
      const user = await apiGet<CurrentUser>("/auth/me");
      setCurrentUser(user);
      setUser(user);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, "个人信息加载失败"));
      }
    }
  }, [message, setUser]);

  useEffect(() => {
    void load();
  }, [load]);

  const permissionSummary = useMemo(() => {
    if (!currentUser) return [];
    return permissionAreas
      .filter((area) =>
        area.prefixes.some((prefix) =>
          currentUser.permissions.some(
            (permission) => permission === "*" || permission.startsWith(prefix),
          ),
        ),
      )
      .map((area) => area.label);
  }, [currentUser]);

  const managementEntries = useMemo(
    () =>
      authAdminRoutes.filter(
        (route) =>
          route.path !== "/dashboard" &&
          Boolean(managementDescriptions[route.path]) &&
          hasUserPermission(currentUser, route.permission),
      ),
    [currentUser],
  );

  const canInspectPermissionCodes = Boolean(
    currentUser?.roleCodes.some((role) => role === "ADMIN" || role === "AUDITOR"),
  );

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="PERSONAL DESK"
          title="个人主页"
          subtitle="查看当前账号身份、功能权限与可访问的管理入口。"
        />

        <section className={pageStyles.overviewGrid}>
          <Card className={styles.panel} variant="borderless">
            <div className={pageStyles.identityHeader}>
              <UserAvatar avatarUrl={currentUser?.avatarUrl} iconSize={30} size={72} />
              <div className={pageStyles.identityTitle}>
                <Typography.Title level={3}>
                  {currentUser?.realName || currentUser?.username || "当前用户"}
                </Typography.Title>
                <Typography.Text type="secondary">{currentUser?.username || "-"}</Typography.Text>
              </div>
              <Link href="/settings">
                <Button icon={<Settings size={16} />}>个人设置</Button>
              </Link>
            </div>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="组织 ID">
                {currentUser?.organizationId || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="部门 ID">
                {currentUser?.departmentId || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="角色">
                {currentUser?.roleCodes.length
                  ? currentUser.roleCodes.map((role) => <Tag key={role}>{role}</Tag>)
                  : "-"}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <div className={pageStyles.summaryGrid}>
            <MetricCard
              description="当前账号已分配的角色。"
              title="角色"
              value={currentUser?.roleCodes.length ?? 0}
            />
            <MetricCard
              description="根据权限归纳的可用功能域。"
              title="功能域"
              value={permissionSummary.length}
            />
          </div>
        </section>

        <section className={pageStyles.detailsGrid}>
          <InfoCard title="当前权限">
            <div className={pageStyles.permissionContent}>
              <Typography.Paragraph type="secondary">
                以下内容按当前账号权限归纳，具体操作仍由页面和接口权限共同校验。
              </Typography.Paragraph>
              <div className={pageStyles.permissionTags}>
                {permissionSummary.length ? (
                  permissionSummary.map((area) => (
                    <Tag className={pageStyles.permissionTag} key={area}>
                      <ShieldCheck size={14} />
                      {area}
                    </Tag>
                  ))
                ) : (
                  <Typography.Text type="secondary">暂无可用功能权限</Typography.Text>
                )}
              </div>
              {canInspectPermissionCodes ? (
                <Collapse
                  ghost
                  items={[
                    {
                      key: "permission-codes",
                      label: "查看技术权限编码",
                      children: (
                        <div className={pageStyles.codeTags}>
                          {currentUser?.permissions.map((permission) => (
                            <Tag key={permission}>{permission}</Tag>
                          ))}
                        </div>
                      ),
                    },
                  ]}
                />
              ) : null}
            </div>
          </InfoCard>

          {managementEntries.length ? (
            <InfoCard title="管理入口">
              <div className={pageStyles.managementList}>
                {managementEntries.map((route) => (
                  <Link className={pageStyles.managementEntry} href={route.path} key={route.path}>
                    <span>
                      <strong>{route.title}</strong>
                      <small>{managementDescriptions[route.path]}</small>
                    </span>
                    <ArrowRight size={17} />
                  </Link>
                ))}
              </div>
            </InfoCard>
          ) : null}
        </section>
      </div>
    </SiteLayout>
  );
}
