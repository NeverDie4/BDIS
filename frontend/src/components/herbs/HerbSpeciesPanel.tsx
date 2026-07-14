import type { Key } from "react";
import { HerbActionToolbar } from "./HerbActionToolbar";
import { HerbFilterBar } from "./HerbFilterBar";
import { HerbTable } from "./HerbTable";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

type HerbSpeciesPanelProps = {
  records?: HerbTableRecord[];
  loading?: boolean;
  selectedRowKeys?: Key[];
  onSelectionChange?: (keys: Key[]) => void;
  onCreate: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onExport: () => void;
  onRefresh: () => void;
  onView: (record: HerbTableRecord) => void;
  onEditRecord: (record: HerbTableRecord) => void;
};

export function HerbSpeciesPanel({
  records = [],
  loading = false,
  selectedRowKeys = [],
  onSelectionChange = () => undefined,
  onCreate,
  onEdit,
  onDelete,
  onExport,
  onRefresh,
  onView,
  onEditRecord,
}: HerbSpeciesPanelProps) {
  return (
    <div className={styles.speciesPanel}>
      <HerbFilterBar />
      <HerbActionToolbar
        selectedCount={selectedRowKeys.length}
        onCreate={onCreate}
        onEdit={onEdit}
        onDelete={onDelete}
        onExport={onExport}
        onRefresh={onRefresh}
      />
      <HerbTable
        loading={loading}
        records={records}
        selectedRowKeys={selectedRowKeys}
        onSelectionChange={onSelectionChange}
        onView={onView}
        onEdit={onEditRecord}
      />
    </div>
  );
}