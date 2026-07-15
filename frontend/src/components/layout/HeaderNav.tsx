"use client";

import { LoginOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Button, Space } from "antd";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef } from "react";
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
  const headerRef = useRef<HTMLElement>(null);
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);
  const visibleNavItems = getPortalNavigationRoutes(status, user);

  useEffect(() => {
    const header = headerRef.current;
    if (!header) {
      return;
    }

    const updateHeaderHeight = () => {
      document.documentElement.style.setProperty(
        "--site-header-height",
        `${Math.ceil(header.getBoundingClientRect().height)}px`,
      );
    };

    updateHeaderHeight();
    const observer = new ResizeObserver(updateHeaderHeight);
    observer.observe(header);

    return () => {
      observer.disconnect();
      document.documentElement.style.removeProperty("--site-header-height");
    };
  }, []);

  return (
    <header className={styles.header} ref={headerRef}>
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
