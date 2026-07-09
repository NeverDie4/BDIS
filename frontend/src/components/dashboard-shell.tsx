"use client";

import { apiDelete, apiGet, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { CurrentUser, MenuItem } from "@/types/api";
import {
  Building2,
  Gauge,
  IdCard,
  KeyRound,
  LogOut,
  Network,
  ShieldCheck,
  Users,
} from "lucide-react";
import { Button, Layout, Menu, Space, Spin, Typography } from "antd";
import type { MenuProps } from "antd";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

const { Header, Sider, Content } = Layout;

const iconMap: Record<string, React.ReactNode> = {
  dashboard: <Gauge size={16} />,
  shield: <ShieldCheck size={16} />,
  users: <Users size={16} />,
  "id-card": <IdCard size={16} />,
  key: <KeyRound size={16} />,
  building: <Building2 size={16} />,
  sitemap: <Network size={16} />,
};

export function DashboardShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  const menus = useAuthStore((state) => state.menus);
  const setUser = useAuthStore((state) => state.setUser);
  const setMenus = useAuthStore((state) => state.setMenus);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) {
      router.replace("/login");
      return;
    }
    Promise.all([apiGet<CurrentUser>("/auth/me"), apiGet<MenuItem[]>("/me/menus")])
      .then(([currentUser, menuTree]) => {
        setUser(currentUser);
        setMenus(menuTree);
      })
      .catch((error) => {
        if (!isAuthRedirectError(error)) {
          clearAuth();
          router.replace("/login");
        }
      })
      .finally(() => setLoading(false));
  }, [clearAuth, router, setMenus, setUser, token]);

  const menuItems = useMemo<MenuProps["items"]>(() => buildMenuItems(menus), [menus]);

  async function logout() {
    try {
      await apiDelete<void>("/auth/sessions/current");
    } finally {
      clearAuth();
      router.replace("/login");
    }
  }

  if (loading) {
    return (
      <main className="full-center">
        <Spin />
      </main>
    );
  }

  return (
    <Layout className="dashboard-shell">
      <Sider width={232} className="dashboard-sider">
        <div className="brand-block">
          <Typography.Text className="brand-eyebrow">BDIS</Typography.Text>
          <Typography.Title level={4}>权限后台</Typography.Title>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[pathname]}
          items={menuItems}
          onClick={({ key }) => {
            if (String(key).startsWith("/")) {
              router.push(String(key));
            }
          }}
        />
      </Sider>
      <Layout>
        <Header className="dashboard-header">
          <Typography.Text>{user?.realName || user?.username}</Typography.Text>
          <Space>
            <Typography.Text type="secondary">
              {user?.roleCodes?.join(" / ") || "未分配角色"}
            </Typography.Text>
            <Button icon={<LogOut size={16} />} onClick={logout}>
              退出
            </Button>
          </Space>
        </Header>
        <Content className="dashboard-content">{children}</Content>
      </Layout>
    </Layout>
  );
}

function buildMenuItems(menus: MenuItem[]): MenuProps["items"] {
  return menus.map((menu) => {
    const children = menu.children?.length ? buildMenuItems(menu.children) : undefined;
    return {
      key: children ? menu.menuCode : menu.routePath || menu.menuCode,
      icon: menu.icon ? iconMap[menu.icon] : undefined,
      label: menu.menuName,
      children,
    };
  });
}
