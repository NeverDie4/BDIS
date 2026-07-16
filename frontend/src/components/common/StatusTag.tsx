"use client";

import { Tag } from "antd";
import styles from "./StatusTag.module.css";

export type StatusType =
  | "normal"
  | "disabled"
  | "draft"
  | "published"
  | "pending"
  | "approved"
  | "rejected"
  | "archived";

type StatusTagProps = {
  status: StatusType;
  label?: string;
  className?: string;
};

const STATUS_LABELS: Record<StatusType, string> = {
  normal: "正常",
  disabled: "停用",
  draft: "草稿",
  published: "已发布",
  pending: "待处理",
  approved: "已通过",
  rejected: "已驳回",
  archived: "已归档",
};

export function StatusTag({ status, label, className }: StatusTagProps) {
  return (
    <Tag className={`${styles.tag} ${styles[status]} ${className ?? ""}`} bordered={false}>
      {label ?? STATUS_LABELS[status]}
    </Tag>
  );
}
