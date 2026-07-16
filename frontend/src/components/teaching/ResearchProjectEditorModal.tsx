"use client";

import { DeleteOutlined, FileAddOutlined, PlusOutlined } from "@ant-design/icons";
import { App, Button, Form, Input, Modal, Select, Space, Tabs, Upload } from "antd";
import type { RcFile, UploadProps } from "antd/es/upload/interface";
import { useEffect, useState } from "react";
import { uploadFile } from "@/lib/files";
import type { HerbSpeciesApi } from "@/lib/herbs";
import {
  addResearchMember,
  bindResearchMaterial,
  changeResearchLeader,
  createResearchAchievement,
  createResearchProject,
  getApiErrorMessage,
  getResearchProject,
  removeResearchMaterial,
  removeResearchMember,
  updateResearchAchievement,
  updateResearchProject,
  type ResearchProjectDetailApi,
  type ResearchUserCandidateApi,
} from "@/lib/research";
import type { ResearchRecord } from "./types";

type ProjectForm = { projectNo: string; projectName: string; projectType: string; leaderId: number; speciesId?: number; description?: string; startedAt?: string; endedAt?: string; remark?: string };
type ResearchProjectEditorModalProps = { open: boolean; project: ResearchRecord | null; users: ResearchUserCandidateApi[]; herbs: HerbSpeciesApi[]; canManage?: boolean; onCancel: () => void; onSaved: (detail: ResearchProjectDetailApi) => void };

export function ResearchProjectEditorModal({ open, project, users, herbs, canManage = true, onCancel, onSaved }: ResearchProjectEditorModalProps) {
  const { message } = App.useApp();
  const [form] = Form.useForm<ProjectForm>();
  const [activeTab, setActiveTab] = useState("basic");
  const [detail, setDetail] = useState<ResearchProjectDetailApi | null>(null);
  const [members, setMembers] = useState<ResearchProjectDetailApi["members"]>([]);
  const [materials, setMaterials] = useState<ResearchProjectDetailApi["materials"]>([]);
  const [achievements, setAchievements] = useState<ResearchProjectDetailApi["achievements"]>([]);
  const [memberForm] = Form.useForm<{ userId: number; memberRole: string }>();
  const [achievementForm] = Form.useForm<{ achievementNo: string; achievementName: string; achievementType: string; achievementStage?: string; description?: string }>();
  const [pendingMaterials, setPendingMaterials] = useState<File[]>([]);
  const [pendingAchievementFile, setPendingAchievementFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const setLeaderId = () => undefined;

  useEffect(() => {
    if (!open) return;
    setActiveTab("basic");
    setDetail(null);
    setPendingMaterials([]);
    setPendingAchievementFile(null);
    if (!project) {
      form.resetFields();
      setMembers([]); setMaterials([]); setAchievements([]);
      return;
    }
    const load = async () => {
      try {
        const loaded = await getResearchProject(Number(project.id));
        setDetail(loaded); setMembers(loaded.members); setMaterials(loaded.materials); setAchievements(loaded.achievements);
        form.setFieldsValue({ projectNo: loaded.projectNo, projectName: loaded.projectName, projectType: loaded.projectType, leaderId: loaded.leaderId, speciesId: loaded.speciesId, description: loaded.description, startedAt: loaded.startedAt?.slice(0, 10), endedAt: loaded.endedAt?.slice(0, 10), remark: loaded.remark });
      } catch (error) { message.error(getApiErrorMessage(error, "课题详情加载失败")); }
    };
    void load();
  }, [form, message, open, project]);

  const leaderOptions = users.filter((user) => user.status === 1 && ["teacher", "researcher"].includes((user.userType ?? "").toLowerCase())).map((user) => ({ value: user.id, label: user.realName ? `${user.realName}（${user.username}）` : user.username }));
  const memberOptions = users.filter((user) => user.status === 1).map((user) => ({ value: user.id, label: user.realName ? `${user.realName}（${user.username}）` : user.username }));

  async function saveBasic(values: ProjectForm) {
    setSaving(true);
    try {
      const dateTime = (value?: string) => value && value.length === 10 ? `${value}T00:00:00` : value;
      const payload = { ...values, startedAt: dateTime(values.startedAt), endedAt: dateTime(values.endedAt) };
      const saved = project && detail ? await updateResearchProject(Number(project.id), { ...payload, version: detail.version }) : await createResearchProject(payload);
      let latest = saved;
      if (project && detail && detail.leaderId !== values.leaderId) {
        latest = await changeResearchLeader(Number(project.id), { newLeaderId: values.leaderId, reason: "前端编辑课题负责人", version: saved.version });
      }
      for (const file of pendingMaterials) {
        const uploaded = await uploadFile(file, { bizType: "research_project", bizId: latest.id, fileUsage: "material" });
        await bindResearchMaterial(latest.id, { fileId: uploaded.id, fileUsage: "material" });
      }
      setPendingMaterials([]);
      const refreshed = await getResearchProject(latest.id);
      message.success(project ? "课题已更新" : "课题已创建");
      onSaved(refreshed);
    } catch (error) { message.error(getApiErrorMessage(error, "保存课题失败")); }
    finally { setSaving(false); }
  }

  async function addMember(values: { userId: number; memberRole: string }) {
    if (!detail) return message.info("请先保存课题基本信息");
    try { await addResearchMember(detail.id, values); setMembers(await (await getResearchProject(detail.id)).members); memberForm.resetFields(); message.success("成员已添加"); }
    catch (error) { message.error(getApiErrorMessage(error, "添加成员失败")); }
  }

  async function removeMember(userId: number) {
    if (!detail) return;
    try { await removeResearchMember(detail.id, userId); setMembers((items) => items.map((item) => item.userId === userId ? { ...item, memberStatus: "left" } : item)); message.success("成员已移除"); }
    catch (error) { message.error(getApiErrorMessage(error, "移除成员失败")); }
  }

  async function addAchievement(values: { achievementNo: string; achievementName: string; achievementType: string; achievementStage?: string; description?: string }) {
    if (!detail) return message.info("请先保存课题基本信息");
    try {
      let fileId: number | undefined;
      if (pendingAchievementFile) fileId = (await uploadFile(pendingAchievementFile, { bizType: "research_project", bizId: detail.id, fileUsage: "achievement" })).id;
      await createResearchAchievement({ ...values, projectId: detail.id, fileId });
      const refreshed = await getResearchProject(detail.id); setAchievements(refreshed.achievements); achievementForm.resetFields(); setPendingAchievementFile(null); message.success("阶段成果已添加");
    } catch (error) { message.error(getApiErrorMessage(error, "添加阶段成果失败")); }
  }

  async function submitAchievement(item: ResearchProjectDetailApi["achievements"][number]) {
    try { await updateResearchAchievement(item.id, { achievementName: item.achievementName, achievementType: item.achievementType, achievementStage: item.achievementStage, achievementStatus: "submitted", version: item.version }); setAchievements((items) => items.map((row) => row.id === item.id ? { ...row, achievementStatus: "submitted" } : row)); message.success("成果已提交"); }
    catch (error) { message.error(getApiErrorMessage(error, "提交成果失败")); }
  }

  async function confirmAchievement(item: ResearchProjectDetailApi["achievements"][number]) {
    try { await updateResearchAchievement(item.id, { achievementName: item.achievementName, achievementType: item.achievementType, achievementStage: item.achievementStage, achievementStatus: "confirmed", version: item.version }); setAchievements((items) => items.map((row) => row.id === item.id ? { ...row, achievementStatus: "confirmed" } : row)); message.success("成果已确认"); }
    catch (error) { message.error(getApiErrorMessage(error, "确认成果失败")); }
  }

  const uploadMaterials: UploadProps["customRequest"] = (options) => { const file = options.file as RcFile; setPendingMaterials((items) => [...items, file]); options.onSuccess?.({}); };

  return <Modal destroyOnClose open={open} width={900} title={project ? "编辑科研课题" : "新增科研课题"} okText="保存课题" cancelText="取消" confirmLoading={saving} onCancel={onCancel} onOk={() => form.validateFields().then(saveBasic)}>
    <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
      { key: "basic", label: "基本信息", children: <Form form={form} layout="vertical"><Space size={16} style={{ display: "flex" }}><Form.Item name="projectNo" label="课题编号" rules={[{ required: true }]} style={{ flex: 1 }}><Input disabled={Boolean(project)} /></Form.Item><Form.Item name="projectType" label="课题类型" rules={[{ required: true }]} style={{ flex: 1 }}><Select options={[{ value: "teaching", label: "教学研究" }, { value: "research", label: "科研项目" }, { value: "cooperation", label: "合作项目" }]} /></Form.Item></Space><Form.Item name="projectName" label="课题名称" rules={[{ required: true }]}><Input /></Form.Item><Space size={16} style={{ display: "flex" }}><Form.Item name="leaderId" label="负责人" rules={[{ required: true }]} style={{ flex: 1 }}><Select options={leaderOptions} onChange={setLeaderId} disabled={!canManage} /></Form.Item><Form.Item name="speciesId" label="关联药材" style={{ flex: 1 }}><Select allowClear showSearch optionFilterProp="label" options={herbs.map((herb) => ({ value: herb.id, label: `${herb.herbName}（${herb.herbCode}）` }))} /></Form.Item></Space><Space size={16} style={{ display: "flex" }}><Form.Item name="startedAt" label="开始日期" style={{ flex: 1 }}><Input type="date" /></Form.Item><Form.Item name="endedAt" label="结束日期" style={{ flex: 1 }}><Input type="date" /></Form.Item></Space><Form.Item name="description" label="课题说明"><Input.TextArea rows={4} /></Form.Item><Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item></Form> },
      { key: "members", label: `成员（${members.filter((item) => item.memberStatus === "active").length}）`, children: <div><Form form={memberForm} layout="inline" onFinish={addMember}><Form.Item name="userId" rules={[{ required: true }]}><Select placeholder="选择成员" style={{ width: 240 }} options={memberOptions} /></Form.Item><Form.Item name="memberRole" initialValue="researcher" rules={[{ required: true }]}><Select style={{ width: 150 }} options={[{ value: "researcher", label: "研究人员" }, { value: "assistant", label: "协助人员" }, { value: "student", label: "学生成员" }]} /></Form.Item><Button type="primary" htmlType="submit" icon={<PlusOutlined />} disabled={!detail}>添加成员</Button></Form><div style={{ display: "grid", gap: 8, marginTop: 16 }}>{members.map((item) => <Space key={item.userId} style={{ justifyContent: "space-between", borderBottom: "1px solid var(--color-border)", padding: "8px 0" }}><span>{item.realName ?? item.username} · {item.memberRole} · {item.memberStatus}</span>{item.memberRole !== "leader" && item.memberStatus === "active" ? <Button danger type="text" icon={<DeleteOutlined />} onClick={() => removeMember(item.userId)} /> : null}</Space>)}</div></div> },
      { key: "materials", label: `过程材料（${materials.length + pendingMaterials.length}）`, children: <div><Upload multiple showUploadList={false} customRequest={uploadMaterials}><Button icon={<FileAddOutlined />} disabled={!detail}>上传材料</Button></Upload><div style={{ marginTop: 12 }}>{materials.map((item) => <Space key={item.fileId} style={{ display: "flex", justifyContent: "space-between", padding: "8px 0", borderBottom: "1px solid var(--color-border)" }}><span>{item.fileName ?? item.originalFilename} · {item.fileUsage}</span><Button danger type="text" onClick={() => detail && removeResearchMaterial(detail.id, item.fileId).then(() => setMaterials((rows) => rows.filter((row) => row.fileId !== item.fileId)))}>解除</Button></Space>)}{pendingMaterials.map((file) => <div key={file.name}>{file.name}（待保存）</div>)}</div></div> },
      { key: "achievements", label: `阶段成果（${achievements.length}）`, children: <div><Form form={achievementForm} layout="vertical" onFinish={addAchievement}><Space size={12} style={{ display: "flex" }}><Form.Item name="achievementNo" label="成果编号" rules={[{ required: true }]} style={{ flex: 1 }}><Input /></Form.Item><Form.Item name="achievementType" label="成果类型" rules={[{ required: true }]} style={{ flex: 1 }}><Select options={[{ value: "paper", label: "论文" }, { value: "patent", label: "专利" }, { value: "report", label: "报告" }, { value: "experiment_result", label: "实验成果" }]} /></Form.Item><Form.Item name="achievementStage" label="阶段" style={{ flex: 1 }}><Select allowClear options={[{ value: "initial", label: "初期" }, { value: "middle", label: "中期" }, { value: "final", label: "结题" }]} /></Form.Item></Space><Form.Item name="achievementName" label="成果名称" rules={[{ required: true }]}><Input /></Form.Item><Form.Item name="description" label="成果说明"><Input.TextArea rows={2} /></Form.Item><Upload maxCount={1} showUploadList={false} beforeUpload={(file) => { setPendingAchievementFile(file); return false; }}><Button icon={<FileAddOutlined />}>选择成果文件</Button></Upload>{pendingAchievementFile ? <span style={{ marginLeft: 8 }}>{pendingAchievementFile.name}</span> : null}<Button style={{ marginTop: 12 }} type="primary" htmlType="submit" disabled={!detail}>新增成果</Button></Form><div style={{ marginTop: 16 }}>{achievements.map((item) => <Space key={item.id} style={{ display: "flex", justifyContent: "space-between", padding: "8px 0", borderBottom: "1px solid var(--color-border)" }}><span>{item.achievementName} · {item.achievementStage ?? "未分阶段"} · {item.achievementStatus}</span>{item.achievementStatus === "draft" ? <Button type="link" onClick={() => submitAchievement(item)}>提交</Button> : null}{item.achievementStatus === "submitted" ? <Button type="link" onClick={() => confirmAchievement(item)}>确认</Button> : null}</Space>)}</div></div> },
    ]} />
  </Modal>;
}
