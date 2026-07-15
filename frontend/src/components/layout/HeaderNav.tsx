"use client";

import { LoginOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Button, Space } from "antd";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { getPortalNavigationRoutes } from "@/config/routes";
import { useAuthStore } from "@/stores/auth-store";
import { UserMenu } from "./UserMenu";
import styles from "./HeaderNav.module.css";

function isActivePath(pathname: string, href: string) {
  if (href === "/") {
    return pathname === "/";
  }

  return pathname === href || pathname.startsWith(`${href}/`);
}

export function HeaderNav() {
  const pathname = usePathname();
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);
  const visibleNavItems = getPortalNavigationRoutes(status, user);

  return (
    <header className={styles.header}>
      <Link href="/" className={styles.brand} aria-label="返回首页">
        <span className={styles.brandMark}>
          <SafetyCertificateOutlined />
        </span>
        <span>
          <span className={styles.brandName}>本草研究院标本馆</span>
          <span className={styles.brandSub}>Herbarium Research Hall</span>
        </span>
      </Link>

      <nav className={styles.nav} aria-label="主导航">
        {visibleNavItems.map((item) => (
          <Link
            key={item.path}
            className={`${styles.navItem} ${
              isActivePath(pathname, item.path) ? styles.navItemActive : ""
            }`}
            href={item.path}
          >
            {item.navLabel}
          </Link>
        ))}
      </nav>

      <Space className={styles.actions} size={8}>
        {status === "authenticated" ? (
          <UserMenu />
        ) : status === "unknown" ? (
          <Button aria-label="正在确认登录状态" loading shape="circle" type="text" />
        ) : (
          <Link href="/login">
            <Button icon={<LoginOutlined />}>登录</Button>
          </Link>
        )}
      </Space>
    </header>
  );
}
