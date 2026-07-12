"use client";

import { Badge, Button, Drawer, Space, Tooltip, Typography } from "antd";
import { BellOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { getPortalNavigationRoutes } from "@/config/routes";
import { useAuthStore } from "@/stores/auth-store";
import { UserMenu } from "./UserMenu";
import styles from "./HeaderNav.module.css";

const NOTICES = [
  {
    title: "采集记录待完善",
    description: "生长数据模块将接入待审核、待补充图片和定位异常提醒。",
  },
  {
    title: "标本资源归档",
    description: "文件资源、课程资料和申报附件后续会统一汇入资源池。",
  },
  {
    title: "权限菜单预留",
    description: "当前导航为门户基础版，后续按登录用户权限动态裁剪入口。",
  },
];

function isActivePath(pathname: string, href: string) {
  if (href === "/") {
    return pathname === "/";
  }

  return pathname === href || pathname.startsWith(`${href}/`);
}

export function HeaderNav() {
  const pathname = usePathname();
  const [notificationOpen, setNotificationOpen] = useState(false);
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
        <Tooltip title="通知">
          <Badge dot offset={[-4, 4]}>
            <Button
              aria-label="打开通知"
              className={styles.iconButton}
              icon={<BellOutlined />}
              shape="circle"
              type="text"
              onClick={() => setNotificationOpen(true)}
            />
          </Badge>
        </Tooltip>
        <UserMenu />
      </Space>

      <Drawer
        title="馆内通知"
        open={notificationOpen}
        onClose={() => setNotificationOpen(false)}
        width={360}
        classNames={{ body: styles.drawerBody }}
      >
        <div className={styles.noticeList}>
          {NOTICES.map((notice) => (
            <article className={styles.noticeItem} key={notice.title}>
              <Typography.Text strong>{notice.title}</Typography.Text>
              <Typography.Paragraph type="secondary">{notice.description}</Typography.Paragraph>
            </article>
          ))}
        </div>
      </Drawer>
    </header>
  );
}
