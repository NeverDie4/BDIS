"use client";

import type { Key } from "react";
import { useState } from "react";
import { TeachingFilterPanel } from "./TeachingFilterPanel";
import { TeachingModuleTabs } from "./TeachingModuleTabs";
import { TeachingPageHeader } from "./TeachingPageHeader";
import { TeachingWorkspace } from "./TeachingWorkspace";
import type { CourseRecord, TeachingTabKey } from "./types";
import styles from "./teaching.module.css";

const pageMeta: Record<TeachingTabKey, { title: string; description: string }> = {
  course: {
    title: "实验课程管理",
    description:
      "管理实验课程信息、实验步骤、课程资源和视频资料，支持课程发布与教学资源共享。",
  },
  research: {
    title: "课题研究管理",
    description: "管理科研课题、参与人员、研究周期、过程材料和阶段成果。",
  },
  training: {
    title: "教学培训管理",
    description: "管理培训计划、培训记录、培训反馈和教学培训资源。",
  },
};

export function TeachingPageClient() {
  const [activeTab, setActiveTab] = useState<TeachingTabKey>("course");
  const [selectedCourse, setSelectedCourse] = useState<CourseRecord | null>(null);
  const [selectedCourseRowKeys, setSelectedCourseRowKeys] = useState<Key[]>([]);
  const [selectedResearchRowKeys, setSelectedResearchRowKeys] = useState<Key[]>([]);
  const activePageMeta = pageMeta[activeTab];

  return (
    <div className={styles.teachingPage}>
      <TeachingPageHeader
        description={activePageMeta.description}
        title={activePageMeta.title}
      />
      <TeachingModuleTabs activeTab={activeTab} onTabChange={setActiveTab} />
      <TeachingFilterPanel />
      <TeachingWorkspace
        selectedCourse={selectedCourse}
        selectedCourseRowKeys={selectedCourseRowKeys}
        selectedResearchRowKeys={selectedResearchRowKeys}
        onCloseCourse={() => setSelectedCourse(null)}
        onCourseSelectionChange={setSelectedCourseRowKeys}
        onResearchSelectionChange={setSelectedResearchRowKeys}
        onViewCourse={setSelectedCourse}
      />
    </div>
  );
}
