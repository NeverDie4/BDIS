import { CalendarOutlined, MessageOutlined, ProfileOutlined } from "@ant-design/icons";
import { TrainingSummaryCard } from "./TrainingSummaryCard";
import styles from "./teaching.module.css";

export function TrainingOverviewPanel() {
  return (
    <section className={styles.trainingPanel}>
      <div className={styles.trainingHeader}>
        <strong>教学培训概览</strong>
      </div>
      <div className={styles.trainingGrid}>
        <TrainingSummaryCard
          description="制定和管理教学培训计划，统筹课程培训安排。"
          icon={<ProfileOutlined />}
          metrics={[
            { label: "计划总数", value: 12 },
            { label: "进行中", value: 4 },
            { label: "已完成", value: 8 },
          ]}
          title="培训计划管理"
        />
        <TrainingSummaryCard
          description="记录培训过程与参与情况，支持结果统计与导出。"
          icon={<CalendarOutlined />}
          metrics={[
            { label: "记录总数", value: 36 },
            { label: "本月新增", value: 6 },
            { label: "参与人次", value: 248 },
          ]}
          title="培训记录管理"
        />
        <TrainingSummaryCard
          description="收集并分析培训反馈，持续优化教学培训质量。"
          icon={<MessageOutlined />}
          metrics={[
            { label: "反馈总数", value: 58 },
            { label: "本月新增", value: 9 },
            { label: "平均满意度", value: 96, suffix: "%" },
          ]}
          title="培训反馈管理"
        />
      </div>
    </section>
  );
}
