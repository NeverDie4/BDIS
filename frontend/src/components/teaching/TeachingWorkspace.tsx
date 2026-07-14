import type { Key } from "react";
import { CourseDetailPanel } from "./CourseDetailPanel";
import { CourseManagementPanel } from "./CourseManagementPanel";
import { ResearchProjectPanel } from "./ResearchProjectPanel";
import { TrainingOverviewPanel } from "./TrainingOverviewPanel";
import type { CourseRecord } from "./types";
import styles from "./teaching.module.css";

type TeachingWorkspaceProps = {
  selectedCourse: CourseRecord | null;
  selectedCourseRowKeys: Key[];
  selectedResearchRowKeys: Key[];
  onViewCourse: (course: CourseRecord) => void;
  onCloseCourse: () => void;
  onCourseSelectionChange: (keys: Key[]) => void;
  onResearchSelectionChange: (keys: Key[]) => void;
};

export function TeachingWorkspace({
  selectedCourse,
  selectedCourseRowKeys,
  selectedResearchRowKeys,
  onViewCourse,
  onCloseCourse,
  onCourseSelectionChange,
  onResearchSelectionChange,
}: TeachingWorkspaceProps) {
  return (
    <div className={`${styles.workspace} ${selectedCourse ? styles.workspaceWithDetail : ""}`}>
      <section className={styles.mainContent}>
        <div className={styles.managementGrid}>
          <CourseManagementPanel
            selectedRowKeys={selectedCourseRowKeys}
            onSelectionChange={onCourseSelectionChange}
            onViewCourse={onViewCourse}
          />
          <ResearchProjectPanel
            selectedRowKeys={selectedResearchRowKeys}
            onSelectionChange={onResearchSelectionChange}
          />
        </div>
        <TrainingOverviewPanel />
      </section>

      {selectedCourse ? (
        <CourseDetailPanel course={selectedCourse} onClose={onCloseCourse} />
      ) : null}
    </div>
  );
}
