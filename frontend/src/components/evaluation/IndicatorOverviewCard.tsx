import { Card } from "antd";
import type { EvaluationIndicator } from "./types";
import styles from "./evaluation.module.css";

export function IndicatorOverviewCard({ indicators }: { indicators: EvaluationIndicator[] }) {
  return (
    <Card className={styles.bottomCard} title="评价指标概览">
      <ul className={styles.indicatorList}>
        {indicators.map((indicator) => <li key={indicator.id}><span>{indicator.name} · 权重 {indicator.weight}%</span><strong>{indicator.score} 分</strong></li>)}
      </ul>
    </Card>
  );
}
