"use client";

import {
  agentPollInterval,
  cancelAgentTask,
  confirmAgentAction,
  createAgentTask,
  fetchAgentAnalysisRounds,
  fetchAgentArchiveResult,
  fetchAgentArchiveStatus,
  fetchAgentEvidence,
  fetchAgentPlan,
  fetchAgentReanalysis,
  fetchAgentTask,
  fetchAgentTasks,
  fetchAgentWaitStatus,
  generateAgentPlan,
  prepareAgentArchive,
  rejectAgentAction,
  startAgentTask,
  updateAgentPlan,
  type AgentAction,
  type AgentCollectionPlan,
  type AgentFinding,
  type AgentStatus,
  type FollowUpPlan,
} from "@/lib/research-agent";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Empty, Form, Input, InputNumber, Modal, Progress, Spin } from "antd";
import {
  Archive, ArrowRight, CheckCircle2, ClipboardCheck, Clock3, ExternalLink,
  FileSearch, FlaskConical, History, ListChecks, Play,
  QrCode, RefreshCw, ShieldCheck, Sparkles, Sprout, TriangleAlert, XCircle,
} from "lucide-react";
import Link from "next/link";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import styles from "./page.module.css";

const DEFAULT_GOAL =
  "持续观察当前中药材任务，发现数据异常和证据缺口后制定复测方案，补采完成后重新分析，并在满足条件时生成可信数字生命档案。";

const STATUS_LABEL: Record<AgentStatus, string> = {
  CREATED: "已创建", PLANNING: "规划中", RUNNING: "执行中",
  WAITING_CONFIRMATION: "等待确认", WAITING_FIELD_DATA: "等待现场数据",
  REANALYZING: "重新分析中", COMPLETED: "已完成", FAILED: "执行失败", CANCELLED: "已取消",
};

const STEP_LABEL: Record<string, string> = {
  LOAD_CONTEXT: "装载任务上下文", INSPECT_TASK: "检查采集批次",
  ANALYZE_TIMELINE: "分析生长时间线", ANALYZE_EVIDENCE: "检查图片与识别证据",
  GENERATE_COLLECTION_PLAN: "生成复测采集方案", WAIT_FOR_CONFIRMATION: "等待用户确认",
  CREATE_COLLECTION_TASK: "创建复测任务", WAIT_FOR_FIELD_DATA: "等待现场数据",
  REANALYZE: "重新分析", PREPARE_DIGITAL_ARCHIVE: "准备数字生命档案",
  GENERATE_STAGE_NARRATIONS: "生成阶段科研解说", GENERATE_ARCHIVE_SUMMARY: "生成档案摘要",
  GENERATE_ARCHIVE_HASH: "生成档案哈希", VERIFY_ARCHIVE: "验证档案完整性",
  GENERATE_TRACE_QR: "生成任务级二维码", WAIT_PUBLIC_CONFIRMATION: "等待公开确认",
  COMPLETE_REPORT: "完成科研报告",
};

const STEP_STATUS: Record<string, string> = {
  PENDING: "待执行", RUNNING: "执行中", WAITING: "等待中", SUCCEEDED: "已完成",
  FAILED: "失败", SKIPPED: "已跳过", CANCELLED: "已取消",
};

const PHASE_LABEL: Record<string, string> = {
  LOADING_CONTEXT: "正在装载任务上下文", INSPECTING_TASK: "正在检查采集任务",
  CHECKING_TIMELINE: "正在检查观测时间线", CHECKING_EVIDENCE: "正在核对证据",
  COMPLETENESS_DIAGNOSIS_COMPLETED: "完整性诊断已完成",
  EVIDENCE_ANALYSIS_COMPLETED: "多模态证据分析已完成",
  GENERATING_NEXT_COLLECTION_PLAN: "正在生成下一轮复测方案",
  WAITING_FOR_FOLLOW_UP_DATA: "等待复测现场数据",
  READY_FOR_ARCHIVE_PREPARATION: "可以准备数字生命档案",
  PREPARING_DIGITAL_ARCHIVE: "正在准备数字生命档案",
  WAITING_PUBLIC_CONFIRMATION: "等待确认公开档案",
  DIGITAL_ARCHIVE_COMPLETED: "可信数字生命档案已完成",
  INTERNAL_ARCHIVE_COMPLETED: "内部数字生命档案已完成",
};

const IMAGE_TYPE_LABEL: Record<string, string> = {
  leaf: "叶片", root: "根部", whole_plant: "全株", environment: "现场环境",
  stem: "茎部", flower: "花", fruit: "果实", medicinal_part: "药用部位", other: "其他",
};

const TARGET_TYPE_LABEL: Record<string, string> = {
  COLLECTION_TASK: "采集任务", HERB_COLLECTION_TASK: "采集任务", BATCH: "采集批次",
  GROWTH_RECORD: "生长记录", IMAGE: "现场图片", DIGITAL_ARCHIVE: "数字生命档案",
};

export function ResearchAgentContent() {
  const search = useSearchParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const user = useAuthStore((state) => state.user);
  const taskId = positiveNumber(search.get("agentTaskId"));
  const contextTaskId = positiveNumber(search.get("collectionTaskId"));
  const canManage = Boolean(user?.roleCodes.some((role) => ["ADMIN", "TEACHER"].includes(role)));
  const canConfirm = Boolean(user?.roleCodes.some((role) => ["ADMIN", "TEACHER", "REVIEWER"].includes(role)));
  const [createOpen, setCreateOpen] = useState(!taskId && Boolean(contextTaskId));
  const [cancelOpen, setCancelOpen] = useState(false);
  const [planOpen, setPlanOpen] = useState(false);
  const [planRegenerating, setPlanRegenerating] = useState(false);
  const [selectedAction, setSelectedAction] = useState<AgentAction | null>(null);
  const [createForm] = Form.useForm();
  const [cancelForm] = Form.useForm();
  const [planForm] = Form.useForm();
  const autoResumeTaskRef = useRef<number | null>(null);

  const taskQuery = useQuery({
    queryKey: ["research-agent", taskId],
    queryFn: () => fetchAgentTask(taskId!),
    enabled: Boolean(taskId),
    refetchInterval: (query) => agentPollInterval(query.state.data?.status),
  });
  const task = taskQuery.data;
  const shouldLoadEvidence = Boolean(taskId && task && !["CREATED", "PLANNING"].includes(task.status));
  const evidenceQuery = useQuery({
    queryKey: ["research-agent-evidence", taskId], queryFn: () => fetchAgentEvidence(taskId!),
    enabled: shouldLoadEvidence, refetchInterval: agentPollInterval(task?.status), retry: false,
  });
  const planQuery = useQuery({
    queryKey: ["research-agent-plan", taskId], queryFn: () => fetchAgentPlan(taskId!),
    enabled: shouldLoadEvidence, refetchInterval: agentPollInterval(task?.status), retry: false,
  });
  const waitQuery = useQuery({
    queryKey: ["research-agent-wait", taskId], queryFn: () => fetchAgentWaitStatus(taskId!),
    enabled: task?.status === "WAITING_FIELD_DATA", refetchInterval: 30_000, retry: false,
  });
  const reanalysisQuery = useQuery({
    queryKey: ["research-agent-reanalysis", taskId], queryFn: () => fetchAgentReanalysis(taskId!),
    enabled: Boolean(task && ["REANALYZING", "WAITING_CONFIRMATION", "COMPLETED"].includes(task.status)),
    retry: false,
  });
  const roundsQuery = useQuery({
    queryKey: ["research-agent-rounds", taskId], queryFn: () => fetchAgentAnalysisRounds(taskId!),
    enabled: Boolean(reanalysisQuery.data), retry: false,
  });
  const archiveStatusQuery = useQuery({
    queryKey: ["research-agent-archive-status", taskId], queryFn: () => fetchAgentArchiveStatus(taskId!),
    enabled: Boolean(task && ["RUNNING", "WAITING_CONFIRMATION", "COMPLETED"].includes(task.status)), retry: false,
  });
  const archiveResultQuery = useQuery({
    queryKey: ["research-agent-archive-result", taskId], queryFn: () => fetchAgentArchiveResult(taskId!),
    enabled: task?.status === "COMPLETED", retry: false,
  });
  const recentTasks = useQuery({
    queryKey: ["research-agent-list"], queryFn: () => fetchAgentTasks({ page: 1, size: 8 }),
    enabled: !taskId,
  });

  useEffect(() => {
    if (contextTaskId) createForm.setFieldsValue({ collectionTaskId: contextTaskId, goalText: DEFAULT_GOAL });
  }, [contextTaskId, createForm]);

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["research-agent"] });
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["research-agent-evidence", taskId] }),
      queryClient.invalidateQueries({ queryKey: ["research-agent-plan", taskId] }),
      queryClient.invalidateQueries({ queryKey: ["research-agent-wait", taskId] }),
      queryClient.invalidateQueries({ queryKey: ["research-agent-reanalysis", taskId] }),
      queryClient.invalidateQueries({ queryKey: ["research-agent-archive-status", taskId] }),
      queryClient.invalidateQueries({ queryKey: ["research-agent-archive-result", taskId] }),
    ]);
  };

  const runMutation = useMutation({
    mutationFn: async (operation: () => Promise<unknown>) => operation(),
    onSuccess: async () => { await refresh(); message.success("操作已提交"); },
    onError: (error) => message.error(getApiErrorMessage(error, "操作失败")),
  });
  const createMutation = useMutation({
    mutationFn: createAgentTask,
    onSuccess: (created) => {
      setCreateOpen(false);
      router.replace(`/assistant/research-agent?agentTaskId=${created.id}`);
      message.success("科研 Agent 任务已创建");
    },
    onError: (error) => message.error(getApiErrorMessage(error, "创建失败")),
  });
  const { mutate: resumeLegacyTask } = useMutation({
    mutationFn: startAgentTask,
    onSuccess: async () => { await refresh(); message.success("科研 Agent 已自动生成复测方案，请确认下一步动作"); },
    onError: (error) => message.error(getApiErrorMessage(error, "自动恢复科研 Agent 失败")),
  });

  useEffect(() => {
    if (!canManage || !taskId || task?.status !== "RUNNING"
      || task.currentPhase !== "EVIDENCE_ANALYSIS_COMPLETED"
      || autoResumeTaskRef.current === taskId) return;
    autoResumeTaskRef.current = taskId;
    resumeLegacyTask(taskId);
  }, [canManage, resumeLegacyTask, task?.currentPhase, task?.status, taskId]);

  const findingsByGroup = useMemo(() => groupFindings(task?.findings ?? []), [task?.findings]);
  const plan = planQuery.data;

  function openPlanEditor(value: AgentCollectionPlan) {
    planForm.setFieldsValue({
      objective: value.plan.objective,
      recommendedStartTime: value.plan.recommendedStartTime,
      recommendedEndTime: value.plan.recommendedEndTime,
      rationale: value.plan.rationale,
      completionCriteria: value.plan.completionCriteria.join("\n"),
    });
    setPlanOpen(true);
  }

  async function requestPlan(regenerate: boolean) {
    if (!taskId || planRegenerating) return;
    setPlanRegenerating(true);
    try {
      await generateAgentPlan(taskId, regenerate);
      await refresh();
      message.success(regenerate ? "复测采集方案已重新生成" : "复测采集方案已生成");
    } catch (error) {
      message.error(getApiErrorMessage(error, regenerate ? "重新生成方案失败" : "生成方案失败"));
    } finally {
      setPlanRegenerating(false);
    }
  }

  return (
    <>
      <div className={styles.page}>
        {!taskId ? (
          <Landing
            canManage={canManage}
            loading={recentTasks.isLoading}
            tasks={recentTasks.data?.records ?? []}
            onCreate={() => { createForm.setFieldsValue({ goalText: DEFAULT_GOAL }); setCreateOpen(true); }}
          />
        ) : taskQuery.isLoading ? (
          <div className={styles.loading}><Spin size="large" /><span>正在恢复持久化 Agent 任务…</span></div>
        ) : taskQuery.isError || !task ? (
          <Empty description={getApiErrorMessage(taskQuery.error, "无法读取 Agent 任务")} />
        ) : (
          <>
            <section className={styles.hero}>
              <div>
                <span className={styles.eyebrow}><FlaskConical size={15} /> HERB DIGITAL TWIN RESEARCH AGENT</span>
                <h1>本草数字孪生科研 Agent 工作台</h1>
                <p>{task.goalText}</p>
                <div className={styles.heroMeta}>
                  <span>任务编号 <strong>{task.taskNo}</strong></span>
                  <span>业务对象 <strong>{task.target?.name || `采集任务 #${task.target?.id ?? "-"}`}</strong></span>
                  <span>创建时间 <strong>{formatTime(task.createTime)}</strong></span>
                </div>
              </div>
              <div className={styles.heroStatus}>
                <span className={`${styles.statusPill} ${styles[`status${task.status}`]}`}>{STATUS_LABEL[task.status]}</span>
                <strong>{task.progressPercent}%</strong>
                <span>{phaseLabel(task.currentPhase)}</span>
                <Progress percent={task.progressPercent} showInfo={false} strokeColor="#2f7d4f" trailColor="#eadfcd" />
                <div className={styles.heroActions}>
                  <Button icon={<RefreshCw size={15} />} onClick={() => void refresh()}>刷新</Button>
                  {task.status === "CREATED" && canManage ? (
                    <Button type="primary" icon={<Play size={15} />} loading={runMutation.isPending}
                      onClick={() => runMutation.mutate(() => startAgentTask(task.id))}>开始运行</Button>
                  ) : null}
                  {!terminal(task.status) ? <Button danger onClick={() => setCancelOpen(true)}>取消任务</Button> : null}
                </div>
              </div>
            </section>

            {task.status === "FAILED" ? (
              <section className={styles.errorBanner}>
                <TriangleAlert size={21} />
                <div><strong>Agent 执行未完成</strong><p>{task.errorMessage || "任务发生不可恢复错误，请联系管理员核对失败步骤。"}</p></div>
                <Button onClick={() => void refresh()}>重新检查状态</Button>
              </section>
            ) : null}

            <div className={styles.workspaceGrid}>
              <main className={styles.primaryColumn}>
                <Section icon={<ListChecks />} title="执行步骤" subtitle="每一步均持久化，刷新或关闭页面不会丢失进度。">
                  <div className={styles.timeline}>
                    {task.steps.map((step) => (
                      <article className={`${styles.step} ${styles[`step${step.status}`]}`} key={step.id}>
                        <span className={styles.stepMarker}>{step.status === "SUCCEEDED" ? <CheckCircle2 /> : step.status === "FAILED" ? <XCircle /> : <span>{step.stepNo}</span>}</span>
                        <div><div className={styles.stepTitle}><strong>{STEP_LABEL[step.stepType] || step.stepName}</strong><em>{STEP_STATUS[step.status] || step.statusLabel}</em></div>
                          <p>{step.errorMessage || step.outputSummary || step.description || waitingReason(step.status)}</p>
                          {step.status === "FAILED" ? <Button size="small" onClick={() => void refresh()}>检查是否可继续</Button> : null}
                        </div>
                      </article>
                    ))}
                  </div>
                </Section>

                <Section icon={<FileSearch />} title="发现项与证据缺口" subtitle="按风险和处理状态整理，不展示模型内部推理。">
                  {task.findings.length ? (
                    <div className={styles.findingGroups}>
                      {findingsByGroup.map((group) => group.items.length ? (
                        <div key={group.label} className={styles.findingGroup}>
                          <h3>{group.label}<span>{group.items.length}</span></h3>
                          {group.items.map((finding) => <FindingCard finding={finding} key={finding.id} />)}
                        </div>
                      ) : null)}
                    </div>
                  ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前尚无发现项" />}
                </Section>

                <Section icon={<Sparkles />} title="多模态证据分析" subtitle="区分数据事实、AI 观察、统计关联与尚未确认内容。">
                  <EvidencePanel evidence={evidenceQuery.data} loading={evidenceQuery.isLoading} />
                </Section>

                {reanalysisQuery.data ? (
                  <Section icon={<History />} title="重新分析对比" subtitle={`已记录 ${roundsQuery.data?.length ?? 1} 轮分析，可在刷新后恢复。`}>
                    <ReanalysisPanel value={reanalysisQuery.data} rounds={roundsQuery.data ?? []} />
                  </Section>
                ) : null}

                {archiveResultQuery.data ? (
                  <ArchiveResultCard result={archiveResultQuery.data} />
                ) : archiveStatusQuery.data?.prerequisitesSatisfied && canManage && task.status === "RUNNING" ? (
                  <section className={styles.archiveReady}>
                    <Archive size={28} /><div><strong>已满足数字生命档案准备条件</strong><p>{archiveStatusQuery.data.message}</p></div>
                    <Button type="primary" loading={runMutation.isPending}
                      onClick={() => runMutation.mutate(() => prepareAgentArchive(task.id))}>准备可信档案</Button>
                  </section>
                ) : null}
              </main>

              <aside className={styles.sideColumn}>
              <Section icon={<ClipboardCheck />} title="复测采集方案">
                {planRegenerating ? <PlanGenerating regenerating={Boolean(plan)} />
                  : plan ? <PlanCard value={plan} canManage={canManage} onEdit={() => openPlanEditor(plan)}
                    onGenerate={() => void requestPlan(true)} />
                    : <div className={styles.emptyAction}><p>诊断完成后可生成基于证据缺口的复测方案。</p>{canManage && shouldLoadEvidence ? <Button onClick={() => void requestPlan(false)}>生成方案</Button> : null}</div>}
                </Section>

                <Section icon={<ShieldCheck />} title="待确认动作">
                  {task.pendingActions.length ? task.pendingActions.map((action) => (
                    <ActionCard action={action} canConfirm={canConfirm} key={action.id}
                      onConfirm={() => setSelectedAction(action)}
                      onReject={() => runMutation.mutate(() => rejectAgentAction(action.id, "用户在工作台拒绝执行"))} />
                  )) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前没有待确认动作" />}
                </Section>

                {task.status === "WAITING_FIELD_DATA" ? (
                  <Section icon={<Clock3 />} title="等待现场数据">
                    <WaitPanel value={waitQuery.data} loading={waitQuery.isLoading} onRefresh={() => void waitQuery.refetch()} />
                  </Section>
                ) : null}
              </aside>
            </div>
          </>
        )}
      </div>

      <Modal title="启动科研 Agent" open={createOpen} footer={null} onCancel={() => setCreateOpen(false)} destroyOnHidden>
        <Form form={createForm} layout="vertical" onFinish={(values) => createMutation.mutate({
          collectionTaskId: Number(values.collectionTaskId), targetId: Number(values.collectionTaskId),
          goalText: values.goalText, pageContext: search.get("pageContext") || "research-agent",
        })}>
          <Form.Item name="collectionTaskId" label="采集任务 ID" rules={[{ required: true, message: "请输入采集任务 ID" }]}><InputNumber min={1} className={styles.fullWidth} /></Form.Item>
          <Form.Item name="goalText" label="长期科研目标" initialValue={DEFAULT_GOAL} rules={[{ required: true }, { max: 1000 }]}><Input.TextArea rows={6} /></Form.Item>
          <div className={styles.modalActions}><Button onClick={() => setCreateOpen(false)}>取消</Button><Button type="primary" htmlType="submit" loading={createMutation.isPending}>创建并进入工作台</Button></div>
        </Form>
      </Modal>

      <Modal title="取消 Agent 任务" open={cancelOpen} footer={null} onCancel={() => setCancelOpen(false)}>
        <Form form={cancelForm} layout="vertical" onFinish={(values) => runMutation.mutate(async () => { await cancelAgentTask(taskId!, values.reason); setCancelOpen(false); })}>
          <p>只取消 Agent 工作流，不会删除已经创建的真实采集任务或现场数据。</p>
          <Form.Item name="reason" label="取消原因" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
          <div className={styles.modalActions}><Button onClick={() => setCancelOpen(false)}>返回</Button><Button danger type="primary" htmlType="submit">确认取消</Button></div>
        </Form>
      </Modal>

      <Modal title="调整复测采集方案" open={planOpen} footer={null} onCancel={() => setPlanOpen(false)} width={680}>
        <Form form={planForm} layout="vertical" onFinish={(values) => {
          if (!plan) return;
          const next: FollowUpPlan = { ...plan.plan, objective: values.objective, recommendedStartTime: values.recommendedStartTime,
            recommendedEndTime: values.recommendedEndTime, rationale: values.rationale,
            completionCriteria: String(values.completionCriteria).split("\n").map((item) => item.trim()).filter(Boolean) };
          runMutation.mutate(async () => { await updateAgentPlan(taskId!, plan.id, next); setPlanOpen(false); });
        }}>
          <Form.Item name="objective" label="复测目的" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
          <div className={styles.twoFields}><Form.Item name="recommendedStartTime" label="推荐开始时间"><Input /></Form.Item><Form.Item name="recommendedEndTime" label="推荐结束时间"><Input /></Form.Item></div>
          <Form.Item label="必填指标与必拍图片"><div className={styles.lockedOptions}>仅可在后端允许的指标和图片类型范围内调整；本次保留当前结构化清单。</div></Form.Item>
          <Form.Item name="completionCriteria" label="完成条件（每行一项）" rules={[{ required: true }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="rationale" label="方案依据" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
          <div className={styles.modalActions}><Button onClick={() => setPlanOpen(false)}>取消</Button><Button type="primary" htmlType="submit">保存调整</Button></div>
        </Form>
      </Modal>

      <Modal title="高风险动作二次确认" open={Boolean(selectedAction)} onCancel={() => setSelectedAction(null)}
        okText="确认执行" cancelText="暂不执行" confirmLoading={runMutation.isPending}
        onOk={() => selectedAction && runMutation.mutate(async () => {
          await confirmAgentAction(selectedAction.id, selectedAction.actionType === "CREATE_FOLLOW_UP_COLLECTION_TASK");
          setSelectedAction(null);
        })}>
        {selectedAction ? <div className={styles.confirmPanel}><span>Agent 准备执行以下操作：</span><strong>{selectedAction.actionName}</strong>
          <p>{selectedAction.actionDescription || "执行后将产生真实业务影响。"}</p>
          <dl><div><dt>目标对象</dt><dd>{selectedAction.targetType || "业务任务"} #{selectedAction.targetId}</dd></div><div><dt>风险级别</dt><dd>{riskLabel(selectedAction.riskLevel)}</dd></div><div><dt>是否公开数据</dt><dd>{selectedAction.actionType === "ENABLE_PUBLIC_TRACE" ? "是" : "否"}</dd></div><div><dt>是否可撤销</dt><dd>需通过对应业务流程处理</dd></div></dl>
        </div> : null}
      </Modal>
    </>
  );
}

function Landing({ canManage, loading, tasks, onCreate }: { canManage: boolean; loading: boolean; tasks: Array<{ id: number; taskNo: string; goalText: string; status: AgentStatus; progressPercent: number; target?: { name?: string } }>; onCreate: () => void }) {
  return <><section className={styles.landingHero}><span className={styles.eyebrow}><FlaskConical size={15} /> HERB DIGITAL TWIN RESEARCH AGENT</span><h1>本草数字孪生科研 Agent 工作台</h1><p>持续分析连续观测任务，识别证据缺口，安全编排复测采集与可信数字生命档案。</p>{canManage ? <Button type="primary" size="large" icon={<Sprout size={17} />} onClick={onCreate}>启动科研 Agent</Button> : <p className={styles.permissionHint}>当前角色可查看有权访问的 Agent 任务；启动权限由后端业务权限决定。</p>}</section><Section icon={<History />} title="最近科研任务" subtitle="选择任务可恢复执行步骤、发现项和等待状态。">{loading ? <Spin /> : tasks.length ? <div className={styles.taskList}>{tasks.map((task) => <Link href={`/assistant/research-agent?agentTaskId=${task.id}`} key={task.id}><div><strong>{task.taskNo}</strong><span>{task.target?.name || "采集任务"}</span><p>{task.goalText}</p></div><aside><em>{STATUS_LABEL[task.status]}</em><b>{task.progressPercent}%</b><ArrowRight size={17} /></aside></Link>)}</div> : <Empty description="暂无科研 Agent 任务" />}</Section></>;
}

function Section({ icon, title, subtitle, children }: { icon: React.ReactNode; title: string; subtitle?: string; children: React.ReactNode }) {
  return <section className={styles.section}><header><span>{icon}</span><div><h2>{title}</h2>{subtitle ? <p>{subtitle}</p> : null}</div></header><div className={styles.sectionBody}>{children}</div></section>;
}

function FindingCard({ finding }: { finding: AgentFinding }) {
  return <details className={styles.finding}><summary><span className={`${styles.severity} ${styles[`severity${finding.severity}`]}`}>{severityLabel(finding.severity)}</span><div><strong>{localizeTechnicalText(finding.title)}</strong><small>{finding.targetType ? targetReference(finding.targetType, finding.targetId) : "任务级发现"}</small></div><em>{finding.status === "RESOLVED" ? "已解决" : finding.status === "IGNORED" ? "已忽略" : "待处理"}</em></summary><div className={styles.findingDetail}><p>{localizeTechnicalText(finding.description || "已记录结构化证据摘要。")}</p>{finding.suggestion ? <p><b>建议：</b>{localizeTechnicalText(finding.suggestion)}</p> : null}<small>发现时间：{formatTime(finding.createTime)}</small></div></details>;
}

function EvidencePanel({ evidence, loading }: { evidence?: Awaited<ReturnType<typeof fetchAgentEvidence>>; loading: boolean }) {
  if (loading) return <Spin />;
  const explanation = evidence?.analysis?.explanation;
  if (!evidence?.available || !explanation) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={evidence?.message || "证据分析尚未生成"} />;
  const blocks = [
    ["数据事实", explanation.confirmedFacts, "fact"], ["统计关联", explanation.possibleAssociations, "association"],
    ["尚未确认", explanation.uncertainties, "uncertain"], ["需要补充的证据", explanation.evidenceGaps, "gap"],
  ] as const;
  return <div className={styles.evidence}><div className={styles.evidenceConclusion}><strong>当前结论</strong><p>{localizeTechnicalText(explanation.conclusion)}</p><span>可信度：{confidenceLabel(explanation.confidenceLevel)}</span></div><div className={styles.evidenceGrid}>{blocks.map(([title, items, kind]) => <article className={styles[kind]} key={title}><h3>{title}</h3>{items?.length ? <ul>{items.map((item) => <li key={item}>{localizeTechnicalText(item)}</li>)}</ul> : <p>暂无记录</p>}</article>)}</div>{evidence.analysis?.metricTrends?.length ? <div className={styles.evidenceCount}><ListChecks size={16} /> 已完成 {evidence.analysis.metricTrends.length} 项指标趋势检查、{evidence.analysis.stages.length} 个阶段证据核对</div> : null}</div>;
}

function PlanGenerating({ regenerating }: { regenerating: boolean }) {
  return <div className={styles.planGenerating}><div className={styles.planGeneratingMark}><Sprout /><span /></div><strong>{regenerating ? "正在调用科研模型重新生成方案" : "正在调用科研模型生成方案"}</strong><p>正在重新核对证据缺口与采集要求，通常需要 10–30 秒。</p></div>;
}

function PlanCard({ value, canManage, onEdit, onGenerate }: { value: AgentCollectionPlan; canManage: boolean; onEdit: () => void; onGenerate: () => void }) {
  const plan = value.plan;
  return <article className={styles.plan}><div className={styles.planHead}><span>{value.statusLabel}</span><em>第 {value.researchRound} 轮</em></div><h3>{localizeTechnicalText(plan.objective)}</h3><InfoList title="推荐时间" items={[`${formatTime(plan.recommendedStartTime)} 至 ${formatTime(plan.recommendedEndTime)}`]} /><InfoList title="必填指标" items={plan.requiredMetrics.map((item) => `${item.metricName}${item.unit ? `（${item.unit}）` : ""}`)} /><InfoList title="必拍图片" items={plan.requiredImages.map((item) => `${localizeTechnicalText(item.imageTypeName || item.imageType)} × ${item.minCount}${item.shootingGuidance ? `：${localizeTechnicalText(item.shootingGuidance)}` : ""}`)} /><InfoList title="完成条件" items={plan.completionCriteria.map(localizeTechnicalText)} /><div className={styles.planReason}><b>方案依据</b><p>{localizeTechnicalText(plan.rationale)}</p>{plan.uncertainty ? <small>不确定性：{localizeTechnicalText(plan.uncertainty)}</small> : null}</div>{canManage && value.status === "PROPOSED" ? <div className={styles.cardActions}><Button onClick={onEdit}>调整方案</Button><Button onClick={onGenerate}>重新生成</Button></div> : null}</article>;
}

function ActionCard({ action, canConfirm, onConfirm, onReject }: { action: AgentAction; canConfirm: boolean; onConfirm: () => void; onReject: () => void }) {
  return <article className={styles.actionCard}><div><span className={styles.risk}>{riskLabel(action.riskLevel)}</span><em>{action.needConfirm ? "需人工确认" : "低风险动作"}</em></div><h3>{localizeTechnicalText(action.actionName)}</h3><p>{localizeTechnicalText(action.actionDescription)}</p><dl><div><dt>影响对象</dt><dd>{targetReference(action.targetType, action.targetId)}</dd></div><div><dt>公开数据</dt><dd>{action.actionType === "ENABLE_PUBLIC_TRACE" ? "将开启公开访问" : "不直接公开"}</dd></div></dl>{canConfirm ? <div className={styles.cardActions}><Button danger onClick={onReject}>拒绝</Button><Button type="primary" onClick={onConfirm}>确认执行</Button></div> : <p className={styles.permissionHint}>当前账号无权确认此动作。</p>}</article>;
}

function WaitPanel({ value, loading, onRefresh }: { value?: Awaited<ReturnType<typeof fetchAgentWaitStatus>>; loading: boolean; onRefresh: () => void }) {
  if (loading || !value) return <Spin />;
  return <div className={styles.wait}><div className={styles.waitProgress}><strong>{value.progressPercent}%</strong><Progress percent={value.progressPercent} showInfo={false} strokeColor="#2f7d4f" /></div><p><b>{value.followUpTaskName || "复测采集任务"}</b><br />截止：{formatTime(value.deadline)}</p><InfoList title="已完成" items={value.completedRequirements} checked /><InfoList title="尚缺" items={value.missingRequirements} /><small>最后检查：{formatTime(value.lastCheckTime)}</small><p className={styles.persistHint}>Agent 任务已持久化，关闭页面后仍会继续等待现场数据。</p><div className={styles.cardActions}>{value.followUpTaskId ? <Link href={`/growth?collectionTaskId=${value.followUpTaskId}`}><Button icon={<ExternalLink size={14} />}>打开复测任务</Button></Link> : null}<Button icon={<RefreshCw size={14} />} onClick={onRefresh}>刷新状态</Button></div></div>;
}

function ReanalysisPanel({ value, rounds }: { value: Awaited<ReturnType<typeof fetchAgentReanalysis>>; rounds: Awaited<ReturnType<typeof fetchAgentAnalysisRounds>> }) {
  return <div className={styles.reanalysis}><div className={styles.scoreChange}><span><small>补采前完整度</small><strong>{value.completenessScoreBefore}%</strong></span><ArrowRight /><span><small>补采后完整度</small><strong>{value.completenessScoreAfter}%</strong></span></div><div className={styles.changeStats}><span>已解决 <b>{value.resolvedFindings.length}</b></span><span>仍存在 <b>{value.remainingFindings.length}</b></span><span>新发现 <b>{value.newFindings.length}</b></span><span>指标变化 <b>{value.metricChanges.length}</b></span></div><p className={styles.reanalysisConclusion}>{value.conclusion || "重新分析已完成。"}</p><details><summary>查看历史分析轮次（{rounds.length}）</summary><ol>{rounds.map((round) => <li key={round.id}><strong>第 {round.roundNo} 轮</strong><span>{round.conclusion || round.outcome || round.status}</span><time>{formatTime(round.finishTime)}</time></li>)}</ol></details></div>;
}

function ArchiveResultCard({ result }: { result: Awaited<ReturnType<typeof fetchAgentArchiveResult>> }) {
  const publicUrl = absolutePublicUrl(result.publicUrl);
  return <section className={styles.archiveResult}><div className={styles.seal}><ShieldCheck /><span>可信档案</span></div><header><span>HERB DIGITAL LIFE ARCHIVE</span><h2>数字生命档案签发结果</h2><p>档案编号：{result.archiveNo}</p></header><div className={styles.archiveMetrics}><span><small>有效阶段</small><strong>{result.stageCount}</strong></span><span><small>现场影像</small><strong>{result.imageCount}</strong></span><span><small>完整度</small><strong>{result.completenessScore}%</strong></span><span><small>完整性校验</small><strong>{result.integrityVerified ? "通过" : "未通过"}</strong></span></div><div className={styles.hashLine}><ShieldCheck size={18} /><span>SHA-256 根哈希</span><code>{result.rootHashShort || "尚未生成"}</code><em>{result.hashVersion}</em></div>{result.publicVisible && result.qrCodeUrl ? <div className={styles.publicResult}><Image unoptimized width={132} height={132} src={apiAssetUrl(result.qrCodeUrl)} alt="任务级数字生命档案二维码" /><div><strong>公开访问已开启</strong><p>{publicUrl}</p><div className={styles.cardActions}><Button onClick={() => void navigator.clipboard.writeText(publicUrl)}>复制公开链接</Button><a href={apiAssetUrl(result.qrCodeUrl)} download><Button icon={<QrCode size={14} />}>下载二维码</Button></a><Link href={result.publicUrl || "#"}><Button type="primary" icon={<ExternalLink size={14} />}>查看数字生命档案</Button></Link></div></div></div> : <p className={styles.internalOnly}>内部档案已完成，尚未开启公开访问。</p>}{result.limitations.length ? <InfoList title="证据限制" items={result.limitations} /> : null}</section>;
}

function InfoList({ title, items, checked = false }: { title: string; items: string[]; checked?: boolean }) { return <div className={styles.infoList}><b>{title}</b>{items.length ? <ul>{items.map((item) => <li key={item}>{checked ? <CheckCircle2 size={14} /> : null}{item}</li>)}</ul> : <p>暂无</p>}</div>; }
function positiveNumber(value: string | null) { const number = Number(value); return Number.isInteger(number) && number > 0 ? number : undefined; }
function formatTime(value?: string) { return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "待确定"; }
function terminal(status: AgentStatus) { return ["COMPLETED", "FAILED", "CANCELLED"].includes(status); }
function phaseLabel(phase?: string) { return phase ? PHASE_LABEL[phase] || "科研流程处理中" : "等待进入下一阶段"; }
function waitingReason(status: string) { return status === "WAITING" ? "正在等待外部条件满足" : "等待 Agent 推进"; }
function severityLabel(value: string) { return ({ CRITICAL: "严重", HIGH: "高", MEDIUM: "中", LOW: "低", INFO: "提示" } as Record<string, string>)[value] || value; }
function riskLabel(value: string) { return ({ CRITICAL: "极高风险", HIGH: "高风险", MEDIUM: "中风险", LOW: "低风险" } as Record<string, string>)[value] || value; }
function confidenceLabel(value?: string) { return ({ HIGH: "高", MEDIUM: "中", LOW: "低" } as Record<string, string>)[value || ""] || "未评定"; }
function groupFindings(findings: AgentFinding[]) { return [
  { label: "高优先级", items: findings.filter((f) => ["HIGH", "CRITICAL"].includes(f.severity) && f.status !== "RESOLVED") },
  { label: "中优先级", items: findings.filter((f) => f.severity === "MEDIUM" && f.status !== "RESOLVED") },
  { label: "一般提示", items: findings.filter((f) => ["INFO", "LOW"].includes(f.severity) && f.status !== "RESOLVED") },
  { label: "已解决", items: findings.filter((f) => f.status === "RESOLVED") },
]; }

function localizeTechnicalText(value?: string) {
  if (!value) return "";
  return Object.entries(IMAGE_TYPE_LABEL).reduce(
    (localized, [code, label]) => localized.replace(new RegExp(`\\b${code}\\b`, "g"), label),
    value,
  );
}

function targetReference(targetType?: string, targetId?: number) {
  const label = targetType ? TARGET_TYPE_LABEL[targetType] || targetType : "业务数据";
  return targetId && targetId > 0 ? `${label} #${targetId}` : label;
}
function absolutePublicUrl(path?: string) { if (!path) return ""; return typeof window === "undefined" ? path : new URL(path, window.location.origin).toString(); }
function apiAssetUrl(path: string) { const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api"; return path.startsWith("/api/") ? `${base.replace(/\/api\/?$/, "")}${path}` : path; }
