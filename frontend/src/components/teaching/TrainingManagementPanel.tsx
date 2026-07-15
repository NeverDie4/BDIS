"use client";

import { PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import { App, Button, Card, Col, DatePicker, Form, Input, Modal, Rate, Row, Select, Space, Statistic, Table, Tag } from "antd";
import type { FormInstance } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { Dayjs } from "dayjs";
import { useCallback, useEffect, useState } from "react";
import { fetchEnabledHerbs, fetchHerbBases, type HerbBaseApi, type HerbSpeciesApi } from "@/lib/herbs";
import { listResearchProjects, listResearchUsers, type ResearchProjectApi, type ResearchUserCandidateApi } from "@/lib/research";
import { getApiErrorMessage } from "@/lib/request";
import { listCourses } from "@/lib/courses";
import {
  addTrainingParticipants,
  createTrainingFeedback,
  createTrainingPlan,
  createTrainingPlanItem,
  getTrainingSummary,
  joinTrainingPlan,
  listTrainingFeedback,
  listTrainingPlanItems,
  listTrainingPlans,
  listTrainingRecords,
  publishTrainingPlan,
  type TrainingFeedback,
  type TrainingPlan,
  type TrainingPlanItem,
  type TrainingRecord,
  type TrainingSummary,
} from "@/lib/training";
import styles from "./teaching.module.css";

type Mode = "plans" | "records" | "feedback";
type Props = { mode: Mode; canManage?: boolean; currentUserId?: number };
type FeedbackValues = { trainingRecordId: number; rating: number; feedbackContent?: string };
const statusLabels: Record<string, string> = { draft: "草稿", published: "已发布", closed: "已关闭" };

export function TrainingManagementPanel({ mode, canManage = false, currentUserId }: Props) {
  const { message } = App.useApp();
  const [plans, setPlans] = useState<TrainingPlan[]>([]);
  const [records, setRecords] = useState<TrainingRecord[]>([]);
  const [feedback, setFeedback] = useState<TrainingFeedback[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<TrainingPlan | null>(null);
  const [items, setItems] = useState<TrainingPlanItem[]>([]);
  const [summary, setSummary] = useState<TrainingSummary | null>(null);
  const [selectedRecord, setSelectedRecord] = useState<TrainingRecord | null>(null);
  const [selectedHasFeedback, setSelectedHasFeedback] = useState(false);
  const [planOpen, setPlanOpen] = useState(false);
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [planForm] = Form.useForm<Record<string, unknown>>();
  const [feedbackForm] = Form.useForm<FeedbackValues>();

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      if (mode === "plans") setPlans((await listTrainingPlans(canManage ? {} : { publishStatus: "published" })).records);
      if (mode === "records") setRecords((await listTrainingRecords()).records);
      if (mode === "feedback") {
        const [feedbackPage, recordPage] = await Promise.all([listTrainingFeedback(), listTrainingRecords()]);
        setFeedback(feedbackPage.records);
        setRecords(recordPage.records);
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, "培训数据加载失败"));
    } finally {
      setLoading(false);
    }
  }, [canManage, message, mode]);

  useEffect(() => { void reload(); }, [reload]);

  async function openPlan(plan: TrainingPlan) {
    setSelectedPlan(plan);
    try {
      const [resourceItems, planSummary] = await Promise.all([listTrainingPlanItems(plan.id), getTrainingSummary(plan.id)]);
      setItems(resourceItems);
      setSummary(planSummary);
      if (!canManage && currentUserId) {
        const [recordPage, feedbackPage] = await Promise.all([
          listTrainingRecords({ planId: plan.id, userId: currentUserId }),
          listTrainingFeedback({ planId: plan.id, userId: currentUserId }),
        ]);
        const record = recordPage.records[0] ?? null;
        setSelectedRecord(record);
        setSelectedHasFeedback(Boolean(record && feedbackPage.records.some((item) => item.trainingRecordId === record.id)));
      } else {
        setSelectedRecord(null);
        setSelectedHasFeedback(false);
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, "培训计划详情加载失败"));
    }
  }

  async function joinSelectedPlan() {
    if (!selectedPlan) throw new Error("Training plan is not selected");
    const record = await joinTrainingPlan(selectedPlan.id);
    setSelectedRecord(record);
    setSelectedHasFeedback(false);
    await reload();
    setSummary(await getTrainingSummary(selectedPlan.id));
    return record;
  }

  async function submitSelectedFeedback(values: FeedbackValues) {
    await createTrainingFeedback(values);
    setSelectedHasFeedback(true);
    message.success("Feedback submitted");
    await reload();
  }

  async function submitPlan(values: Record<string, unknown>) {
    if (!currentUserId) return;
    try {
      const courseIds = (values.courseIds as number[] | undefined) ?? [];
      const projectIds = (values.projectIds as number[] | undefined) ?? [];
      const baseIds = (values.baseIds as number[] | undefined) ?? [];
      const speciesIds = (values.speciesIds as number[] | undefined) ?? [];
      const peopleIds = (values.peopleIds as number[] | undefined) ?? [];
      const startedAt = values.startedAt as Dayjs | undefined;
      const endedAt = values.endedAt as Dayjs | undefined;
      const plan = await createTrainingPlan({
        planNo: `TRAIN-${Date.now()}`,
        planName: String(values.planName),
        planType: "course",
        ownerId: currentUserId,
        courseId: courseIds[0],
        startedAt: startedAt?.toISOString(),
        endedAt: endedAt?.toISOString(),
      });
      await Promise.all([
        ...courseIds.map((id, index) => createTrainingPlanItem(plan.id, { itemType: "course", itemTitle: `实验课程 ${id}`, courseId: id, sortOrder: index })),
        ...projectIds.map((id, index) => createTrainingPlanItem(plan.id, { itemType: "project", itemTitle: `课题研究 ${id}`, projectId: id, sortOrder: index })),
        ...baseIds.map((id, index) => createTrainingPlanItem(plan.id, { itemType: "base", itemTitle: `基地 ${id}`, baseId: id, sortOrder: index })),
        ...speciesIds.map((id, index) => createTrainingPlanItem(plan.id, { itemType: "species", itemTitle: `药材 ${id}`, speciesId: id, sortOrder: index })),
      ]);
      if (peopleIds.length) await addTrainingParticipants(plan.id, peopleIds);
      message.success("培训计划已创建");
      setPlanOpen(false);
      planForm.resetFields();
      await reload();
    } catch (error) {
      message.error(getApiErrorMessage(error, "培训计划创建失败"));
    }
  }

  if (mode === "plans") {
    const columns: ColumnsType<TrainingPlan> = [
      { title: "培训名称", dataIndex: "planName", render: (value: string, record) => <Button type="link" onClick={() => void openPlan(record)}>{value}</Button> },
      { title: "培训时间", render: (_, record) => `${record.startedAt?.slice(0, 10) ?? "-"} 至 ${record.endedAt?.slice(0, 10) ?? "-"}` },
      { title: "参与人数", dataIndex: "participantCount", render: (value?: number) => value ?? 0 },
      { title: "状态", dataIndex: "publishStatus", render: (value: string) => <Tag color="green">{statusLabels[value] ?? value}</Tag> },
    ];
    return <>
      <div className={styles.sectionToolbar}><strong>培训计划</strong><Space><Button icon={<ReloadOutlined />} onClick={() => void reload()}>刷新</Button>{canManage ? <Button type="primary" icon={<PlusOutlined />} onClick={() => setPlanOpen(true)}>新建计划</Button> : null}</Space></div>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={plans} pagination={{ pageSize: 8 }} />
      <PlanDetail plan={selectedPlan} items={items} summary={summary} canParticipate={!canManage} currentRecord={selectedRecord} hasFeedback={selectedHasFeedback} onClose={() => setSelectedPlan(null)} onJoin={joinSelectedPlan} onFeedback={submitSelectedFeedback} onPublish={async () => { if (!selectedPlan) return; await publishTrainingPlan(selectedPlan.id, selectedPlan.version); await reload(); }} />
      <PlanForm open={planOpen} form={planForm} onCancel={() => setPlanOpen(false)} onFinish={submitPlan} />
    </>;
  }

  if (mode === "records") {
    const ownRecords = currentUserId ? records.filter((record) => record.userId === currentUserId) : [];
    const columns: ColumnsType<TrainingRecord> = [
      { title: "培训计划", render: (_, record) => record.planName || `计划 ${record.planId}` },
      { title: "参与人", render: (_, record) => record.realName || record.userName || record.userId },
      { title: "参与时间", dataIndex: "createdAt", render: (value?: string) => value?.slice(0, 10) ?? "-" },
      { title: "参与次数", render: () => 1 },
    ];
    return <><div className={styles.sectionToolbar}><strong>培训记录</strong><Space><Statistic title="总参与人次" value={records.length} /><Statistic title="我的参与次数" value={ownRecords.length} /></Space></div><Table rowKey="id" loading={loading} columns={columns} dataSource={records} pagination={{ pageSize: 10 }} /></>;
  }

  const ownRecords = currentUserId ? records.filter((record) => record.userId === currentUserId) : [];
  const feedbackColumns: ColumnsType<TrainingFeedback> = [
    { title: "培训计划", render: (_, record) => record.planName || `计划 ${record.planId}` },
    { title: "评分", dataIndex: "rating", render: (value: number) => <Rate disabled value={value} /> },
    { title: "反馈内容", dataIndex: "feedbackContent", ellipsis: true },
    { title: "提交时间", dataIndex: "submittedAt", render: (value?: string) => value?.slice(0, 10) ?? "-" },
  ];
  return <>
    <div className={styles.sectionToolbar}><strong>培训反馈问卷</strong><Space><Button icon={<ReloadOutlined />} onClick={() => void reload()}>刷新</Button>{ownRecords.length ? <Button type="primary" onClick={() => setFeedbackOpen(true)}>填写问卷</Button> : null}</Space></div>
    <Table rowKey="id" loading={loading} columns={feedbackColumns} dataSource={feedback} pagination={{ pageSize: 10 }} />
    <FeedbackForm open={feedbackOpen} form={feedbackForm} records={ownRecords} onCancel={() => setFeedbackOpen(false)} onFinish={async (values) => { try { await createTrainingFeedback(values); message.success("反馈已提交"); setFeedbackOpen(false); feedbackForm.resetFields(); await reload(); } catch (error) { message.error(getApiErrorMessage(error, "反馈提交失败")); } }} />
  </>;
}

function PlanDetail({ plan, items, summary, canParticipate, currentRecord, hasFeedback, onClose, onJoin, onFeedback, onPublish }: { plan: TrainingPlan | null; items: TrainingPlanItem[]; summary: TrainingSummary | null; canParticipate: boolean; currentRecord: TrainingRecord | null; hasFeedback: boolean; onClose: () => void; onJoin: () => Promise<TrainingRecord>; onFeedback: (values: FeedbackValues) => Promise<void>; onPublish: () => Promise<void> }) {
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [feedbackForm] = Form.useForm<FeedbackValues>();
  const join = async () => {
    await onJoin();
    setFeedbackOpen(true);
  };
  return <Modal open={Boolean(plan)} title={plan?.planName ?? "培训计划详情"} footer={null} width={820} onCancel={onClose}>
    {plan ? <>
      <Card size="small"><Row gutter={16}><Col span={8}><Statistic title="参与人数" value={summary?.totalParticipantCount ?? plan.participantCount ?? 0} /></Col><Col span={8}><Statistic title="关联资源" value={items.length} /></Col><Col span={8}><Statistic title="反馈数量" value={summary?.feedbackCount ?? 0} /></Col></Row></Card>
      <p>培训时间：{plan.startedAt?.slice(0, 10) ?? "-"} 至 {plan.endedAt?.slice(0, 10) ?? "-"}</p>
      <h4>关联课程、课题、基地和药材</h4><Space wrap>{items.map((item) => <Tag key={item.id}>{item.itemTitle}</Tag>)}</Space>
      {canParticipate && plan.publishStatus === "published" ? <div style={{ marginTop: 20 }}><Space><Button type="primary" onClick={() => void (currentRecord ? setFeedbackOpen(true) : join())}>{currentRecord ? "填写培训反馈" : "参与培训"}</Button>{currentRecord ? <Tag color="green">已提交参与</Tag> : null}{hasFeedback ? <Tag>已提交反馈</Tag> : null}</Space></div> : null}
      {plan.publishStatus === "draft" ? <div style={{ marginTop: 20 }}><Button type="primary" onClick={() => void onPublish()}>发布计划</Button></div> : null}
      <FeedbackForm open={feedbackOpen} form={feedbackForm} records={currentRecord ? [currentRecord] : []} onCancel={() => setFeedbackOpen(false)} onFinish={async (values) => { await onFeedback(values); setFeedbackOpen(false); feedbackForm.resetFields(); }} />
      {currentRecord && canParticipate && plan.publishStatus === "published" ? <div style={{ marginTop: 12 }}><Button type="primary" onClick={() => void join()}>再次参加培训</Button></div> : null}
    </> : null}
  </Modal>;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function LegacyPlanDetail({ plan, items, summary, onClose, onPublish }: { plan: TrainingPlan | null; items: TrainingPlanItem[]; summary: TrainingSummary | null; onClose: () => void; onPublish: () => Promise<void> }) {
  return <Modal open={Boolean(plan)} title={plan?.planName ?? "培训计划详情"} footer={null} width={820} onCancel={onClose}>{plan ? <><Card size="small"><Row gutter={16}><Col span={8}><Statistic title="参与人数" value={summary?.totalParticipantCount ?? plan.participantCount ?? 0} /></Col><Col span={8}><Statistic title="关联资源" value={items.length} /></Col><Col span={8}><Statistic title="反馈数量" value={summary?.feedbackCount ?? 0} /></Col></Row></Card><p>培训时间：{plan.startedAt?.slice(0, 10) ?? "-"} 至 {plan.endedAt?.slice(0, 10) ?? "-"}</p><h4>关联课程、课题、基地和药材</h4><Space wrap>{items.map((item) => <Tag key={item.id}>{item.itemTitle}</Tag>)}</Space>{plan.publishStatus === "draft" ? <div style={{ marginTop: 20 }}><Button type="primary" onClick={() => void onPublish()}>发布计划</Button></div> : null}</> : null}</Modal>;
}

function PlanForm({ open, form, onCancel, onFinish }: { open: boolean; form: FormInstance<Record<string, unknown>>; onCancel: () => void; onFinish: (values: Record<string, unknown>) => void }) {
  const [courses, setCourses] = useState<Array<{ id: number; name: string }>>([]);
  const [projects, setProjects] = useState<ResearchProjectApi[]>([]);
  const [bases, setBases] = useState<HerbBaseApi[]>([]);
  const [species, setSpecies] = useState<HerbSpeciesApi[]>([]);
  const [users, setUsers] = useState<ResearchUserCandidateApi[]>([]);

  useEffect(() => {
    if (!open) return;
    void Promise.all([listCourses(), listResearchProjects(), fetchHerbBases({ page: 1, size: 100 }), fetchEnabledHerbs(), listResearchUsers()])
      .then(([coursePage, projectPage, basePage, herbList, userList]) => {
        setCourses(coursePage.records.map((item) => ({ id: Number(item.id), name: item.courseName })));
        setProjects(projectPage.records);
        setBases(basePage.records);
        setSpecies(herbList);
        setUsers(userList);
      })
      .catch(() => undefined);
  }, [open]);

  return <Modal open={open} title="新建培训计划" onCancel={onCancel} onOk={() => form.submit()}>
    <Form form={form} layout="vertical" onFinish={onFinish}>
      <Form.Item name="planName" label="培训名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Row gutter={12}>
        <Col span={12}><Form.Item name="startedAt" label="开始时间" rules={[{ required: true }]}><DatePicker showTime style={{ width: "100%" }} /></Form.Item></Col>
        <Col span={12}><Form.Item name="endedAt" label="结束时间" rules={[{ required: true }]}><DatePicker showTime style={{ width: "100%" }} /></Form.Item></Col>
      </Row>
      <Form.Item name="courseIds" label="实验课程"><Select mode="multiple" options={courses.map((item) => ({ label: item.name, value: item.id }))} /></Form.Item>
      <Form.Item name="projectIds" label="课题研究"><Select mode="multiple" options={projects.map((item) => ({ label: item.projectName, value: item.id }))} /></Form.Item>
      <Form.Item name="baseIds" label="基地"><Select mode="multiple" options={bases.map((item) => ({ label: item.baseName, value: item.id }))} /></Form.Item>
      <Form.Item name="speciesIds" label="药材"><Select mode="multiple" options={species.map((item) => ({ label: item.herbName, value: item.id }))} /></Form.Item>
      <Form.Item name="peopleIds" label="参与老师和学生"><Select mode="multiple" options={users.map((item) => ({ label: item.realName || item.username, value: item.id }))} /></Form.Item>
    </Form>
  </Modal>;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function LegacyPlanForm({ open, form, onCancel, onFinish }: { open: boolean; form: FormInstance<Record<string, unknown>>; onCancel: () => void; onFinish: (values: Record<string, unknown>) => void }) {
  const [courses, setCourses] = useState<Array<{ id: number; name: string }>>([]);
  const [projects, setProjects] = useState<ResearchProjectApi[]>([]);
  const [bases, setBases] = useState<HerbBaseApi[]>([]);
  const [species, setSpecies] = useState<HerbSpeciesApi[]>([]);
  const [users, setUsers] = useState<ResearchUserCandidateApi[]>([]);
  useEffect(() => {
    if (!open) return;
    void Promise.all([listCourses(), listResearchProjects(), fetchHerbBases({ page: 1, size: 100 }), fetchEnabledHerbs(), listResearchUsers()])
      .then(([coursePage, projectPage, basePage, herbList, userList]) => {
        setCourses(coursePage.records.map((item) => ({ id: Number(item.id), name: item.courseName })));
        setProjects(projectPage.records);
        setBases(basePage.records);
        setSpecies(herbList);
        setUsers(userList);
      }).catch(() => undefined);
  }, [open]);
  return <Modal open={open} title="新建培训计划" onCancel={onCancel} onOk={() => form.submit()}>
    <Form form={form} layout="vertical" onFinish={onFinish}>
      <Form.Item name="planName" label="培训名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Row gutter={12}><Col span={12}><Form.Item name="startedAt" label="开始时间" rules={[{ required: true }]}><DatePicker showTime style={{ width: "100%" }} onChange={(value) => form.setFieldValue("startedAt", value?.toISOString())} /></Form.Item></Col><Col span={12}><Form.Item name="endedAt" label="结束时间" rules={[{ required: true }]}><DatePicker showTime style={{ width: "100%" }} onChange={(value) => form.setFieldValue("endedAt", value?.toISOString())} /></Form.Item></Col></Row>
      <Form.Item name="courseIds" label="实验课程"><Select mode="multiple" options={courses.map((item) => ({ label: item.name, value: item.id }))} /></Form.Item>
      <Form.Item name="projectIds" label="课题研究"><Select mode="multiple" options={projects.map((item) => ({ label: item.projectName, value: item.id }))} /></Form.Item>
      <Form.Item name="baseIds" label="基地"><Select mode="multiple" options={bases.map((item) => ({ label: item.baseName, value: item.id }))} /></Form.Item>
      <Form.Item name="speciesIds" label="药材"><Select mode="multiple" options={species.map((item) => ({ label: item.herbName, value: item.id }))} /></Form.Item>
      <Form.Item name="peopleIds" label="参与老师和学生"><Select mode="multiple" options={users.map((item) => ({ label: item.realName || item.username, value: item.id }))} /></Form.Item>
    </Form>
  </Modal>;
}

function FeedbackForm({ open, form, records, onCancel, onFinish }: { open: boolean; form: FormInstance<FeedbackValues>; records: TrainingRecord[]; onCancel: () => void; onFinish: (values: FeedbackValues) => void }) {
  return <Modal open={open} title="培训反馈问卷" onCancel={onCancel} onOk={() => form.submit()}><Form form={form} layout="vertical" onFinish={onFinish}><Form.Item name="trainingRecordId" label="培训计划" rules={[{ required: true }]}><Select options={records.map((record) => ({ label: record.planName || `计划 ${record.planId}`, value: record.id }))} /></Form.Item><Form.Item name="rating" label="总体满意度" rules={[{ required: true }]}><Rate /></Form.Item><Form.Item name="feedbackContent" label="反馈建议"><Input.TextArea rows={5} maxLength={500} showCount /></Form.Item></Form></Modal>;
}
