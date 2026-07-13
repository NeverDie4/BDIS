"use client";

import { Space, Typography } from "antd";
import styles from "./ActionToolbar.module.css";

type ActionToolbarProps = {
  title?: string;
  description?: string;
  actions?: React.ReactNode;
  children?: React.ReactNode;
  className?: string;
  variant?: "default" | "compact";
};

export function ActionToolbar({ title, description, actions, children, className, variant = "default" }: ActionToolbarProps) {
  return (
    <div className={`${styles.toolbar} ${styles[variant]} ${className ?? ""}`}>
      <div className={styles.copy}>
        {title ? <Typography.Title level={2}>{title}</Typography.Title> : null}
        {description ? <Typography.Paragraph>{description}</Typography.Paragraph> : null}
        {children}
      </div>
      {actions ? (
        <Space className={styles.actions} size={10} wrap>
          {actions}
        </Space>
      ) : null}
    </div>
  );
}
