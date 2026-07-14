import { Tag } from "antd";
import type { TeachingStatus } from "./types";
import styles from "./teaching.module.css";

const statusMeta: Record<TeachingStatus, { label: string; className: string }> = {
  published: { label: "已发布", className: styles.statusPublished },
  draft: { label: "草稿", className: styles.statusDraft },
  offline: { label: "已下架", className: styles.statusOffline },
  ongoing: { label: "进行中", className: styles.statusOngoing },
  completed: { label: "已结题", className: styles.statusCompleted },
  applying: { label: "申报中", className: styles.statusApplying },
};

type TeachingStatusTagProps = {
  status: TeachingStatus;
};

export function TeachingStatusTag({ status }: TeachingStatusTagProps) {
  const meta = statusMeta[status];
  return <Tag className={`${styles.statusTag} ${meta.className}`}>{meta.label}</Tag>;
}
