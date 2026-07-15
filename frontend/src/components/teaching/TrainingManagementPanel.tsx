"use client";

import {
  DeleteOutlined,
  LinkOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { App, Button, Card, Drawer, Form, Input, InputNumber, Modal, Progress, Select, Space, Statistic, Table, Tag, Upload } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useCallback, useEffect, useState } from "react";
import { uploadFile } from "@/lib/files";
import { getApiErrorMessage } from "@/lib/request";
import {
  addTrainingParticipants,
  bindPlanMaterial,
  closeTrainingPlan,
  createTrainingMaterial,
  createTrainingPlan,
  createTrainingFeedback,
  deleteTrainingPlan,
  getTrainingSummary,
  getTrainingPlan,
  getTrainingCompletionProof,
  listPlanMaterials,
  listTrainingFeedback,
  listTrainingMaterials,
  listTrainingPlans,
  listTrainingRecords,
  publishTrainingPlan,
  unbindPlanMaterial,
  updateTrainingRecord,
  type TrainingFeedback,
  type TrainingMaterial,
  type TrainingPlan,
  type TrainingPlanMaterial,
  type TrainingRecord,
  type TrainingSummary,
} from "@/lib/training";
import styles from "./teaching.module.css";

type TrainingManagementPanelProps = {
  canManage?: boolean;
  currentUserId?: number;
};

const statusLabels: Record<string, string> = { draft: "草稿", published: "已发布", closed: "已关闭" };
const attendanceLabels: Record<string, string> = { pending: "待签到", present: "已签到", late: "迟到", absent: "缺勤", leave: "请假" };
const trainingStatusLabels: Record<string, string> = { not_started: "未开始", learning: "学习中", completed: "已完成", failed: "未通过", makeup: "待补训" };

function displayName(record: { realName?: string; userName?: string; userId: number }) {
  return record.realName || record.userName || `用户 ${record.userId}`;
}

export function TrainingManagementPanel({ canManage = false, currentUserId }: TrainingManagementPanelProps) {
  const { message, modal } = App.useApp();
  const [plans, setPlans] = useState<TrainingPlan[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<TrainingPlan | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [materials, setMaterials] = useState<TrainingPlanMaterial[]>([]);
  const [availableMaterials, setAvailableMaterials] = useState<TrainingMaterial[]>([]);
  const [records, setRecords] = useState<TrainingRecord[]>([]);
  const [feedback, setFeedback] = useState<TrainingFeedback[]>([]);
  const [summary, setSummary] = useState<TrainingSummary | null>(null);
  const [planModalOpen, setPlanModalOpen] = useState(false);
  const [materialModalOpen, setMaterialModalOpen] = useState(false);
  const [participantModalOpen, setParticipantModalOpen] = useState(false);
  const [feedbackModalOpen, setFeedbackModalOpen] = useState(false);
  const [planForm] = Form.useForm();
  const [materialForm] = Form.useForm();
  const [participantForm] = Form.useForm();
  const [feedbackForm] = Form.useForm();

  const reloadPlans = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listTrainingPlans();
      setPlans(result.records);
    } catch (error) {
      message.error(getApiErrorMessage(error, "培训计划加载失败"));
    } finally {
      setLoading(false);
    }
  }, [message]);

  const reloadPlanData = useCallback(async (plan: TrainingPlan) => {
    try {
      const [detail, planMaterials, recordPage, feedbackPage, planSummary] = await Promise.all([
        getTrainingPlan(plan.id),
        listPlanMaterials(plan.id),
        listTrainingRecords({ planId: plan.id }),
        listTrainingFeedback({ planId: plan.id }),
        getTrainingSummary(plan.id),
      ]);
      setSelectedPlan(detail);
      setMaterials(planMaterials);
      setRecords(recordPage.records);
      setFeedback(feedbackPage.records);
      setSummary(planSummary);
    } catch (error) {
      message.error(getApiErrorMessage(error, "培训计划详情加载失败"));
    }
  }, [message]);

  useEffect(() => { void reloadPlans(); }, [reloadPlans]);

  async function openPlan(plan: TrainingPlan) {
    setDrawerOpen(true);
    await reloadPlanData(plan);
  }

  async function handleCreatePlan(values: Record<string, unknown>) {
    try {
      await createTrainingPlan({
        planNo: String(values.planNo),
        planName: String(values.planName),
        planType: String(values.planType),
        ownerId: Number(values.ownerId),
        trainerId: values.trainerId ? Number(values.trainerId) : undefined,
        courseId: values.courseId ? Number(values.courseId) : undefined,
        location: values.location ? String(values.location) : undefined,
        description: values.description ? String(values.description) : undefined,
      });
      message.success("培训计划已创建");
      setPlanModalOpen(false);
      planForm.resetFields();
      await reloadPlans();
    } catch (error) { message.error(getApiErrorMessage(error, "培训计划创建失败")); }
  }

  async function handlePublish() {
    if (!selectedPlan) return;
    try { await publishTrainingPlan(selectedPlan.id, selectedPlan.version); message.success("培训计划已发布"); await reloadPlans(); await reloadPlanData(selectedPlan); }
    catch (error) { message.error(getApiErrorMessage(error, "发布失败，请先关联课程或素材")); }
  }

  async function handleClose() {
    if (!selectedPlan) return;
    try { await closeTrainingPlan(selectedPlan.id, selectedPlan.version, "前端关闭培训计划"); message.success("培训计划已关闭"); await reloadPlans(); await reloadPlanData(selectedPlan); }
    catch (error) { message.error(getApiErrorMessage(error, "关闭失败")); }
  }

  function confirmDelete(plan: TrainingPlan) {
    modal.confirm({ title: "删除培训计划", content: `确定删除“${plan.planName}”吗？仅草稿且未关联学员的计划可以删除。`, okText: "删除", okButtonProps: { danger: true }, cancelText: "取消", onOk: async () => {
      try { await deleteTrainingPlan(plan.id); message.success("培训计划已删除"); await reloadPlans(); if (selectedPlan?.id === plan.id) setDrawerOpen(false); }
      catch (error) { message.error(getApiErrorMessage(error, "删除失败")); }
    } });
  }

  async function handleCreateMaterial(values: Record<string, unknown>, file?: File) {
    try {
      if (!file) throw new Error("请选择素材文件");
      const uploaded = await uploadFile(file, { fileUsage: "training_material" });
      await createTrainingMaterial({ materialNo: String(values.materialNo), materialName: String(values.materialName), materialType: String(values.materialType), fileId: uploaded.id, sourceType: "upload", description: values.description ? String(values.description) : undefined });
      message.success("培训素材已创建");
      setMaterialModalOpen(false);
      materialForm.resetFields();
      if (selectedPlan) await reloadPlanData(selectedPlan);
    } catch (error) { message.error(getApiErrorMessage(error, "培训素材创建失败")); }
  }

  async function handleBindMaterial(materialId: number) {
    if (!selectedPlan) return;
    try { await bindPlanMaterial(selectedPlan.id, { materialId, isRequired: 1, sortOrder: materials.length }); message.success("素材已绑定"); await reloadPlanData(selectedPlan); }
    catch (error) { message.error(getApiErrorMessage(error, "素材绑定失败")); }
  }

  async function handleAddParticipants(values: { userIds: string }) {
    if (!selectedPlan) return;
    const userIds = values.userIds.split(/[，,\s]+/).filter(Boolean).map(Number).filter(Number.isInteger);
    if (!userIds.length) { message.warning("请输入有效的用户 ID"); return; }
    try { const result = await addTrainingParticipants(selectedPlan.id, userIds); message.success(`已添加 ${result.successCount} 人，重复 ${result.duplicateCount} 人，失败 ${result.failureCount} 人`); setParticipantModalOpen(false); participantForm.resetFields(); await reloadPlanData(selectedPlan); }
    catch (error) { message.error(getApiErrorMessage(error, "批量添加学员失败")); }
  }

  async function saveRecord(record: TrainingRecord, values: Record<string, unknown>) {
    try { await updateTrainingRecord(record.id, { attendanceStatus: String(values.attendanceStatus), trainingStatus: String(values.trainingStatus), progress: Number(values.progress), score: values.score === undefined || values.score === null ? undefined : Number(values.score), resultComment: values.resultComment ? String(values.resultComment) : undefined }); message.success("培训记录已更新"); if (selectedPlan) await reloadPlanData(selectedPlan); }
    catch (error) { message.error(getApiErrorMessage(error, "培训记录更新失败")); }
  }

  async function handleCreateFeedback(values: { rating: number; feedbackContent?: string }) {
    const ownRecord = records.find((record) => record.userId === currentUserId);
    if (!ownRecord) { message.warning("当前账号尚未加入该培训计划"); return; }
    try { await createTrainingFeedback({ trainingRecordId: ownRecord.id, rating: values.rating, feedbackContent: values.feedbackContent }); message.success("反馈已提交"); setFeedbackModalOpen(false); feedbackForm.resetFields(); if (selectedPlan) await reloadPlanData(selectedPlan); }
    catch (error) { message.error(getApiErrorMessage(error, "反馈提交失败")); }
  }

  const planColumns: ColumnsType<TrainingPlan> = [
    { title: "计划名称", dataIndex: "planName", render: (value: string, record) => <Button type="link" className={styles.actionLink} onClick={() => void openPlan(record)}>{value}</Button> },
    { title: "负责人", render: (_, record) => record.ownerName || record.ownerId },
    { title: "培训时间", render: (_, record) => `${record.startedAt || "未设置"} - ${record.endedAt || "未设置"}` },
    { title: "学员", dataIndex: "participantCount", render: (value?: number) => value ?? 0 },
    { title: "状态", dataIndex: "publishStatus", render: (value: string) => <Tag color={value === "published" ? "green" : value === "closed" ? "default" : "blue"}>{statusLabels[value] || value}</Tag> },
    { title: "操作", key: "actions", render: (_, record) => <Space size={0}><Button type="link" className={styles.actionLink} onClick={() => void openPlan(record)}>查看</Button>{canManage && record.publishStatus === "draft" ? <Button type="link" className={styles.actionLink} onClick={() => confirmDelete(record)}>删除</Button> : null}</Space> },
  ];

  const recordColumns: ColumnsType<TrainingRecord> = [
    { title: "学员", render: (_, record) => displayName(record) },
    { title: "签到", dataIndex: "attendanceStatus", render: (value?: string) => attendanceLabels[value || "pending"] || value },
    { title: "进度", dataIndex: "progress", render: (value?: number) => <Progress percent={value ?? 0} size="small" /> },
    { title: "成绩", dataIndex: "score", render: (value?: number) => value ?? "未评分" },
    { title: "状态", dataIndex: "trainingStatus", render: (value?: string) => trainingStatusLabels[value || "not_started"] || value },
    { title: "操作", render: (_, record) => <Button type="link" className={styles.actionLink} onClick={() => { Modal.confirm({ title: `更新 ${displayName(record)} 的培训记录`, content: <RecordEditor record={record} onSave={(values) => void saveRecord(record, values)} />, icon: null, width: 520, okButtonProps: { style: { display: "none" } }, cancelButtonProps: { style: { display: "none" } } }); }}>编辑</Button> },
  ];

  const ownCompletedRecord = records.find((record) => record.userId === currentUserId && record.trainingStatus === "completed");
  const openCompletionProof = () => {
    if (!ownCompletedRecord) return;
    void getTrainingCompletionProof(ownCompletedRecord.id).then((proof) => {
      if (!proof.completionProofFileId) { message.info(proof.message || "完成条件尚未满足"); return; }
      window.open(`${window.location.origin}/api/files/${proof.completionProofFileId}/content?disposition=attachment`, "_blank", "noopener,noreferrer");
    }).catch((error) => message.error(getApiErrorMessage(error, "完成证明生成失败")));
  };

  return <section className={styles.trainingManagementPanel}>
    {ownCompletedRecord ? <Button onClick={openCompletionProof}>查看完成证明</Button> : null}
    <div className={styles.sectionToolbar}>
      <strong>培训计划管理</strong>
      <Space><Button icon={<ReloadOutlined />} onClick={() => void reloadPlans()}>刷新</Button>{canManage ? <Button type="primary" icon={<PlusOutlined />} onClick={() => setPlanModalOpen(true)}>新建计划</Button> : null}</Space>
    </div>
    <Table rowKey="id" loading={loading} columns={planColumns} dataSource={plans} pagination={{ pageSize: 8 }} className={styles.dataTable} scroll={{ x: 900 }} />

    <Drawer title={selectedPlan?.planName || "培训计划详情"} width={760} open={drawerOpen} onClose={() => setDrawerOpen(false)} extra={selectedPlan ? <Space>{canManage && selectedPlan.publishStatus === "draft" ? <Button type="primary" onClick={() => void handlePublish()}>发布</Button> : null}{canManage && selectedPlan.publishStatus === "published" ? <Button onClick={() => void handleClose()}>关闭计划</Button> : null}</Space> : null}>
      {selectedPlan ? <div className={styles.trainingDetailBody}>
        <Card size="small" title="基本信息"><div className={styles.trainingDetailGrid}><span>计划编号：{selectedPlan.planNo}</span><span>负责人：{selectedPlan.ownerName || selectedPlan.ownerId}</span><span>培训师：{selectedPlan.trainerName || selectedPlan.trainerId || "未设置"}</span><span>地点：{selectedPlan.location || "未设置"}</span><span>状态：{statusLabels[selectedPlan.publishStatus]}</span><span>关联素材：{materials.length} 个</span></div><p>{selectedPlan.description || "暂无计划说明"}</p></Card>
        <Card size="small" title="统计汇总" className={styles.trainingStatsCard}>{summary ? <div className={styles.trainingStatsGrid}><Statistic title="参与人数" value={summary.totalParticipantCount} /><Statistic title="已完成" value={summary.completedCount} /><Statistic title="平均进度" value={summary.averageProgress} suffix="%" /><Statistic title="平均成绩" value={summary.averageScore ?? "-"} /><Statistic title="反馈数" value={summary.feedbackCount} /><Statistic title="平均评分" value={summary.averageRating ?? "-"} suffix={summary.averageRating ? "/5" : ""} /></div> : "暂无统计数据"}</Card>
        <div className={styles.trainingDetailSection}><div className={styles.recordSectionHeading}><strong>培训素材</strong><Space>{canManage ? <Button size="small" icon={<UploadOutlined />} onClick={() => setMaterialModalOpen(true)}>上传素材</Button> : null}<Select size="small" placeholder="绑定已有素材" style={{ width: 220 }} options={availableMaterials.filter((item) => !materials.some((bound) => bound.id === item.id)).map((item) => ({ label: item.materialName, value: item.id }))} onDropdownVisibleChange={(open) => { if (open) void listTrainingMaterials().then((result) => setAvailableMaterials(result.records)); }} onChange={(value: number) => void handleBindMaterial(value)} /></Space></div>{materials.length ? materials.map((item) => <div className={styles.trainingMaterialRow} key={item.id}><LinkOutlined /><span>{item.materialName}</span><small>{item.originalFilename || item.materialType}</small>{canManage && selectedPlan.publishStatus === "draft" ? <Button danger type="text" icon={<DeleteOutlined />} onClick={() => void unbindPlanMaterial(selectedPlan.id, item.id).then(() => reloadPlanData(selectedPlan))} /> : null}</div>) : <span className={styles.mutedText}>暂未绑定素材</span>}</div>
        <div className={styles.trainingDetailSection}><div className={styles.recordSectionHeading}><strong>学员签到 / 进度 / 成绩</strong>{canManage ? <Button size="small" onClick={() => setParticipantModalOpen(true)}>批量添加学员</Button> : null}</div><Table rowKey="id" size="small" columns={recordColumns} dataSource={records} pagination={{ pageSize: 6 }} scroll={{ x: 650 }} /></div>
        <div className={styles.trainingDetailSection}><div className={styles.recordSectionHeading}><strong>学员反馈</strong><Space><span className={styles.mutedText}>{feedback.length} 条</span>{!canManage && currentUserId ? <Button size="small" onClick={() => setFeedbackModalOpen(true)}>提交反馈</Button> : null}</Space></div>{feedback.length ? feedback.map((item) => <div className={styles.trainingFeedbackRow} key={item.id}><strong>{item.userName || item.userId}</strong><Tag color="gold">{item.rating}/5</Tag><span>{item.feedbackContent || "暂无文字反馈"}</span></div>) : <span className={styles.mutedText}>暂无反馈</span>}</div>
      </div> : <span className={styles.mutedText}>正在加载计划详情…</span>}
    </Drawer>

    <Modal title="新建培训计划" open={planModalOpen} onCancel={() => setPlanModalOpen(false)} onOk={() => planForm.submit()} okText="保存计划"><Form form={planForm} layout="vertical" onFinish={(values) => void handleCreatePlan(values)}><Form.Item name="planNo" label="计划编号" rules={[{ required: true }]}><Input placeholder="TRAIN-2026-001" /></Form.Item><Form.Item name="planName" label="计划名称" rules={[{ required: true }]}><Input /></Form.Item><div className={styles.trainingFormRow}><Form.Item name="planType" label="计划类型" rules={[{ required: true }]}><Select options={[{ label: "集中培训", value: "offline" }, { label: "在线培训", value: "online" }, { label: "混合培训", value: "hybrid" }]} /></Form.Item><Form.Item name="ownerId" label="负责人用户 ID" rules={[{ required: true }]}><InputNumber min={1} style={{ width: "100%" }} /></Form.Item><Form.Item name="trainerId" label="培训师用户 ID"><InputNumber min={1} style={{ width: "100%" }} /></Form.Item></div><Form.Item name="location" label="培训地点"><Input /></Form.Item><Form.Item name="description" label="计划说明"><Input.TextArea rows={3} /></Form.Item></Form></Modal>
    <Modal title="上传培训素材" open={materialModalOpen} onCancel={() => setMaterialModalOpen(false)} onOk={() => materialForm.submit()} okText="上传并保存"><MaterialForm form={materialForm} onFinish={handleCreateMaterial} /></Modal>
    <Modal title="批量添加学员" open={participantModalOpen} onCancel={() => setParticipantModalOpen(false)} onOk={() => participantForm.submit()} okText="添加学员"><Form form={participantForm} layout="vertical" onFinish={(values) => void handleAddParticipants(values)}><Form.Item name="userIds" label="学员用户 ID" extra="输入用户 ID，多个 ID 用逗号或空格分隔" rules={[{ required: true }]}><Input.TextArea rows={3} placeholder="例如：101,102,103" /></Form.Item></Form></Modal>
    <Modal title="提交培训反馈" open={feedbackModalOpen} onCancel={() => setFeedbackModalOpen(false)} onOk={() => feedbackForm.submit()} okText="提交反馈"><Form form={feedbackForm} layout="vertical" onFinish={(values) => void handleCreateFeedback(values)}><Form.Item name="rating" label="评分" rules={[{ required: true }]}><Select options={[1, 2, 3, 4, 5].map((value) => ({ value, label: `${value} 分` }))} /></Form.Item><Form.Item name="feedbackContent" label="反馈内容"><Input.TextArea rows={4} maxLength={500} showCount /></Form.Item></Form></Modal>
  </section>;
}

function MaterialForm({ form, onFinish }: { form: ReturnType<typeof Form.useForm>[0]; onFinish: (values: Record<string, unknown>, file?: File) => void }) {
  const [file, setFile] = useState<File>();
  return <Form form={form} layout="vertical" onFinish={(values) => onFinish(values as Record<string, unknown>, file)}><Form.Item name="materialNo" label="素材编号" rules={[{ required: true }]}><Input /></Form.Item><Form.Item name="materialName" label="素材名称" rules={[{ required: true }]}><Input /></Form.Item><Form.Item name="materialType" label="素材类型" rules={[{ required: true }]}><Select options={[{ label: "课件", value: "courseware" }, { label: "视频", value: "video" }, { label: "文档", value: "document" }, { label: "其他", value: "other" }]} /></Form.Item><Form.Item label="文件" required><Upload beforeUpload={(nextFile) => { setFile(nextFile); return false; }} maxCount={1} accept=".pdf,.ppt,.pptx,.doc,.docx,.mp4,.webm"><Button icon={<UploadOutlined />}>选择文件</Button></Upload></Form.Item><Form.Item name="description" label="素材说明"><Input.TextArea rows={3} /></Form.Item></Form>;
}

function RecordEditor({ record, onSave }: { record: TrainingRecord; onSave: (values: Record<string, unknown>) => void }) {
  const [form] = Form.useForm();
  return <Form form={form} layout="vertical" initialValues={{ attendanceStatus: record.attendanceStatus || "pending", trainingStatus: record.trainingStatus || "not_started", progress: record.progress ?? 0, score: record.score }} onFinish={onSave}><Form.Item name="attendanceStatus" label="签到状态"><Select options={Object.entries(attendanceLabels).map(([value, label]) => ({ value, label }))} /></Form.Item><Form.Item name="trainingStatus" label="培训状态"><Select options={Object.entries(trainingStatusLabels).map(([value, label]) => ({ value, label }))} /></Form.Item><Form.Item name="progress" label="学习进度"><InputNumber min={0} max={100} addonAfter="%" style={{ width: "100%" }} /></Form.Item><Form.Item name="score" label="成绩"><InputNumber min={0} max={100} style={{ width: "100%" }} /></Form.Item><Form.Item name="resultComment" label="结果备注"><Input.TextArea rows={3} /></Form.Item><Button type="primary" icon={<SaveOutlined />} onClick={() => form.submit()}>保存</Button></Form>;
}
