"use client";

import { UserOutlined } from "@ant-design/icons";
import { App, Avatar, Dropdown } from "antd";
import type { MenuProps } from "antd";
import { useRouter } from "next/navigation";
import { apiDelete } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "./UserMenu.module.css";

export function UserMenu() {
  const { message } = App.useApp();
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  const items: MenuProps["items"] = [
    { key: "profile", label: user?.realName || user?.username || "个人主页" },
    { type: "divider" },
    { key: "logout", danger: true, label: "退出登录" },
  ];

  async function onClick({ key }: { key: string }) {
    if (key === "profile") {
      router.push("/profile");
      return;
    }
    try {
      await apiDelete<void>("/auth/sessions/current");
    } catch {
      message.warning("服务端会话注销失败，已清除本地登录状态");
    } finally {
      clearAuth();
      router.push("/login");
    }
  }

  return (
    <Dropdown menu={{ items, onClick }} placement="bottomRight" trigger={["click"]}>
      <button aria-label="打开用户菜单" className={styles.avatarButton} type="button">
        <Avatar icon={<UserOutlined />} size={34} />
      </button>
    </Dropdown>
  );
}
