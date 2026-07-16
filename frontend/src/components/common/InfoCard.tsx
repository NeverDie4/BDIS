"use client";

import { Card } from "antd";
import styles from "./InfoCard.module.css";

type InfoCardProps = {
  title?: React.ReactNode;
  extra?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
};

export function InfoCard({ title, extra, children, className }: InfoCardProps) {
  return (
    <Card
      className={`${styles.card} ${className ?? ""}`}
      extra={extra}
      title={title}
      variant="borderless"
    >
      {children}
    </Card>
  );
}
