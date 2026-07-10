"use client";

import { Avatar, Tooltip } from "antd";
import { UserOutlined } from "@ant-design/icons";
import { useRouter } from "next/navigation";
import styles from "./UserMenu.module.css";

export function UserMenu() {
  const router = useRouter();

  return (
    <Tooltip title="个人主页">
      <button
        aria-label="进入个人主页"
        className={styles.avatarButton}
        type="button"
        onClick={() => router.push("/profile")}
      >
        <Avatar icon={<UserOutlined />} size={34} />
      </button>
    </Tooltip>
  );
}
