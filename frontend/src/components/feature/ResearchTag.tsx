"use client";

import { Tag } from "antd";
import styles from "./ResearchTag.module.css";

export type ResearchTagType = "course" | "project" | "training" | "achievement" | "paper" | "resource";
export type ResearchTagStatus = "draft" | "active" | "done" | "archived";

type ResearchTagProps = {
  type: ResearchTagType;
  status?: ResearchTagStatus;
  label?: string;
  className?: string;
};

const TYPE_LABELS: Record<ResearchTagType, string> = {
  course: "课程",
  project: "课题",
  training: "培训",
  achievement: "成果",
  paper: "论文",
  resource: "资料",
};

export function ResearchTag({ type, status = "active", label, className }: ResearchTagProps) {
  return (
    <Tag className={`${styles.tag} ${styles[type]} ${styles[status]} ${className ?? ""}`} bordered={false}>
      {label ?? TYPE_LABELS[type]}
    </Tag>
  );
}
