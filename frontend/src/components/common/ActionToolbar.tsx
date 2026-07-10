"use client";

import { Space, Typography } from "antd";
import styles from "./ActionToolbar.module.css";

type ActionToolbarProps = {
  title?: string;
  description?: string;
  actions?: React.ReactNode;
  children?: React.ReactNode;
  className?: string;
};

export function ActionToolbar({ title, description, actions, children, className }: ActionToolbarProps) {
  return (
    <div className={`${styles.toolbar} ${className ?? ""}`}>
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
