"use client";

import { Card, Typography } from "antd";
import styles from "./MetricCard.module.css";

type MetricCardProps = {
  title: string;
  value: string | number;
  unit?: string;
  trend?: React.ReactNode;
  icon?: React.ReactNode;
  description?: React.ReactNode;
  className?: string;
};

export function MetricCard({
  title,
  value,
  unit,
  trend,
  icon,
  description,
  className,
}: MetricCardProps) {
  return (
    <Card className={`${styles.card} ${className ?? ""}`} variant="borderless">
      <div className={styles.topLine}>
        <Typography.Text className={styles.title}>{title}</Typography.Text>
        {icon ? <span className={styles.icon}>{icon}</span> : null}
      </div>
      <div className={styles.valueLine}>
        <strong>{value}</strong>
        {unit ? <span>{unit}</span> : null}
      </div>
      <div className={styles.bottomLine}>
        {description ? <Typography.Text className={styles.description}>{description}</Typography.Text> : null}
        {trend ? <span className={styles.trend}>{trend}</span> : null}
      </div>
    </Card>
  );
}
