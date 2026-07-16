"use client";

import {
  agentPollInterval,
  createAgentTask,
  fetchAgentTask,
  fetchAgentTasks,
  type AgentTaskSummary,
} from "@/lib/research-agent";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Empty, Form, Input, InputNumber, Progress, Spin } from "antd";
import { ArrowRight, CheckCircle2, Clock3, FlaskConical, ShieldAlert, Sprout } from "lucide-react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import styles from "./assistant-float.module.css";

const ACTIVE_TASK_KEY = "web_research_agent_task_id";
const DEFAULT_GOAL =
  "持续观察当前中药材任务，发现数据异常和证据缺口后制定复测方案，补采完成后重新分析，并在满足条件时生成可信数字生命档案。";

export function AssistantAgentPanel() {
  const pathname = usePathname();
  const search = useSearchParams();
  const queryClient = useQueryClient();
  const roles = useAuthStore((state) => state.user?.roleCodes ?? []);
  const canCreate = roles.some((role) => ["ADMIN", "TEACHER"].includes(role));
  const [activeId, setActiveId] = useState<number>();
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm();
  const contextTaskId = positiveNumber(search.get("collectionTaskId"));

  useEffect(() => {
    const stored = positiveNumber(window.localStorage.getItem(ACTIVE_TASK_KEY));
    if (stored) setActiveId(stored);
  }, []);

  const listQuery = useQuery({
    queryKey: ["research-agent-list", "sidebar"],
    queryFn: () => fetchAgentTasks({ page: 1, size: 5 }),
  });
  const fallbackTask = useMemo(
    () => listQuery.data?.records.find((task) => !["COMPLETED", "FAILED", "CANCELLED"].includes(task.status))
      ?? listQuery.data?.records[0],
    [listQuery.data],
  );
  const selectedId = activeId ?? fallbackTask?.id;
  const taskQuery = useQuery({
    queryKey: ["research-agent", selectedId],
    queryFn: () => fetchAgentTask(selectedId!),
    enabled: Boolean(selectedId),
    refetchInterval: (query) => agentPollInterval(query.state.data?.status),
  });
  const task = taskQuery.data;

  useEffect(() => {
    if (fallbackTask?.id && !activeId) {
      setActiveId(fallbackTask.id);
    }
  }, [activeId, fallbackTask?.id]);

  const createMutation = useMutation({
    mutationFn: createAgentTask,
    onSuccess: async (created) => {
      window.localStorage.setItem(ACTIVE_TASK_KEY, String(created.id));
      setActiveId(created.id);
      setCreating(false);
      await queryClient.invalidateQueries({ queryKey: ["research-agent-list"] });
    },
  });

  function selectTask(next: AgentTaskSummary) {
    window.localStorage.setItem(ACTIVE_TASK_KEY, String(next.id));
    setActiveId(next.id);
  }

  if (creating) {
    return (
      <div className={styles.agentPanel}>
        <div className={styles.agentIntro}>
          <Sprout size={22} />
          <div><strong>启动科研 Agent</strong><p>用户身份由当前登录会话确定，前端不会提交 userId。</p></div>
        </div>
        <Form form={form} layout="vertical" initialValues={{ collectionTaskId: contextTaskId, goalText: DEFAULT_GOAL }}
          onFinish={(values) => createMutation.mutate({
            collectionTaskId: Number(values.collectionTaskId), targetId: Number(values.collectionTaskId),
            goalText: values.goalText, pageContext: pathname || "assistant-sidebar",
            sessionId: window.localStorage.getItem("web_assistant_session_id") || undefined,
          })}>
          <Form.Item label="采集任务 ID" name="collectionTaskId" rules={[{ required: true }]}>
            <InputNumber min={1} className={styles.agentFullWidth} />
          </Form.Item>
          <Form.Item label="科研目标" name="goalText" rules={[{ required: true }, { max: 1000 }]}>
            <Input.TextArea rows={5} />
          </Form.Item>
          {createMutation.isError ? <p className={styles.agentError}>{getApiErrorMessage(createMutation.error, "创建失败")}</p> : null}
          <div className={styles.agentFormActions}><Button onClick={() => setCreating(false)}>返回</Button><Button type="primary" htmlType="submit" loading={createMutation.isPending}>创建任务</Button></div>
        </Form>
      </div>
    );
  }

  return (
    <div className={styles.agentPanel}>
      <div className={styles.agentIntro}>
        <FlaskConical size={22} />
        <div><strong>本草数字孪生科研 Agent</strong><p>长期任务会持久化运行，关闭页面后仍可恢复。</p></div>
      </div>
      {taskQuery.isLoading || listQuery.isLoading ? <div className={styles.agentLoading}><Spin size="small" /> 正在读取科研任务</div> : task ? (
        <>
          <article className={styles.agentTaskCard}>
            <div className={styles.agentTaskTop}><span>{task.statusLabel}</span><em>{task.progressPercent}%</em></div>
            <h3>{task.target?.name || task.taskNo}</h3>
            <p>{task.goalText}</p>
            <Progress percent={task.progressPercent} showInfo={false} strokeColor="#2f7d4f" trailColor="#e8ddcc" />
            <div className={styles.agentCurrentStep}><Clock3 size={15} /><span>{task.steps.find((step) => ["RUNNING", "WAITING"].includes(step.status))?.stepName || currentPhaseLabel(task.currentPhase)}</span></div>
          </article>
          <div className={styles.agentFacts}>
            <span><ShieldAlert size={15} /><b>{task.findings.filter((finding) => finding.status === "OPEN").length}</b> 个待处理发现</span>
            <span><CheckCircle2 size={15} /><b>{task.steps.filter((step) => step.status === "SUCCEEDED").length}</b> 个步骤已完成</span>
          </div>
          {task.pendingActions[0] ? (
            <div className={styles.agentPending}><strong>等待确认</strong><p>{task.pendingActions[0].actionName}</p><small>{task.pendingActions[0].actionDescription}</small></div>
          ) : null}
          {task.status === "WAITING_FIELD_DATA" ? <div className={styles.agentWaiting}><Clock3 size={17} /><span>正在等待移动端补充现场数据，无需保持网页开启。</span></div> : null}
          <Link className={styles.agentWorkbenchLink} href={`/assistant/research-agent?agentTaskId=${task.id}`}>打开完整科研工作台 <ArrowRight size={16} /></Link>
        </>
      ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无科研 Agent 任务" />}

      <div className={styles.agentFooterActions}>
        {canCreate ? <Button type="primary" onClick={() => setCreating(true)}>启动科研 Agent</Button> : null}
        <Link href="/assistant/research-agent"><Button>查看全部任务</Button></Link>
      </div>
      {(listQuery.data?.records.length ?? 0) > 1 ? (
        <div className={styles.agentRecent}><strong>最近任务</strong>{listQuery.data?.records.map((item) => <button type="button" className={item.id === task?.id ? styles.agentRecentActive : ""} key={item.id} onClick={() => selectTask(item)}><span>{item.taskNo}</span><em>{item.statusLabel}</em></button>)}</div>
      ) : null}
    </div>
  );
}

function positiveNumber(value: string | null) {
  const number = Number(value);
  return Number.isInteger(number) && number > 0 ? number : undefined;
}

function currentPhaseLabel(value?: string) {
  if (!value) return "等待进入下一阶段";
  const labels: Record<string, string> = {
    WAITING_FOR_FOLLOW_UP_DATA: "等待现场数据",
    GENERATING_NEXT_COLLECTION_PLAN: "生成复测方案",
    WAITING_PUBLIC_CONFIRMATION: "等待公开确认",
    DIGITAL_ARCHIVE_COMPLETED: "数字生命档案已完成",
  };
  return labels[value] || "科研流程处理中";
}
