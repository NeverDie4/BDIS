import type { Key } from "react";
import { CourseDetailPanel } from "./CourseDetailPanel";
import { CourseManagementPanel } from "./CourseManagementPanel";
import { ResearchProjectPanel } from "./ResearchProjectPanel";
import { TrainingManagementPanel } from "./TrainingManagementPanel";
import { TrainingOverviewPanel } from "./TrainingOverviewPanel";
import type { CourseRecord, ResearchRecord, TeachingTabKey } from "./types";
import styles from "./teaching.module.css";

type TeachingWorkspaceProps = {
  activeTab: TeachingTabKey;
  courses: CourseRecord[];
  coursesLoading?: boolean;
  canAdd?: boolean;
  canEdit?: boolean;
  canPublish?: boolean;
  canDelete?: boolean;
  canEnroll?: boolean;
  courseViewMode?: "all" | "mine";
  onCourseViewModeChange?: (mode: "all" | "mine") => void;
  canRecordAdd?: boolean;
  canRecordList?: boolean;
  canRecordUpdate?: boolean;
  canRecordSubmit?: boolean;
  canRecordArchive?: boolean;
  canRecordDelete?: boolean;
  canGrade?: boolean;
  researchProjects: ResearchRecord[];
  researchLoading?: boolean;
  canResearchAdd?: boolean;
  canResearchEdit?: boolean;
  canResearchStatus?: boolean;
  canTrainingManage?: boolean;
  currentUserId?: number;
  selectedCourse: CourseRecord | null;
  selectedCourseRowKeys: Key[];
  selectedResearchRowKeys: Key[];
  onViewCourse: (course: CourseRecord) => void;
  onCloseCourse: () => void;
  onCourseSelectionChange: (keys: Key[]) => void;
  onResearchSelectionChange: (keys: Key[]) => void;
  onAddCourse: () => void;
  onEditCourse: (course: CourseRecord) => void;
  onPublishCourse: (course: CourseRecord) => void;
  onOfflineCourse: (course: CourseRecord) => void;
  onDeleteCourse: (course: CourseRecord) => void;
  onEnrollCourse?: (course: CourseRecord) => void;
  onAddResearch: () => void;
  onEditResearch: (project: ResearchRecord) => void;
  onViewResearch: (project: ResearchRecord) => void;
  onChangeResearchStatus: (project: ResearchRecord, status: string) => void;
  onReloadResearch: () => void;
};

export function TeachingWorkspace({
  activeTab,
  courses,
  coursesLoading,
  canAdd,
  canEdit,
  canPublish,
  canDelete,
  canEnroll,
  courseViewMode,
  onCourseViewModeChange,
  canRecordAdd,
  canRecordList,
  canRecordUpdate,
  canRecordSubmit,
  canRecordArchive,
  canRecordDelete,
  canGrade,
  selectedCourse,
  selectedCourseRowKeys,
  selectedResearchRowKeys,
  onViewCourse,
  onCloseCourse,
  onCourseSelectionChange,
  onResearchSelectionChange,
  onAddCourse,
  onEditCourse,
  onPublishCourse,
  onOfflineCourse,
  onDeleteCourse,
  onEnrollCourse,
  researchProjects,
  researchLoading,
  canResearchAdd,
  canResearchEdit,
  canResearchStatus,
  canTrainingManage,
  currentUserId,
  onAddResearch,
  onEditResearch,
  onViewResearch,
  onChangeResearchStatus,
  onReloadResearch,
}: TeachingWorkspaceProps) {
  return (
    <div className={`${styles.workspace} ${selectedCourse ? styles.workspaceWithDetail : ""}`}>
      <section className={styles.mainContent}>
        <div className={styles.managementGrid}>
          {activeTab === "course" ? (
            <CourseManagementPanel
              courses={courses}
              loading={coursesLoading}
              canAdd={canAdd}
              canEdit={canEdit}
              canPublish={canPublish}
              canDelete={canDelete}
              canEnroll={canEnroll}
              viewMode={courseViewMode}
              onViewModeChange={onCourseViewModeChange}
              selectedRowKeys={selectedCourseRowKeys}
              onSelectionChange={onCourseSelectionChange}
              onViewCourse={onViewCourse}
              onAddCourse={onAddCourse}
              onEditCourse={onEditCourse}
              onPublishCourse={onPublishCourse}
              onOfflineCourse={onOfflineCourse}
              onDeleteCourse={onDeleteCourse}
              onEnrollCourse={onEnrollCourse}
            />
          ) : activeTab === "research" ? (
            <ResearchProjectPanel
              projects={researchProjects}
              loading={researchLoading}
              canAdd={canResearchAdd}
              canEdit={canResearchEdit}
              canStatus={canResearchStatus}
              selectedRowKeys={selectedResearchRowKeys}
              onSelectionChange={onResearchSelectionChange}
              onAdd={onAddResearch}
              onEdit={onEditResearch}
              onView={onViewResearch}
              onChangeStatus={onChangeResearchStatus}
              onReload={onReloadResearch}
            />
          ) : activeTab === "training" ? (
            <TrainingManagementPanel canManage={canTrainingManage} currentUserId={currentUserId} />
          ) : null}
        </div>
        {activeTab !== "training" ? <TrainingOverviewPanel /> : null}
      </section>

      {selectedCourse ? (
        <CourseDetailPanel
          course={selectedCourse}
          onClose={onCloseCourse}
          canRecordAdd={canRecordAdd}
          canRecordList={canRecordList}
          canRecordUpdate={canRecordUpdate}
          canRecordSubmit={canRecordSubmit}
          canRecordArchive={canRecordArchive}
          canRecordDelete={canRecordDelete}
          canGrade={canGrade}
        />
      ) : null}
    </div>
  );
}
