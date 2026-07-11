"use client";

import {
  App,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Typography,
} from "antd";
import { useCallback, useEffect, useState } from "react";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { DetailDrawer } from "@/components/common/DetailDrawer";
import { InfoCard } from "@/components/common/InfoCard";
import { FileUploadField } from "@/components/file/FileUploadField";
import { MetricCard } from "@/components/common/MetricCard";
import { StatusTag } from "@/components/common/StatusTag";
import { ResearchTag } from "@/components/feature/ResearchTag";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { fetchDictionaryOptions, type DictionaryOption } from "@/lib/dictionaries";
import {
  changeCoursePublishStatus,
  addCourseResource,
  addCourseStep,
  createCourse,
  downloadCourseResource,
  fetchCourseDetail,
  fetchCoursePage,
  type CourseApi,
  type CoursePayload,
} from "@/lib/courses";
import { deleteFileResource, type FileResource } from "@/lib/files";
import { getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "@/styles/mockPages.module.css";

export default function TeachingPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [form] = Form.useForm<CoursePayload>();
  const [courses, setCourses] = useState<CourseApi[]>([]);
  const [courseTypeOptions, setCourseTypeOptions] = useState<DictionaryOption[]>([]);
  const [resourceTypeOptions, setResourceTypeOptions] = useState<DictionaryOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<CourseApi | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [stepModalOpen, setStepModalOpen] = useState(false);
  const [resourceModalOpen, setResourceModalOpen] = useState(false);
  const [uploadedFile, setUploadedFile] = useState<FileResource>();
  const [stepForm] = Form.useForm();
  const [resourceForm] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [page, courseTypes, resourceTypes] = await Promise.all([
        fetchCoursePage(),
        fetchDictionaryOptions("course_type"),
        fetchDictionaryOptions("course_resource_type"),
      ]);
      setCourses(page.records);
      setCourseTypeOptions(courseTypes);
      setResourceTypeOptions(resourceTypes);
    } catch (error) {
      if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "课程加载失败"));
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  async function openDetail(courseId: number) {
    try {
      setSelectedCourse(await fetchCourseDetail(courseId));
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程详情加载失败"));
    }
  }

  async function submit(values: CoursePayload) {
    try {
      const courseId = await createCourse({ ...values, status: 1 });
      message.success("课程已创建");
      setModalOpen(false);
      form.resetFields();
      await load();
      await openDetail(courseId);
    } catch (error) {
      message.error(getApiErrorMessage(error, "新增课程失败"));
    }
  }

  async function publish(status: CourseApi["publishStatus"]) {
    if (!selectedCourse) return;
    try {
      setSelectedCourse(await changeCoursePublishStatus(selectedCourse.id, status));
      message.success(status === "published" ? "课程已发布" : "课程状态已更新");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程状态更新失败"));
    }
  }

  async function saveStep(values: {
    stepNo: string;
    stepTitle: string;
    stepContent?: string;
    expectedResult?: string;
    sortOrder?: number;
  }) {
    if (!selectedCourse) return;
    try {
      await addCourseStep(selectedCourse.id, values);
      setSelectedCourse(await fetchCourseDetail(selectedCourse.id));
      setStepModalOpen(false);
      stepForm.resetFields();
      message.success("实验步骤已添加");
    } catch (error) {
      message.error(getApiErrorMessage(error, "实验步骤保存失败"));
    }
  }

  async function saveResource(values: { resourceName: string; resourceType?: string }) {
    if (!selectedCourse || !uploadedFile) {
      message.error("请先上传课程文件");
      return;
    }
    try {
      await addCourseResource(selectedCourse.id, { ...values, fileId: uploadedFile.id });
      setSelectedCourse(await fetchCourseDetail(selectedCourse.id));
      setResourceModalOpen(false);
      setUploadedFile(undefined);
      resourceForm.resetFields();
      message.success("课程资源已添加");
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程资源保存失败"));
    }
  }

  function replaceUploadedFile(file: FileResource) {
    const previous = uploadedFile;
    setUploadedFile(file);
    if (previous && previous.id !== file.id) {
      void deleteFileResource(previous.id).catch(() => undefined);
    }
  }

  async function discardUploadedFile() {
    const file = uploadedFile;
    setUploadedFile(undefined);
    resourceForm.resetFields();
    setResourceModalOpen(false);
    if (!file) return;
    try {
      await deleteFileResource(file.id);
    } catch (error) {
      message.error(getApiErrorMessage(error, "临时文件清理失败"));
    }
  }

  const resourceCount = courses.reduce((sum, course) => sum + (course.resources?.length || 0), 0);

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="TEACHING & RESEARCH"
          title="实验课程资源馆"
          subtitle="课程发布、实验步骤与统一文件资源由课程服务协同管理。"
        />
        <div className={styles.metricGrid}>
          <MetricCard description="当前用户可见的课程。" title="实验课程" value={courses.length} />
          <MetricCard
            description="当前列表已加载的课程资料。"
            title="课程资料"
            value={resourceCount}
          />
          <MetricCard
            description="已向学员发布的课程。"
            title="已发布"
            value={courses.filter((course) => course.publishStatus === "published").length}
          />
          <MetricCard
            description="教师本人或管理员可继续编辑。"
            title="草稿"
            value={courses.filter((course) => course.publishStatus === "draft").length}
          />
        </div>
        <ActionToolbar
          actions={
            hasPermission("course:manage") ? (
              <Button type="primary" onClick={() => setModalOpen(true)}>
                新增课程
              </Button>
            ) : undefined
          }
          description="教师只能管理本人课程；普通查看者只能看到已发布课程。"
          title="实验课程"
        />
        {loading ? <Typography.Text type="secondary">课程加载中…</Typography.Text> : null}
        <div className={styles.threeGrid}>
          {courses.map((course) => (
            <Card className={styles.panel} key={course.id} variant="borderless">
              <div className={styles.panelBody}>
                <ResearchTag
                  status={course.publishStatus === "published" ? "done" : "draft"}
                  type="course"
                />
                <Typography.Title level={2}>{course.courseName}</Typography.Title>
                <Typography.Paragraph className={styles.mutedText}>
                  {course.description || "暂无课程说明"}
                </Typography.Paragraph>
                <div className={styles.detailList}>
                  <span>课程编号：{course.courseNo}</span>
                  <span>授课教师：{course.teacherName || "未配置"}</span>
                  <span>课程类型：{course.courseType || "未分类"}</span>
                </div>
                <Button type="link" onClick={() => void openDetail(course.id)}>
                  查看课程详情
                </Button>
              </div>
            </Card>
          ))}
        </div>
        {!loading && courses.length === 0 ? (
          <InfoCard title="暂无课程">
            <Typography.Text type="secondary">当前权限范围内没有可见课程。</Typography.Text>
          </InfoCard>
        ) : null}
      </div>
      <DetailDrawer
        open={Boolean(selectedCourse)}
        title={selectedCourse?.courseName ?? "课程详情"}
        width={620}
        onClose={() => setSelectedCourse(null)}
      >
        {selectedCourse ? (
          <div className={styles.sectionStack}>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="课程编号">{selectedCourse.courseNo}</Descriptions.Item>
              <Descriptions.Item label="授课教师">
                {selectedCourse.teacherName || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <StatusTag status={selectedCourse.publishStatus} />
              </Descriptions.Item>
              <Descriptions.Item label="说明">
                {selectedCourse.description || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="视频链接">
                {selectedCourse.videoUrl || "-"}
              </Descriptions.Item>
            </Descriptions>
            {hasPermission("course:publish") ? (
              <div className={styles.toolbarActions}>
                {selectedCourse.publishStatus !== "published" ? (
                  <Button type="primary" onClick={() => void publish("published")}>
                    发布课程
                  </Button>
                ) : (
                  <Button onClick={() => void publish("archived")}>归档课程</Button>
                )}
              </div>
            ) : null}
            <InfoCard title="实验步骤">
              {hasPermission("course:manage") ? (
                <Button type="link" onClick={() => setStepModalOpen(true)}>
                  添加步骤
                </Button>
              ) : null}
              <ul className={styles.compactList}>
                {selectedCourse.steps.length ? (
                  selectedCourse.steps.map((step) => (
                    <li key={step.id}>
                      {step.stepNo}. {step.stepTitle}：{step.stepContent || "-"}
                    </li>
                  ))
                ) : (
                  <li>暂无实验步骤</li>
                )}
              </ul>
            </InfoCard>
            <InfoCard title="课程资料">
              {hasPermission("course:manage") ? (
                <Button type="link" onClick={() => setResourceModalOpen(true)}>
                  添加资源
                </Button>
              ) : null}
              <ul className={styles.compactList}>
                {selectedCourse.resources.length ? (
                  selectedCourse.resources.map((resource) => (
                    <li key={resource.id}>
                      {resource.resourceName} /{" "}
                      {resource.resourceType || resource.fileFormat || "文件"}
                      {resource.fileId ? (
                        <Button
                          type="link"
                          onClick={() =>
                            downloadCourseResource(resource).catch((error) =>
                              message.error(getApiErrorMessage(error, "下载失败")),
                            )
                          }
                        >
                          下载
                        </Button>
                      ) : null}
                    </li>
                  ))
                ) : (
                  <li>暂无课程资料</li>
                )}
              </ul>
            </InfoCard>
          </div>
        ) : null}
      </DetailDrawer>
      <Modal
        title="新增课程"
        open={modalOpen}
        footer={null}
        destroyOnHidden
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item
            name="courseNo"
            label="课程编号"
            rules={[{ required: true, message: "请输入课程编号" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="courseName"
            label="课程名称"
            rules={[{ required: true, message: "请输入课程名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="courseType" label="课程类型">
            {courseTypeOptions.length ? (
              <Select allowClear options={courseTypeOptions} />
            ) : (
              <Select
                allowClear
                options={[
                  { label: "实验课程", value: "experiment" },
                  { label: "培训课程", value: "training" },
                ]}
              />
            )}
          </Form.Item>
          <Form.Item name="description" label="课程简介">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="videoUrl" label="视频链接">
            <Input />
          </Form.Item>
          <Button block htmlType="submit" type="primary">
            保存草稿
          </Button>
        </Form>
      </Modal>
      <Modal
        title="添加实验步骤"
        open={stepModalOpen}
        footer={null}
        destroyOnHidden
        onCancel={() => setStepModalOpen(false)}
      >
        <Form form={stepForm} layout="vertical" onFinish={saveStep}>
          <Form.Item
            name="stepNo"
            label="步骤编号"
            rules={[{ required: true, message: "请输入步骤编号" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="stepTitle"
            label="步骤标题"
            rules={[{ required: true, message: "请输入步骤标题" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="stepContent" label="步骤内容">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="expectedResult" label="预期结果">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: "100%" }} />
          </Form.Item>
          <Button block htmlType="submit" type="primary">
            保存步骤
          </Button>
        </Form>
      </Modal>
      <Modal
        title="添加课程资源"
        open={resourceModalOpen}
        footer={null}
        destroyOnHidden
        onCancel={() => void discardUploadedFile()}
      >
        <Form form={resourceForm} layout="vertical" onFinish={saveResource}>
          <Form.Item label="课程文件" required>
            <FileUploadField
              accept="*/*"
              fileUsage="course_resource"
              onChange={(value) => {
                if (!value) void discardUploadedFile();
              }}
              onUploaded={replaceUploadedFile}
              value={uploadedFile?.fileName}
            />
          </Form.Item>
          <Form.Item
            name="resourceName"
            label="资源名称"
            rules={[{ required: true, message: "请输入资源名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="resourceType" label="资源类型">
            {resourceTypeOptions.length ? (
              <Select allowClear options={resourceTypeOptions} />
            ) : (
              <Input placeholder="例如 document、video、guide" />
            )}
          </Form.Item>
          <Button block htmlType="submit" type="primary">
            保存资源
          </Button>
        </Form>
      </Modal>
    </SiteLayout>
  );
}
