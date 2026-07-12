"use client";

import { App, Avatar, Dropdown } from "antd";
import type { MenuProps } from "antd";
import { LogOut, Settings, UserRound } from "lucide-react";
import { useRouter } from "next/navigation";
import { apiDelete } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "./UserMenu.module.css";

export function UserMenu() {
  const { message } = App.useApp();
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const displayName = user?.realName || user?.username || "当前用户";
  const roleText = user?.roleCodes.length ? user.roleCodes.join(" / ") : "未分配角色";

  const items: MenuProps["items"] = [
    {
      key: "profile",
      icon: <UserRound size={16} />,
      label: "个人主页",
    },
    {
      key: "settings",
      icon: <Settings size={16} />,
      label: "个人设置",
    },
    { type: "divider" },
    {
      key: "logout",
      danger: true,
      icon: <LogOut size={16} />,
      label: "退出登录",
    },
  ];

  async function onClick({ key }: { key: string }) {
    if (key === "profile") {
      router.push("/profile");
      return;
    }
    if (key === "settings") {
      router.push("/settings");
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
    <Dropdown
      menu={{ items, onClick, className: styles.menu }}
      placement="bottomRight"
      trigger={["click"]}
      popupRender={(menu) => (
        <div className={styles.popup}>
          <div className={styles.identity}>
            <Avatar className={styles.identityAvatar} icon={<UserRound size={19} />} size={42} />
            <div className={styles.identityText}>
              <strong>{displayName}</strong>
              <span>{user?.username || "-"}</span>
            </div>
          </div>
          <div className={styles.roleLine}>
            <span>当前角色</span>
            <strong>{roleText}</strong>
          </div>
          {menu}
        </div>
      )}
    >
      <button aria-label="打开用户菜单" className={styles.avatarButton} type="button">
        <Avatar icon={<UserRound size={18} />} size={34} />
      </button>
    </Dropdown>
  );
}
