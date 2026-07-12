"use client";

import {
  DashboardMetric,
  DashboardPage,
  DashboardPanel,
} from "@/components/dashboard/DashboardPage";
import { useAuthStore } from "@/stores/auth-store";
import {
  ArrowRight,
  Building2,
  ClipboardList,
  IdCard,
  KeyRound,
  Network,
  ShieldCheck,
  Users,
} from "lucide-react";
import Link from "next/link";
import styles from "./page.module.css";

const adminEntries = [
  {
    path: "/dashboard/users",
    permission: "auth:user:view",
    title: "用户管理",
    description: "维护账号资料、启停状态、组织归属与角色关系。",
    icon: Users,
  },
  {
    path: "/dashboard/roles",
    permission: "auth:role:view",
    title: "角色管理",
    description: "维护业务角色、数据范围与角色权限配置。",
    icon: IdCard,
  },
  {
    path: "/dashboard/permissions",
    permission: "auth:permission:view",
    title: "菜单权限",
    description: "维护后台菜单结构、权限点和接口授权信息。",
    icon: KeyRound,
  },
  {
    path: "/dashboard/organizations",
    permission: "auth:organization:view",
    title: "组织机构",
    description: "维护机构主体、联系方式和启停状态。",
    icon: Building2,
  },
  {
    path: "/dashboard/departments",
    permission: "auth:department:view",
    title: "部门管理",
    description: "维护部门层级、所属机构和排序状态。",
    icon: Network,
  },
  {
    path: "/dashboard/audit",
    permission: "audit:log:view",
    title: "操作审计",
    description: "查询后台操作、个人设置和认证会话的执行结果。",
    icon: ClipboardList,
  },
];

export default function AuthorizationCenterPage() {
  const user = useAuthStore((state) => state.user);
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const availableEntries = adminEntries.filter((entry) => hasPermission(entry.permission));

  return (
    <DashboardPage
      eyebrow="AUTHORIZATION CENTER"
      title="权限后台"
      description="集中进入账号、角色、菜单权限和组织层级管理。入口依据当前账号权限动态开放。"
      metrics={
        <>
          <DashboardMetric icon={ShieldCheck} label="可访问模块" value={availableEntries.length} />
          <DashboardMetric icon={IdCard} label="当前角色" value={user?.roleCodes.length ?? 0} />
          <DashboardMetric
            icon={KeyRound}
            label="持有权限"
            value={user?.permissions.includes("*") ? "全部" : (user?.permissions.length ?? 0)}
          />
        </>
      }
    >
      <DashboardPanel
        title="管理入口"
        description="这里只展示当前账号具有页面查看权限的管理模块。"
        flush
      >
        <div className={styles.entryList}>
          {availableEntries.map(({ path, title, description, icon: Icon }) => (
            <Link key={path} href={path} className={styles.entry}>
              <span className={styles.entryIcon}>
                <Icon size={19} />
              </span>
              <span className={styles.entryText}>
                <strong>{title}</strong>
                <span>{description}</span>
              </span>
              <ArrowRight className={styles.entryArrow} size={18} />
            </Link>
          ))}
        </div>
      </DashboardPanel>
    </DashboardPage>
  );
}
