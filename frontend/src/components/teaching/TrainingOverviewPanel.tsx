"use client";

import { CalendarOutlined, MessageOutlined, ProfileOutlined } from "@ant-design/icons";
import { Button, Modal } from "antd";
import { useEffect, useState } from "react";
import { exportTrainingRecords, listTrainingFeedback, listTrainingPlans, listTrainingRecords } from "@/lib/training";
import { TrainingManagementPanel } from "./TrainingManagementPanel";
import { TrainingSummaryCard } from "./TrainingSummaryCard";
import styles from "./teaching.module.css";

type ModalKey = "plans" | "records" | "feedback" | null;

type Props = { canManage?: boolean; currentUserId?: number };

export function TrainingOverviewPanel({ canManage = false, currentUserId }: Props) {
  const [modalKey, setModalKey] = useState<ModalKey>(null);
  const [metrics, setMetrics] = useState({ plans: 0, records: 0, people: 0, feedback: 0, rating: 0 });

  useEffect(() => {
    void Promise.all([listTrainingPlans({ publishStatus: "published" }), listTrainingRecords(), listTrainingFeedback()])
      .then(([plans, records, feedback]) => {
        const people = new Set(records.records.map((item) => item.userId)).size;
        const rating = feedback.records.length
          ? feedback.records.reduce((sum, item) => sum + Number(item.rating || 0), 0) / feedback.records.length
          : 0;
        setMetrics({
          plans: plans.total ?? plans.records.length,
          records: records.total ?? records.records.length,
          people,
          feedback: feedback.total ?? feedback.records.length,
          rating: Number(rating.toFixed(1)),
        });
      })
      .catch(() => undefined);
  }, [modalKey]);

  const title = modalKey === "plans" ? "培训计划" : modalKey === "records" ? "培训记录" : "培训反馈";

  return (
    <section className={styles.trainingPanel}>
      <div className={styles.trainingHeader}><strong>教学培训概览</strong></div>
      {canManage ? <Button onClick={() => void exportTrainingRecords()}>导出全部参加记录</Button> : null}
      <div className={styles.trainingGrid}>
        <TrainingSummaryCard
          title="培训计划管理"
          description="查看现有培训计划，管理员可组合课程、课题、基地和参与人员。"
          icon={<ProfileOutlined />}
          metrics={[{ label: "已发布计划", value: metrics.plans }, { label: "参与人次", value: metrics.records }, { label: "参与人数", value: metrics.people }]}
          onClick={() => setModalKey("plans")}
        />
        <TrainingSummaryCard
          title="培训记录管理"
          description="查看培训参与次数和参与人次，普通用户重点查看自己的参与记录。"
          icon={<CalendarOutlined />}
          metrics={[{ label: "记录总数", value: metrics.records }, { label: "参与人数", value: metrics.people }, { label: "参与人次", value: metrics.records }]}
          onClick={() => setModalKey("records")}
        />
        <TrainingSummaryCard
          title="培训反馈管理"
          description="参与培训的老师和学生提交问卷，所有角色查看反馈统计。"
          icon={<MessageOutlined />}
          metrics={[{ label: "反馈总数", value: metrics.feedback }, { label: "参与人次", value: metrics.records }, { label: "平均评分", value: metrics.rating, suffix: "/5" }]}
          onClick={() => setModalKey("feedback")}
        />
      </div>
      <Modal open={modalKey !== null} title={title} footer={null} width={1080} destroyOnClose onCancel={() => setModalKey(null)}>
        {modalKey ? <TrainingManagementPanel mode={modalKey} canManage={canManage} currentUserId={currentUserId} /> : null}
      </Modal>
    </section>
  );
}
