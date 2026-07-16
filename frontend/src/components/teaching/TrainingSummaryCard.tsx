import type { ReactNode } from "react";
import { Button } from "antd";
import styles from "./teaching.module.css";

type TrainingMetric = {
  label: string;
  value: string | number;
  suffix?: string;
};

type TrainingSummaryCardProps = {
  icon: ReactNode;
  title: string;
  description: string;
  metrics: TrainingMetric[];
  actionText?: string;
  onClick?: () => void;
};

export function TrainingSummaryCard({
  icon,
  title,
  description,
  metrics,
  onClick,
  actionText = "查看全部",
}: TrainingSummaryCardProps) {
  return (
    <article className={styles.trainingCard}>
      <div className={styles.trainingCardHeader}>
        <span className={styles.trainingCardIcon}>{icon}</span>
        <strong>{title}</strong>
      </div>
      <p className={styles.trainingCardDescription}>{description}</p>
      <div className={styles.trainingMetrics}>
        {metrics.map((metric) => (
          <div className={styles.trainingMetric} key={metric.label}>
            <span>{metric.label}</span>
            <strong>{metric.value}{metric.suffix}</strong>
          </div>
        ))}
      </div>
      <Button className={styles.trainingAction} type="link" onClick={onClick}>
        {actionText} &gt;
      </Button>
    </article>
  );
}
