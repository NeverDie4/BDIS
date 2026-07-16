"use client";

import { App } from "antd";
import type { Key } from "react";
import { useCallback, useEffect, useState } from "react";
import { useAuthStore } from "@/stores/auth-store";
import {
  deleteCourse,
  getApiErrorMessage,
  getCourse,
  listCourses,
  mapCourseDetail,
  mapCourseList,
  offlineCourse,
  publishCourse,
  enrollCourse,
  listMyCourseEnrollments,
} from "@/lib/courses";
import { fetchEnabledHerbs } from "@/lib/herbs";
import { listResearchProjects, mapResearchDetail, mapResearchProject, changeResearchStatus, getResearchProject, getApiErrorMessage as getResearchApiErrorMessage } from "@/lib/research";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { CourseEditorModal } from "./CourseEditorModal";
import { ResearchProjectEditorModal } from "./ResearchProjectEditorModal";
import { TeachingFilterPanel } from "./TeachingFilterPanel";
import { TeachingModuleTabs } from "./TeachingModuleTabs";
import { TeachingWorkspace } from "./TeachingWorkspace";
import type { CourseRecord, ResearchRecord, TeachingTabKey } from "./types";
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
  const { message, modal } = App.useApp();
  const user = useAuthStore((state) => state.user);
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [activeTab, setActiveTab] = useState<TeachingTabKey>("course");
  const [courses, setCourses] = useState<CourseRecord[]>([]);
  const [coursesLoading, setCoursesLoading] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<CourseRecord | null>(null);
  const [selectedResearch, setSelectedResearch] = useState<ResearchRecord | null>(null);
  const [courseViewMode, setCourseViewMode] = useState<"all" | "mine">("all");
  const [enrolledCourseIds, setEnrolledCourseIds] = useState<string[]>([]);
  const [editorCourse, setEditorCourse] = useState<CourseRecord | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [selectedCourseRowKeys, setSelectedCourseRowKeys] = useState<Key[]>([]);
  const [selectedResearchRowKeys, setSelectedResearchRowKeys] = useState<Key[]>([]);
  const [researchProjects, setResearchProjects] = useState<ResearchRecord[]>([]);
  const [researchLoading, setResearchLoading] = useState(false);
  const [researchUsers, setResearchUsers] = useState<import("@/lib/research").ResearchUserCandidateApi[]>([]);
  const [researchHerbs, setResearchHerbs] = useState<import("@/lib/herbs").HerbSpeciesApi[]>([]);
  const [researchEditorProject, setResearchEditorProject] = useState<ResearchRecord | null>(null);
  const [researchEditorOpen, setResearchEditorOpen] = useState(false);
  const activePageMeta = pageMeta[activeTab];
  const canCreateCourse = hasPermission("edu:course:add");
  const canEnrollCourse = !canCreateCourse && hasPermission("edu:course:list");
  const canReviewExperiment =
    hasPermission("edu:experiment-record:grade") ||
    hasPermission("edu:experiment-record:archive") ||
    user?.roleCodes.some((role) => ["TEACHER", "REVIEWER"].includes(role.toUpperCase())) === true;
  const visibleCourses = courses
    .map((course) => ({ ...course, enrollmentStatus: enrolledCourseIds.includes(String(course.id)) ? "enrolled" as const : "available" as const }))
    .filter((course) => courseViewMode !== "mine" || (canEnrollCourse ? course.enrollmentStatus === "enrolled" : course.teacherId === user?.userId));

  const reloadCourses = useCallback(async () => {
    if (activeTab !== "course" || !hasPermission("edu:course:list")) return;
    setCoursesLoading(true);
    try {
      const result = await listCourses();
      setCourses(result.records.map(mapCourseList));
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程列表加载失败"));
    } finally {
      setCoursesLoading(false);
    }
  }, [activeTab, hasPermission, message]);

  useEffect(() => {
    void reloadCourses();
  }, [reloadCourses]);

  useEffect(() => {
    if (!canEnrollCourse) {
      setCourseViewMode("all");
      return;
    }
    void listMyCourseEnrollments()
      .then((items) => setEnrolledCourseIds(items.map((item) => String(item.courseId))))
      .catch((error) => {
        setEnrolledCourseIds([]);
        message.error(getApiErrorMessage(error, "我的课程加载失败"));
      });
  }, [canEnrollCourse, message]);

  const reloadResearch = useCallback(async () => {
    if (activeTab !== "research" || !hasPermission("research:project:list")) return;
    setResearchLoading(true);
    try {
      const result = await listResearchProjects();
      setResearchProjects(result.records.map(mapResearchProject));
    } catch (error) { message.error(getResearchApiErrorMessage(error, "课题列表加载失败")); }
    finally { setResearchLoading(false); }
  }, [activeTab, hasPermission, message]);

  useEffect(() => {
    if (activeTab !== "research") return;
    void reloadResearch();
    void Promise.all([
      hasPermission("research:project:add") || hasPermission("research:project:update") ? import("@/lib/research").then(({ listResearchUsers }) => listResearchUsers()) : Promise.resolve(null),
      hasPermission("research:project:add") || hasPermission("research:project:update") ? fetchEnabledHerbs() : Promise.resolve([]),
    ]).then(([userPage, herbs]) => { if (userPage) setResearchUsers(userPage); setResearchHerbs(herbs); }).catch((error) => message.error(getResearchApiErrorMessage(error, "课题候选数据加载失败")));
  }, [activeTab, hasPermission, message, reloadResearch]);

  async function handleViewCourse(course: CourseRecord) {
    try {
      const detail = await getCourse(Number(course.id));
      setSelectedCourse(mapCourseDetail(detail));
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程详情加载失败"));
    }
  }

  async function handleEditCourse(course: CourseRecord) {
    try {
      const detail = await getCourse(Number(course.id));
      setEditorCourse(mapCourseDetail(detail));
      setEditorOpen(true);
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程详情加载失败"));
    }
  }

  async function handlePublish(course: CourseRecord) {
    if (!course) return;
    try {
      const detail = await getCourse(Number(course.id));
      await publishCourse(detail.id, detail.version);
      message.success("课程已发布");
      await reloadCourses();
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程发布失败"));
    }
  }

  async function handleOffline(course: CourseRecord) {
    if (!course) return;
    try {
      const detail = await getCourse(Number(course.id));
      await offlineCourse(detail.id, detail.version);
      message.success("课程已下线");
      await reloadCourses();
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程下线失败"));
    }
  }

  function handleDelete(course: CourseRecord) {
    if (!course) return;
    modal.confirm({
      title: "确认删除课程？",
      content: `删除后将同时失去课程的步骤和资源入口：${course.courseName}`,
      okText: "删除",
      okButtonProps: { danger: true },
      cancelText: "取消",
      onOk: async () => {
        try {
          await deleteCourse(Number(course.id));
          message.success("课程已删除");
          await reloadCourses();
        } catch (error) {
          message.error(getApiErrorMessage(error, "课程删除失败"));
        }
      },
    });
  }

  async function handleViewResearch(project: ResearchRecord) {
    try { const detail = await getResearchProject(Number(project.id)); setResearchEditorProject(mapResearchDetail(detail)); setResearchEditorOpen(true); }
    catch (error) { message.error(getResearchApiErrorMessage(error, "课题详情加载失败")); }
  }

  async function handleResearchView(project: ResearchRecord) {
    try {
      const detail = await getResearchProject(Number(project.id));
      setSelectedResearch(mapResearchDetail(detail));
      setSelectedCourse(null);
    } catch (error) {
      message.error(getResearchApiErrorMessage(error, "课题详情加载失败"));
    }
  }

  async function handleChangeResearchStatus(project: ResearchRecord, status: string) {
    try { const detail = await getResearchProject(Number(project.id)); await changeResearchStatus(detail.id, { targetStatus: status, version: detail.version, reason: "前端课题状态操作" }); message.success("课题状态已更新"); await reloadResearch(); }
    catch (error) { message.error(getResearchApiErrorMessage(error, "课题状态更新失败")); }
  }

  return (
    <div className={styles.teachingPage}>
      <ModuleHeroBanner
        description={activePageMeta.description}
        eyebrow="TEACHING & RESEARCH"
        sealText="教学"
        className={styles.moduleHero}
        title={activePageMeta.title}
      />
      <TeachingModuleTabs
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setSelectedCourse(null);
          setSelectedResearch(null);
        }}
      />
      <TeachingFilterPanel />
      <TeachingWorkspace
        activeTab={activeTab}
        courses={visibleCourses}
        coursesLoading={coursesLoading}
        canAdd={canCreateCourse}
        canEnroll={canEnrollCourse}
        courseViewMode={courseViewMode}
        onCourseViewModeChange={setCourseViewMode}
        canEdit={hasPermission("edu:course:update")}
        canPublish={hasPermission("edu:course:publish")}
        canDelete={hasPermission("edu:course:delete")}
        canRecordAdd={hasPermission("edu:experiment-record:add")}
        canRecordList={hasPermission("edu:experiment-record:list")}
        canRecordUpdate={hasPermission("edu:experiment-record:update")}
        canRecordSubmit={hasPermission("edu:experiment-record:submit")}
        canRecordArchive={hasPermission("edu:experiment-record:archive")}
        canRecordDelete={hasPermission("edu:experiment-record:delete")}
        canGrade={canReviewExperiment}
        selectedCourse={selectedCourse}
        selectedResearch={selectedResearch}
        selectedCourseRowKeys={selectedCourseRowKeys}
        selectedResearchRowKeys={selectedResearchRowKeys}
        onCloseCourse={() => setSelectedCourse(null)}
        onCloseResearch={() => setSelectedResearch(null)}
        onCourseSelectionChange={setSelectedCourseRowKeys}
        onResearchSelectionChange={setSelectedResearchRowKeys}
        onViewCourse={handleViewCourse}
        onAddCourse={() => {
          setEditorCourse(null);
          setEditorOpen(true);
        }}
        onEditCourse={handleEditCourse}
        onPublishCourse={handlePublish}
        onOfflineCourse={handleOffline}
        onDeleteCourse={handleDelete}
        researchProjects={researchProjects}
        researchLoading={researchLoading}
        canResearchAdd={hasPermission("research:project:add")}
        canResearchEdit={hasPermission("research:project:update")}
        canResearchStatus={hasPermission("research:project:status")}
        currentUserId={user?.userId}
        canManageAll={user?.roleCodes.some((role) => role.toUpperCase() === "ADMIN") === true}
        onAddResearch={() => { setResearchEditorProject(null); setResearchEditorOpen(true); }}
        onEditResearch={handleViewResearch}
        onViewResearch={handleResearchView}
        onChangeResearchStatus={handleChangeResearchStatus}
        onReloadResearch={() => void reloadResearch()}
        onEnrollCourse={(course) => { void enrollCourse(Number(course.id)).then(() => { setEnrolledCourseIds((ids) => ids.includes(String(course.id)) ? ids : [...ids, String(course.id)]); message.success("已加入我的课程"); }).catch((error) => message.error(getApiErrorMessage(error, "选课失败"))); }}
      />
      <CourseEditorModal
        open={editorOpen}
        course={editorCourse}
        teacherId={user?.userId ?? 0}
        onCancel={() => setEditorOpen(false)}
        onSaved={(detail) => {
          setEditorOpen(false);
          setSelectedCourse(mapCourseDetail(detail));
          void reloadCourses();
        }}
      />
      <ResearchProjectEditorModal
        open={researchEditorOpen}
        project={researchEditorProject}
        users={researchUsers}
        herbs={researchHerbs}
        canManage={hasPermission("research:project:update")}
        onCancel={() => setResearchEditorOpen(false)}
        onSaved={(detail) => { setResearchEditorOpen(false); setResearchEditorProject(mapResearchDetail(detail)); void reloadResearch(); }}
      />
    </div>
  );
}
