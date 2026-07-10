"use client";

import { Drawer } from "antd";
import type { DrawerProps } from "antd";
import styles from "./DetailDrawer.module.css";

type DetailDrawerProps = {
  title: React.ReactNode;
  open: boolean;
  onClose: DrawerProps["onClose"];
  width?: DrawerProps["width"];
  extra?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
};

export function DetailDrawer({
  title,
  open,
  onClose,
  width = 460,
  extra,
  children,
  className,
}: DetailDrawerProps) {
  return (
    <Drawer
      className={`${styles.drawer} ${className ?? ""}`}
      extra={extra}
      open={open}
      title={title}
      width={width}
      onClose={onClose}
    >
      <div className={styles.content}>{children}</div>
    </Drawer>
  );
}
