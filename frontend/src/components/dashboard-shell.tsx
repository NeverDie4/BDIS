"use client";

import { apiDelete, apiGet, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { MenuItem } from "@/types/api";
import { isRegisteredRoutePath } from "@/config/routes";
import {
  Building2,
  Gauge,
  House,
  IdCard,
  KeyRound,
  LogOut,
  ClipboardList,
  Menu as MenuIcon,
  Network,
  ShieldCheck,
  Users,
} from "lucide-react";
import { Alert, App, Button, Layout, Menu, Spin } from "antd";
import type { MenuProps } from "antd";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import styles from "./dashboard-shell.module.css";
import { useSettingNamespace } from "@/hooks/settings/useSettingNamespace";
import type { AppearanceSettings } from "@/types/settings";

const { Header, Sider, Content } = Layout;

const iconMap: Record<string, React.ReactNode> = {
  dashboard: <Gauge size={16} />,
  shield: <ShieldCheck size={16} />,
  users: <Users size={16} />,
  "id-card": <IdCard size={16} />,
  key: <KeyRound size={16} />,
  building: <Building2 size={16} />,
  sitemap: <Network size={16} />,
  "clipboard-list": <ClipboardList size={16} />,
};

export function DashboardShell({ children }: { children: React.ReactNode }) {
  const { message } = App.useApp();
  const router = useRouter();
  const pathname = usePathname();
  const user = useAuthStore((state) => state.user);
  const menus = useAuthStore((state) => state.menus);
  const setMenus = useAuthStore((state) => state.setMenus);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string>();
  const [collapsed, setCollapsed] = useState(false);
  const [mobile, setMobile] = useState(false);
  const appearance = useSettingNamespace<AppearanceSettings>("appearance", Boolean(user));
  const sidebarMode = appearance.data?.values.sidebarMode ?? "auto";

  useEffect(() => {
    if (mobile) {
      setCollapsed(true);
      return;
    }
    setCollapsed(sidebarMode === "collapsed");
  }, [mobile, sidebarMode]);

  const loadMenus = useCallback(async () => {
    if (!user) {
      return;
    }
    setLoading(true);
    setLoadError(undefined);
    try {
      setMenus(await apiGet<MenuItem[]>("/me/menus"));
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        const nextError = getApiErrorMessage(error, "后台菜单加载失败");
        setLoadError(nextError);
        message.error(nextError);
      }
    } finally {
      setLoading(false);
    }
  }, [message, setMenus, user]);

  useEffect(() => {
    void loadMenus();
  }, [loadMenus]);

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
      <main className={styles.loading}>
        <Spin />
      </main>
    );
  }

  if (loadError) {
    return (
      <main className={styles.loading}>
        <Alert
          showIcon
          type="error"
          message="后台菜单加载失败"
          description={loadError}
          action={<Button onClick={() => void loadMenus()}>重新加载</Button>}
        />
      </main>
    );
  }

  return (
    <Layout className={styles.shell}>
      <Sider
        width={248}
        collapsedWidth={0}
        collapsed={collapsed}
        breakpoint="lg"
        className={styles.sider}
        trigger={null}
        onBreakpoint={(broken) => {
          setMobile(broken);
          setCollapsed(broken || sidebarMode === "collapsed");
        }}
      >
        <Link href="/dashboard" className={styles.brand}>
          <span className={styles.brandMark}>
            <ShieldCheck size={18} strokeWidth={1.7} />
          </span>
          <span className={styles.brandText}>
            <span className={styles.brandName}>本草研究院</span>
            <span className={styles.brandSub}>数字管理中心</span>
          </span>
        </Link>
        <Menu
          className={styles.menu}
          mode="inline"
          theme="light"
          selectedKeys={[pathname]}
          items={menuItems}
          onClick={({ key }) => {
            if (String(key).startsWith("/")) {
              router.push(String(key));
              if (mobile) {
                setCollapsed(true);
              }
            }
          }}
        />
      </Sider>
      <Layout className={styles.body}>
        <Header className={styles.header}>
          <div className={styles.headerLeft}>
            <Button
              aria-label="打开管理菜单"
              className={styles.menuButton}
              icon={<MenuIcon size={18} />}
              type="text"
              onClick={() => setCollapsed((value) => !value)}
            />
            <Link href="/" className={styles.portalLink}>
              <House size={16} />
              <span className={styles.portalLabel}>返回门户</span>
            </Link>
          </div>
          <div className={styles.headerRight}>
            <div className={styles.userInfo}>
              <span className={styles.userName}>{user?.realName || user?.username}</span>
              <span className={styles.userDivider} />
              <span className={styles.roleText}>
                {user?.roleCodes?.join(" / ") || "未分配角色"}
              </span>
            </div>
            <Button
              aria-label="退出登录"
              className={styles.logoutButton}
              icon={<LogOut size={15} />}
              type="text"
              onClick={logout}
            >
              退出
            </Button>
          </div>
        </Header>
        <Content className={styles.content}>{children}</Content>
      </Layout>
    </Layout>
  );
}

function buildMenuItems(menus: MenuItem[]): MenuProps["items"] {
  return menus.flatMap((menu) => {
    const children = menu.children?.length ? buildMenuItems(menu.children) : undefined;
    if (!children?.length && (!menu.routePath || !isRegisteredRoutePath(menu.routePath))) {
      return [];
    }
    return [
      {
        key: children?.length ? menu.menuCode : menu.routePath || menu.menuCode,
        icon: menu.icon ? iconMap[menu.icon] : undefined,
        label: menu.menuName,
        children,
      },
    ];
  });
}
