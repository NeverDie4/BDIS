"use client";

import { Button, Card, Descriptions, Modal, Tabs, Typography } from "antd";
import type { TableProps } from "antd";
import { useState } from "react";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { DataTable } from "@/components/common/DataTable";
import { DetailDrawer } from "@/components/common/DetailDrawer";
import { InfoCard } from "@/components/common/InfoCard";
import { MetricCard } from "@/components/common/MetricCard";
import { StatusTag } from "@/components/common/StatusTag";
import { ResearchTag } from "@/components/feature/ResearchTag";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import {
  courseResources,
  courses,
  experimentSteps,
  researchProjects,
  trainingFeedback,
  trainingPlans,
  trainingRecords,
  videoResources,
} from "@/mocks/teaching";
import type { Course, ResearchProject, TrainingPlan } from "@/types/teaching";
import styles from "@/styles/mockPages.module.css";

export default function TeachingPage() {
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const projectColumns: TableProps<ResearchProject>["columns"] = [
    { title: "课题编号", dataIndex: "projectNo", key: "projectNo", width: 150 },
    { title: "课题名称", dataIndex: "projectName", key: "projectName" },
    { title: "负责人", dataIndex: "leader", key: "leader", width: 100 },
    {
      title: "方向",
      key: "researchDirection",
      render: (_, record) => <ResearchTag label={record.researchDirection} type="project" />,
    },
    {
      title: "状态",
      key: "status",
      render: (_, record) => <StatusTag status={record.status} />,
    },
  ];

  const trainingColumns: TableProps<TrainingPlan>["columns"] = [
    { title: "培训编号", dataIndex: "planNo", key: "planNo", width: 150 },
    { title: "培训计划", dataIndex: "planName", key: "planName" },
    { title: "对象", dataIndex: "targetGroup", key: "targetGroup", width: 120 },
    { title: "讲师", dataIndex: "trainer", key: "trainer", width: 110 },
    {
      title: "状态",
      key: "status",
      render: (_, record) => <StatusTag status={record.status} />,
    },
  ];

  const selectedSteps = experimentSteps.filter((step) => step.courseId === selectedCourse?.id);
  const selectedResources = courseResources.filter((resource) => resource.courseId === selectedCourse?.id);
  const selectedVideos = videoResources.filter((resource) => resource.courseId === selectedCourse?.id);

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="TEACHING & RESEARCH"
          title="教学科研资源馆"
          subtitle="汇集实验课程、实验步骤、课程资料、视频链接和课题研究材料，让教学资源与科研过程材料在同一门户中沉淀和复用。"
        />

        <div className={styles.metricGrid}>
          <MetricCard description="实验课程和资料归档入口。" title="实验课程" value={courses.length} />
          <MetricCard description="课件、视频、指导书和图片资料。" title="课程资料" value={courseResources.length} />
          <MetricCard description="正在推进的科研主题。" title="课题研究" value={researchProjects.length} />
          <MetricCard description="培训计划与反馈记录。" title="培训反馈" value={trainingFeedback.length} />
        </div>

        <ActionToolbar
          actions={
            <div className={styles.toolbarActions}>
              <Button onClick={() => setModalOpen(true)}>导入资料</Button>
              <Button type="primary" onClick={() => setModalOpen(true)}>
                新增课程
              </Button>
            </div>
          }
          description="课程卡片点击后展示实验步骤、资料和视频资源。"
          title="实验课程"
        />

        <div className={styles.threeGrid}>
          {courses.map((course) => (
            <Card className={styles.panel} key={course.id} variant="borderless">
              <div className={styles.panelBody}>
                <ResearchTag status={course.status === "published" ? "done" : "draft"} type="course" />
                <Typography.Title level={2}>{course.courseName}</Typography.Title>
                <Typography.Paragraph className={styles.mutedText}>{course.description}</Typography.Paragraph>
                <div className={styles.detailList}>
                  <span>授课教师：{course.teacher}</span>
                  <span>所属部门：{course.department}</span>
                  <span>标签：{course.tags.join("、")}</span>
                </div>
                <Button type="link" onClick={() => setSelectedCourse(course)}>
                  查看课程详情
                </Button>
              </div>
            </Card>
          ))}
        </div>

        <Tabs
          items={[
            {
              key: "projects",
              label: "课题研究",
              children: <DataTable<ResearchProject> columns={projectColumns} dataSource={researchProjects} pagination={false} rowKey="id" />,
            },
            {
              key: "training",
              label: "教学培训",
              children: <DataTable<TrainingPlan> columns={trainingColumns} dataSource={trainingPlans} pagination={false} rowKey="id" />,
            },
          ]}
        />

        <div className={styles.twoGrid}>
          <InfoCard title="培训记录">
            <ul className={styles.compactList}>
              {trainingRecords.map((record) => (
                <li key={record.id}>{record.traineeName} / {record.department} / {record.attendanceStatus} / {record.score ?? "-"} 分</li>
              ))}
            </ul>
          </InfoCard>
          <InfoCard title="培训反馈">
            <ul className={styles.compactList}>
              {trainingFeedback.map((feedback) => (
                <li key={feedback.id}>{feedback.traineeName}：{feedback.comment}</li>
              ))}
            </ul>
          </InfoCard>
        </div>
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
              <Descriptions.Item label="授课教师">{selectedCourse.teacher}</Descriptions.Item>
              <Descriptions.Item label="状态"><StatusTag status={selectedCourse.status} /></Descriptions.Item>
              <Descriptions.Item label="说明">{selectedCourse.description}</Descriptions.Item>
            </Descriptions>
            <InfoCard title="实验步骤">
              <ul className={styles.compactList}>
                {selectedSteps.map((step) => (
                  <li key={step.id}>{step.stepNo}. {step.title}：{step.description}</li>
                ))}
              </ul>
            </InfoCard>
            <InfoCard title="课程资料">
              <ul className={styles.compactList}>
                {selectedResources.map((resource) => (
                  <li key={resource.id}>{resource.resourceName} / {resource.resourceType}</li>
                ))}
              </ul>
            </InfoCard>
            <InfoCard title="视频资源">
              <ul className={styles.compactList}>
                {selectedVideos.length > 0 ? selectedVideos.map((video) => (
                  <li key={video.id}>{video.resourceName} / {video.durationText}</li>
                )) : <li>暂无视频资源</li>}
              </ul>
            </InfoCard>
          </div>
        ) : null}
      </DetailDrawer>

      <Modal footer={null} open={modalOpen} title="教学科研操作占位" onCancel={() => setModalOpen(false)}>
        <Typography.Paragraph className={styles.mutedText}>
          课程新增、资料上传和课题维护将在文件资源与后端接口接入后完善。
        </Typography.Paragraph>
      </Modal>
    </SiteLayout>
  );
}
