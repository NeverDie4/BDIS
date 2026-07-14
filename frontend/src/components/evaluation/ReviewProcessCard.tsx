import { CheckCircleOutlined, ClockCircleOutlined } from "@ant-design/icons";
import { Card } from "antd";
import type { EvaluationApplication } from "./types";
import styles from "./evaluation.module.css";

export function ReviewProcessCard({ application }: { application?: EvaluationApplication }) {
  const completed = application?.reviewRecords.length ?? 4;
  const steps = ["创建任务", "专家评分", "结果确认", "创建申报", "提交审核", "归档输出"];
  return (
    <Card className={styles.bottomCard} title="审核流程进度">
      <ol className={styles.processList}>
        {steps.map((step, index) => <li className={index < completed ? styles.processDone : ""} key={step}>{index < completed ? <CheckCircleOutlined /> : <ClockCircleOutlined />}<span>{step}</span></li>)}
      </ol>
    </Card>
  );
}
