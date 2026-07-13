import { Card, Empty } from "antd";
import type { EvaluationResult } from "./types";
import styles from "./evaluation.module.css";

export function RecentEvaluationResultCard({ results }: { results: EvaluationResult[] }) {
  return (
    <Card className={styles.bottomCard} title="近期评价结果">
      {results.length > 0 ? <div className={styles.resultList}>{results.slice(0, 3).map((result) => <div key={result.id}><strong>{result.objectName}</strong><span>综合得分 {result.score} · 等级 {result.level} · {result.completedAt}</span></div>)}</div> : <Empty description="暂无评价结果" />}
    </Card>
  );
}
