import { CheckCircleOutlined, ClockCircleOutlined, FileTextOutlined, ProfileOutlined } from "@ant-design/icons";
import { MetricCard } from "@/components/common/MetricCard";
import type { EvaluationSummary } from "./types";
import styles from "./evaluation.module.css";

export function EvaluationSummaryGrid({ summary }: { summary: EvaluationSummary }) {
  const items = [
    { title: "评价任务", value: summary.taskTotal, description: `本月新增 ${summary.taskNewCount} 项`, icon: <ProfileOutlined /> },
    { title: "进行中", value: summary.ongoingTaskCount, description: `${summary.nearDeadlineTaskCount} 项临近截止`, icon: <ClockCircleOutlined /> },
    { title: "申报档案", value: summary.applicationTotal, description: `草稿 ${summary.draftCount} 项`, icon: <FileTextOutlined /> },
    { title: "待审核", value: summary.pendingCount, description: `最长等待 ${summary.longestWaitingDays} 天`, icon: <CheckCircleOutlined /> },
  ];
  return (
    <section aria-label="评价申报统计" className={styles.summaryGrid}>
      {items.map((item) => <MetricCard description={item.description} icon={item.icon} key={item.title} title={item.title} value={item.value} variant="compact" />)}
    </section>
  );
}
