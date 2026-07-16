"use client";

import { DeleteOutlined, FileAddOutlined, PlusOutlined, UploadOutlined } from "@ant-design/icons";
import { App, Button, Form, Input, InputNumber, Modal, Select, Space, Tabs, Upload } from "antd";
import type { RcFile, UploadProps } from "antd/es/upload/interface";
import { useEffect, useMemo, useState } from "react";
import { uploadFile } from "@/lib/files";
import {
  bindCourseResource,
  createCourse,
  createCourseStep,
  deleteCourseResource,
  deleteCourseStep,
  getCourse,
  getApiErrorMessage,
  updateCourse,
  updateCourseStep,
  type CourseDetailApi,
} from "@/lib/courses";
import type { CourseRecord } from "./types";

type CourseFormValues = {
  courseNo: string;
  courseName: string;
  courseType: string;
  description?: string;
  videoUrl?: string;
  applicableMajors?: string;
  hours?: number;
  credits?: number;
  prerequisites?: string;
  teachingObjectives?: string;
  teachingMethods?: string;
  tags?: string;
  remark?: string;
};

type StepDraft = {
  key: string;
  id?: number;
  version?: number;
  stepNo: string;
  stepTitle: string;
  stepContent?: string;
  expectedResult?: string;
  sortOrder: number;
};

type ResourceDraft = {
  key: string;
  id?: number;
  fileId?: number;
  resourceName: string;
  resourceType?: string;
  fileName?: string;
  fileSize?: number;
  file?: File;
  sortOrder: number;
};

type CourseEditorModalProps = {
  open: boolean;
  course: CourseRecord | null;
  teacherId: number;
  onCancel: () => void;
  onSaved: (course: CourseDetailApi) => void;
};

function fromCourse(course: CourseRecord | null) {
  if (!course) {
    return { values: {}, steps: [] as StepDraft[], resources: [] as ResourceDraft[] };
  }
  return {
    values: {
      courseNo: course.courseNo,
      courseName: course.courseName,
      courseType: course.category,
      description: course.description,
      videoUrl: course.detail.videoUrl,
      applicableMajors: course.detail.applicableMajors.join("\n"),
      hours: course.detail.hours,
      credits: course.detail.credits,
      prerequisites: course.detail.prerequisites.join("\n"),
      teachingObjectives: course.detail.teachingObjectives.join("\n"),
      teachingMethods: course.detail.teachingMethods.join("\n"),
      tags: course.detail.tags.join("\n"),
    },
    steps: course.detail.experimentSteps.map((step, index) => ({
      key: `step-${step.id ?? index}`,
      id: step.id,
      version: step.version,
      stepNo: step.stepNo ?? String(index + 1),
      stepTitle: step.title,
      stepContent: step.description,
      expectedResult: step.expectedResult,
      sortOrder: index,
    })),
    resources: course.detail.resources.map((resource, index) => ({
      key: `resource-${resource.id ?? index}`,
      id: resource.id,
      fileId: resource.fileId,
      resourceName: resource.name,
      resourceType: resource.type,
      fileName: resource.name,
      fileSize: Number(resource.size) || undefined,
      sortOrder: index,
    })),
  };
}

export function CourseEditorModal({ open, course, teacherId, onCancel, onSaved }: CourseEditorModalProps) {
  const { message } = App.useApp();
  const [form] = Form.useForm<CourseFormValues>();
  const [activeTab, setActiveTab] = useState("basic");
  const [steps, setSteps] = useState<StepDraft[]>([]);
  const [resources, setResources] = useState<ResourceDraft[]>([]);
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const initial = useMemo(() => fromCourse(course), [course]);

  useEffect(() => {
    if (!open) return;
    form.setFieldsValue(initial.values);
    setSteps(initial.steps);
    setResources(initial.resources);
    setVideoFile(null);
    setActiveTab("basic");
  }, [form, initial, open]);

  function addStep() {
    setSteps((current) => [
      ...current,
      {
        key: `step-new-${Date.now()}`,
        stepNo: String(current.length + 1),
        stepTitle: "",
        stepContent: "",
        expectedResult: "",
        sortOrder: current.length,
      },
    ]);
    setActiveTab("steps");
  }

  function updateStep(key: string, patch: Partial<StepDraft>) {
    setSteps((current) => current.map((step) => (step.key === key ? { ...step, ...patch } : step)));
  }

  async function removeStep(step: StepDraft) {
    if (step.id && course) {
      try {
        await deleteCourseStep(Number(course.id), step.id);
      } catch (error) {
        message.error(getApiErrorMessage(error, "删除实验步骤失败"));
        return;
      }
    }
    setSteps((current) => current.filter((item) => item.key !== step.key));
  }

  const uploadResource: UploadProps["customRequest"] = (options) => {
    const file = options.file as RcFile;
    const key = `resource-new-${Date.now()}`;
    setResources((current) => [
      ...current,
      {
        key,
        file,
        fileName: file.name,
        fileSize: file.size,
        resourceName: file.name,
        resourceType: "courseware",
        sortOrder: current.length,
      },
    ]);
    options.onSuccess?.({ key });
    message.success("课程文件已加入待保存资源");
  }

  async function removeResource(resource: ResourceDraft) {
    if (resource.id && course) {
      try {
        await deleteCourseResource(Number(course.id), resource.id);
      } catch (error) {
        message.error(getApiErrorMessage(error, "解绑课程资源失败"));
        return;
      }
    }
    setResources((current) => current.filter((item) => item.key !== resource.key));
  }

  async function handleSave() {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const toList = (value?: string) => value?.split(/\r?\n|,/).map((item) => item.trim()).filter(Boolean) ?? [];
      const payload = {
        ...values,
        teacherId,
        applicableMajors: toList(values.applicableMajors),
        prerequisites: toList(values.prerequisites),
        teachingObjectives: toList(values.teachingObjectives),
        teachingMethods: toList(values.teachingMethods),
        tags: toList(values.tags),
      };
      const current = course ? await getCourse(Number(course.id)) : null;
      const currentStepVersions = new Map((current?.steps ?? []).map((step) => [step.id, step.version]));
      const saved = course
        ? await updateCourse(Number(course.id), { ...payload, version: current?.version ?? course.version ?? 0 })
        : await createCourse(payload);
      const courseId = saved.id;

      let latest = saved;
      if (videoFile) {
        const uploaded = await uploadFile(videoFile, {
          bizType: "edu_course",
          bizId: courseId,
          fileUsage: "course_video",
        });
        latest = await updateCourse(courseId, {
          ...payload,
          videoUrl: uploaded.fileUrl,
          version: latest.version,
        });
      }

      for (const [index, step] of steps.entries()) {
        const stepPayload = {
          stepNo: step.stepNo,
          stepTitle: step.stepTitle,
          stepContent: step.stepContent,
          expectedResult: step.expectedResult,
          sortOrder: index,
        };
        if (step.id) {
           await updateCourseStep(courseId, step.id, { ...stepPayload, version: currentStepVersions.get(step.id) ?? step.version ?? 0 });
        } else {
          await createCourseStep(courseId, stepPayload);
        }
      }

      for (const resource of resources) {
        if (!resource.file || resource.fileId) continue;
        const uploaded = await uploadFile(resource.file, {
          bizType: "edu_course",
          bizId: courseId,
          fileUsage: "course_resource",
        });
        await bindCourseResource(courseId, {
          fileId: uploaded.id,
          resourceName: resource.resourceName || uploaded.originalFilename || uploaded.fileName,
          resourceType: resource.resourceType || "courseware",
          sortOrder: resource.sortOrder,
        });
      }

      const refreshed = await getCourse(latest.id);
      message.success(course ? "课程已更新" : "课程已创建");
      onSaved(refreshed);
    } catch (error) {
      message.error(getApiErrorMessage(error, "保存课程失败"));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      destroyOnClose
      open={open}
      title={course ? "编辑实验课程" : "新增实验课程"}
      width={860}
      okText="保存课程"
      cancelText="取消"
      confirmLoading={saving}
      onCancel={onCancel}
      onOk={handleSave}
    >
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: "basic",
            label: "基础信息",
            children: (
              <Form form={form} layout="vertical" preserve={false}>
                <Space size={16} style={{ display: "flex" }}>
                  <Form.Item label="课程编号" name="courseNo" rules={[{ required: true, message: "请输入课程编号" }]} style={{ flex: 1 }}>
                    <Input placeholder="如 TC-2026-001" />
                  </Form.Item>
                  <Form.Item label="课程类型" name="courseType" rules={[{ required: true, message: "请选择课程类型" }]} style={{ flex: 1 }}>
                    <Select options={[{ value: "experiment", label: "实验课程" }, { value: "online", label: "在线课程" }, { value: "mixed", label: "混合课程" }]} />
                  </Form.Item>
                </Space>
                <Form.Item label="课程名称" name="courseName" rules={[{ required: true, message: "请输入课程名称" }]}>
                  <Input />
                </Form.Item>
                <Form.Item label="课程简介" name="description">
                  <Input.TextArea rows={4} showCount maxLength={1000} />
                </Form.Item>
                <Form.Item label="视频地址" name="videoUrl">
                  <Input placeholder="可选，填写课程视频地址" />
                </Form.Item>
                <Space size={16} style={{ display: "flex" }}>
                  <Form.Item label="适用专业（每行一个）" name="applicableMajors" style={{ flex: 2 }}>
                    <Input.TextArea rows={2} placeholder="中药学\n药学" />
                  </Form.Item>
                  <Form.Item label="学时" name="hours" style={{ flex: 1 }}>
                    <InputNumber min={0} style={{ width: "100%" }} />
                  </Form.Item>
                  <Form.Item label="学分" name="credits" style={{ flex: 1 }}>
                    <InputNumber min={0} step={0.5} style={{ width: "100%" }} />
                  </Form.Item>
                </Space>
                <Form.Item label="先修课程（每行一个）" name="prerequisites">
                  <Input.TextArea rows={2} />
                </Form.Item>
                <Form.Item label="教学目标（每行一个）" name="teachingObjectives">
                  <Input.TextArea rows={3} />
                </Form.Item>
                <Form.Item label="教学方式（每行一个）" name="teachingMethods">
                  <Input.TextArea rows={2} />
                </Form.Item>
                <Form.Item label="课程标签（每行一个）" name="tags">
                  <Input.TextArea rows={2} />
                </Form.Item>
                <Upload
                  accept="video/*"
                  maxCount={1}
                  showUploadList={false}
                  beforeUpload={(file) => {
                    setVideoFile(file);
                    message.success("视频已加入待上传列表，保存课程后上传");
                    return false;
                  }}
                >
                  <Button icon={<UploadOutlined />}>上传视频文件</Button>
                </Upload>
                {videoFile ? <span style={{ marginLeft: 12 }}>{videoFile.name}</span> : null}
              </Form>
            ),
          },
          {
            key: "steps",
            label: `实验步骤（${steps.length}）`,
            children: (
              <div>
                <Button icon={<PlusOutlined />} type="dashed" block onClick={addStep}>新增实验步骤</Button>
                <div style={{ display: "grid", gap: 12, marginTop: 12 }}>
                  {steps.map((step, index) => (
                    <div key={step.key} style={{ border: "1px solid var(--color-border)", borderRadius: 4, padding: 12 }}>
                      <Space align="start" style={{ display: "flex" }}>
                        <InputNumber min={0} value={Number(step.stepNo) || index + 1} onChange={(value) => updateStep(step.key, { stepNo: String(value ?? index + 1) })} />
                        <Input value={step.stepTitle} placeholder="步骤标题" onChange={(event) => updateStep(step.key, { stepTitle: event.target.value })} style={{ flex: 1 }} />
                        <Button danger type="text" icon={<DeleteOutlined />} onClick={() => removeStep(step)} />
                      </Space>
                      <Input.TextArea value={step.stepContent} placeholder="实验步骤内容" rows={2} onChange={(event) => updateStep(step.key, { stepContent: event.target.value })} style={{ marginTop: 8 }} />
                      <Input value={step.expectedResult} placeholder="预期结果（可选）" onChange={(event) => updateStep(step.key, { expectedResult: event.target.value })} style={{ marginTop: 8 }} />
                    </div>
                  ))}
                </div>
              </div>
            ),
          },
          {
            key: "resources",
            label: `课程资源（${resources.length}）`,
            children: (
              <div>
                <Upload multiple showUploadList={false} customRequest={uploadResource}>
                  <Button icon={<FileAddOutlined />}>添加课程文件</Button>
                </Upload>
                <div style={{ display: "grid", gap: 10, marginTop: 12 }}>
                  {resources.map((resource) => (
                    <Space key={resource.key} style={{ display: "flex" }}>
                      <Input value={resource.resourceName} onChange={(event) => setResources((current) => current.map((item) => item.key === resource.key ? { ...item, resourceName: event.target.value } : item))} style={{ flex: 1 }} />
                      <Select value={resource.resourceType} options={[{ value: "courseware", label: "课件" }, { value: "handout", label: "讲义" }, { value: "video", label: "视频" }]} onChange={(value) => setResources((current) => current.map((item) => item.key === resource.key ? { ...item, resourceType: value } : item))} style={{ width: 120 }} />
                      <span>{resource.fileName}</span>
                      <Button danger type="text" icon={<DeleteOutlined />} onClick={() => removeResource(resource)} />
                    </Space>
                  ))}
                </div>
              </div>
            ),
          },
        ]}
      />
    </Modal>
  );
}
