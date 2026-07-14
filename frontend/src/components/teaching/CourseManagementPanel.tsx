import type { Key } from "react";
import { CourseActionToolbar } from "./CourseActionToolbar";
import { CourseTable } from "./CourseTable";
import type { CourseRecord } from "./types";
import styles from "./teaching.module.css";

type CourseManagementPanelProps = {
  courses: CourseRecord[];
  loading?: boolean;
  canAdd?: boolean;
  canEdit?: boolean;
  canPublish?: boolean;
  canDelete?: boolean;
  canEnroll?: boolean;
  viewMode?: "all" | "mine";
  onViewModeChange?: (mode: "all" | "mine") => void;
  selectedRowKeys: Key[];
  onSelectionChange: (keys: Key[]) => void;
  onViewCourse: (course: CourseRecord) => void;
  onAddCourse: () => void;
  onEditCourse: (course: CourseRecord) => void;
  onPublishCourse: (course: CourseRecord) => void;
  onOfflineCourse: (course: CourseRecord) => void;
  onDeleteCourse: (course: CourseRecord) => void;
  onEnrollCourse?: (course: CourseRecord) => void;
};

export function CourseManagementPanel({
  courses,
  loading,
  canAdd,
  canEdit,
  canPublish,
  canDelete,
  canEnroll,
  viewMode,
  onViewModeChange,
  selectedRowKeys,
  onSelectionChange,
  onViewCourse,
  onAddCourse,
  onEditCourse,
  onPublishCourse,
  onOfflineCourse,
  onDeleteCourse,
  onEnrollCourse,
}: CourseManagementPanelProps) {
  return (
    <section className={styles.managementSection}>
      <CourseActionToolbar
        canAdd={canAdd}
        canDelete={canDelete}
        canEdit={canEdit}
        canPublish={canPublish}
        canEnroll={canEnroll}
        viewMode={viewMode}
        onViewModeChange={onViewModeChange}
        hasSelection={selectedRowKeys.length > 0}
        onAdd={onAddCourse}
        onDelete={() => onDeleteCourse(courses.find((course) => String(course.id) === String(selectedRowKeys[0]))!)}
        onEdit={() => onEditCourse(courses.find((course) => String(course.id) === String(selectedRowKeys[0]))!)}
        onOffline={() => onOfflineCourse(courses.find((course) => String(course.id) === String(selectedRowKeys[0]))!)}
        onPublish={() => onPublishCourse(courses.find((course) => String(course.id) === String(selectedRowKeys[0]))!)}
      />
      <div className={styles.tableContainer}>
        <CourseTable
          selectedRowKeys={selectedRowKeys}
          courses={courses}
          loading={loading}
          canEdit={canEdit}
          canPublish={canPublish}
          canDelete={canDelete}
          canEnroll={canEnroll}
          viewMode={viewMode}
          onEnrollCourse={onEnrollCourse}
          onSelectionChange={onSelectionChange}
          onViewCourse={onViewCourse}
          onEditCourse={onEditCourse}
          onPublishCourse={onPublishCourse}
          onOfflineCourse={onOfflineCourse}
          onDeleteCourse={onDeleteCourse}
        />
      </div>
    </section>
  );
}
