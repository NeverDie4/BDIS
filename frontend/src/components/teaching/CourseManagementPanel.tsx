import type { Key } from "react";
import { CourseActionToolbar } from "./CourseActionToolbar";
import { CourseTable } from "./CourseTable";
import type { CourseRecord } from "./types";
import styles from "./teaching.module.css";

type CourseManagementPanelProps = {
  selectedRowKeys: Key[];
  onSelectionChange: (keys: Key[]) => void;
  onViewCourse: (course: CourseRecord) => void;
};

export function CourseManagementPanel({
  selectedRowKeys,
  onSelectionChange,
  onViewCourse,
}: CourseManagementPanelProps) {
  return (
    <section className={styles.managementSection}>
      <CourseActionToolbar hasSelection={selectedRowKeys.length > 0} />
      <div className={styles.tableContainer}>
        <CourseTable
          selectedRowKeys={selectedRowKeys}
          onSelectionChange={onSelectionChange}
          onViewCourse={onViewCourse}
        />
      </div>
    </section>
  );
}
