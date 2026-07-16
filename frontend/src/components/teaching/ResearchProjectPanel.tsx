import type { Key } from "react";
import { ResearchTable } from "./ResearchTable";
import { ResearchToolbar } from "./ResearchToolbar";
import type { ResearchRecord } from "./types";
import styles from "./teaching.module.css";

type ResearchProjectPanelProps = {
  projects: ResearchRecord[];
  loading?: boolean;
  canAdd?: boolean;
  canEdit?: boolean;
  canStatus?: boolean;
  selectedRowKeys: Key[];
  onSelectionChange: (keys: Key[]) => void;
  onAdd: () => void;
  onEdit: (project: ResearchRecord) => void;
  onView: (project: ResearchRecord) => void;
  onChangeStatus: (project: ResearchRecord, status: string) => void;
  onReload: () => void;
};

export function ResearchProjectPanel({ projects, loading, canAdd, canEdit, canStatus, selectedRowKeys, onSelectionChange, onAdd, onEdit, onView, onChangeStatus, onReload }: ResearchProjectPanelProps) {
  return (
    <section className={styles.managementSection}>
      <ResearchToolbar hasSelection={selectedRowKeys.length > 0} canAdd={canAdd} canEdit={canEdit} onAdd={onAdd} onEdit={() => { const selected = projects.find((item) => String(item.id) === String(selectedRowKeys[0])); if (selected) onEdit(selected); }} onReload={onReload} />
      <div className={styles.tableContainer}>
        <ResearchTable projects={projects} loading={loading} canEdit={canEdit} canStatus={canStatus} selectedRowKeys={selectedRowKeys} onSelectionChange={onSelectionChange} onView={onView} onEdit={onEdit} onChangeStatus={onChangeStatus} />
      </div>
    </section>
  );
}
