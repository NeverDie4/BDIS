"use client";

import { useAuthStore } from "@/stores/auth-store";
import {
  DashboardMetric,
  DashboardPage,
  DashboardPanel,
} from "@/components/dashboard/DashboardPage";
import { Gauge, KeyRound, ShieldCheck } from "lucide-react";
import { Button, Space, Tag } from "antd";
import { useRouter } from "next/navigation";
import type { MenuItem } from "@/types/api";

export default function DashboardHomePage() {
  const user = useAuthStore((state) => state.user);
  const menus = useAuthStore((state) => state.menus);
  const router = useRouter();
  const quickMenus = flattenMenuRoutes(menus).slice(0, 6);

  return (
    <DashboardPage
      eyebrow="ADMINISTRATION OVERVIEW"
      title="后台管理总览"
      description="统一维护用户、角色、菜单权限和组织层级。所有管理操作均依据当前账号权限开放。"
      metrics={
        <>
          <DashboardMetric
            icon={ShieldCheck}
            label="当前用户"
            value={user?.realName || user?.username || "-"}
          />
          <DashboardMetric icon={KeyRound} label="角色数量" value={user?.roleCodes.length ?? 0} />
          <DashboardMetric icon={Gauge} label="可访问菜单" value={quickMenus.length} />
        </>
      }
    >
      <DashboardPanel
        title="快捷入口"
        description="入口由服务端菜单权限生成，只展示当前账号可访问的管理功能。"
      >
        <Space wrap size={[10, 10]}>
          {quickMenus.length ? (
            quickMenus.map((menu) => (
              <Button key={menu.routePath} onClick={() => router.push(menu.routePath!)}>
                {menu.menuName}
              </Button>
            ))
          ) : (
            <Tag>暂无其他管理入口</Tag>
          )}
        </Space>
      </DashboardPanel>
    </DashboardPage>
  );
}

function flattenMenuRoutes(menus: MenuItem[]): MenuItem[] {
  return menus.flatMap((menu) => [
    ...(menu.routePath && menu.routePath !== "/dashboard" ? [menu] : []),
    ...flattenMenuRoutes(menu.children || []),
  ]);
}
