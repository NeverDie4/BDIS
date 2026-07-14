import { Button, Empty, Skeleton, Table, type TableProps } from "antd";
import { ArrowRight, ClipboardCheck, FileClock, ListChecks, Scale } from "lucide-react";
import type { Declaration, EvaluationIndicator, EvaluationTask } from "@/lib/evaluation";
import type { EvaluationTabKey } from "./types";
import { applicationTypeLabel, EvaluationStatus, formatDateTime } from "./display";
import styles from "./evaluation.module.css";

type Props = {
  loading: boolean;
  tasks: EvaluationTask[];
  indicators: EvaluationIndicator[];
  declarations: Declaration[];
  onNavigate: (tab: EvaluationTabKey) => void;
};

export function EvaluationWorkbench({
  loading,
  tasks,
  indicators,
  declarations,
  onNavigate,
}: Props) {
  if (loading) return <Skeleton active paragraph={{ rows: 8 }} />;
  const pendingTasks = tasks.filter((item) => item.taskStatus !== "confirmed");
  const pendingDeclarations = declarations.filter((item) => item.reviewStatus === "submitted");
  const weightTotal = indicators.reduce((sum, item) => sum + Number(item.weight || 0), 0);
  const taskColumns: TableProps<EvaluationTask>["columns"] = [
    { title: "任务", dataIndex: "taskName", ellipsis: true },
    {
      title: "状态",
      dataIndex: "taskStatus",
      width: 90,
      render: (value) => <EvaluationStatus value={value} />,
    },
    { title: "截止时间", dataIndex: "endedAt", width: 150, render: formatDateTime },
  ];
  const declarationColumns: TableProps<Declaration>["columns"] = [
    { title: "申报档案", dataIndex: "applicationTitle", ellipsis: true },
    {
      title: "类型",
      dataIndex: "applicationType",
      width: 110,
      render: applicationTypeLabel,
    },
    { title: "提交时间", dataIndex: "submittedAt", width: 150, render: formatDateTime },
  ];

  return (
    <section className={styles.workbench}>
      <div className={styles.workbenchMetrics}>
        <button type="button" onClick={() => onNavigate("tasks")}>
          <ListChecks size={20} />
          <span>评价任务</span>
          <strong>{tasks.length}</strong>
          <small>{pendingTasks.length} 项待完成</small>
        </button>
        <button type="button" onClick={() => onNavigate("indicators")}>
          <Scale size={20} />
          <span>指标体系</span>
          <strong>{indicators.length}</strong>
          <small>当前权重 {weightTotal}%</small>
        </button>
        <button type="button" onClick={() => onNavigate("archives")}>
          <FileClock size={20} />
          <span>待审核申报</span>
          <strong>{pendingDeclarations.length}</strong>
          <small>共 {declarations.length} 份档案</small>
        </button>
        <button type="button" onClick={() => onNavigate("tasks")}>
          <ClipboardCheck size={20} />
          <span>已确认结果</span>
          <strong>{tasks.filter((item) => item.taskStatus === "confirmed").length}</strong>
          <small>查看评价结果</small>
        </button>
      </div>

      <div className={styles.workbenchGrid}>
        <div className={styles.dataPanel}>
          <header>
            <div>
              <h3>待办评价任务</h3>
              <p>优先处理评分中和临近截止的任务</p>
            </div>
            <Button type="text" icon={<ArrowRight size={16} />} onClick={() => onNavigate("tasks")}>
              全部任务
            </Button>
          </header>
          {pendingTasks.length ? (
            <Table
              columns={taskColumns}
              dataSource={pendingTasks.slice(0, 6)}
              pagination={false}
              rowKey="id"
              size="small"
            />
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无待办任务" />
          )}
        </div>
        <div className={styles.dataPanel}>
          <header>
            <div>
              <h3>待审核申报</h3>
              <p>已提交并等待业务审核的申报档案</p>
            </div>
            <Button
              type="text"
              icon={<ArrowRight size={16} />}
              onClick={() => onNavigate("archives")}
            >
              申报管理
            </Button>
          </header>
          {pendingDeclarations.length ? (
            <Table
              columns={declarationColumns}
              dataSource={pendingDeclarations.slice(0, 6)}
              pagination={false}
              rowKey="id"
              size="small"
            />
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无待审核申报" />
          )}
        </div>
      </div>
    </section>
  );
}
