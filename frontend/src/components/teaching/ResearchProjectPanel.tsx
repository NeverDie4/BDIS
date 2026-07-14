import type { Key } from "react";
import { ResearchTable } from "./ResearchTable";
import { ResearchToolbar } from "./ResearchToolbar";
import styles from "./teaching.module.css";

type ResearchProjectPanelProps = {
  selectedRowKeys: Key[];
  onSelectionChange: (keys: Key[]) => void;
};

export function ResearchProjectPanel({ selectedRowKeys, onSelectionChange }: ResearchProjectPanelProps) {
  return (
    <section className={styles.managementSection}>
      <ResearchToolbar hasSelection={selectedRowKeys.length > 0} />
      <div className={styles.tableContainer}>
        <ResearchTable selectedRowKeys={selectedRowKeys} onSelectionChange={onSelectionChange} />
      </div>
    </section>
  );
}
