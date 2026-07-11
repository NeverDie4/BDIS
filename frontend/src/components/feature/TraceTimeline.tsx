"use client";

import { Typography } from "antd";
import styles from "./TraceTimeline.module.css";

export type TraceStatus = "recorded" | "pending" | "approved" | "archived" | "rejected";

export type TraceTimelineItem = {
  id: string | number;
  time: string;
  actor: string;
  action: string;
  status: TraceStatus;
  description?: string;
};

type TraceTimelineProps = {
  items: TraceTimelineItem[];
  title?: string;
  className?: string;
};

const STATUS_TEXT: Record<TraceStatus, string> = {
  recorded: "已记录",
  pending: "待处理",
  approved: "已通过",
  archived: "已归档",
  rejected: "已驳回",
};

export function TraceTimeline({ items, title = "生长溯源时间线", className }: TraceTimelineProps) {
  return (
    <section className={`${styles.timeline} ${className ?? ""}`}>
      <Typography.Title level={2}>{title}</Typography.Title>
      <ol className={styles.list}>
        {items.map((item) => (
          <li className={styles.item} key={item.id}>
            <span className={`${styles.node} ${styles[item.status]}`} />
            <div className={styles.card}>
              <div className={styles.header}>
                <strong>{item.action}</strong>
                <span className={`${styles.status} ${styles[item.status]}`}>{STATUS_TEXT[item.status]}</span>
              </div>
              <p>{item.description}</p>
              <footer>
                <span>{item.actor}</span>
                <time>{item.time}</time>
              </footer>
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
