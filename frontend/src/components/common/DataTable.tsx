"use client";

import { Table } from "antd";
import type { TableProps } from "antd";
import styles from "./DataTable.module.css";

type DataTableProps<RecordType extends object> = {
  columns: TableProps<RecordType>["columns"];
  dataSource?: TableProps<RecordType>["dataSource"];
  rowKey?: TableProps<RecordType>["rowKey"];
  pagination?: TableProps<RecordType>["pagination"];
  loading?: TableProps<RecordType>["loading"];
  rowSelection?: TableProps<RecordType>["rowSelection"];
  className?: string;
  scroll?: TableProps<RecordType>["scroll"];
  onChange?: TableProps<RecordType>["onChange"];
  size?: TableProps<RecordType>["size"];
  tableLayout?: TableProps<RecordType>["tableLayout"];
};

export function DataTable<RecordType extends object>({
  columns,
  dataSource,
  rowKey,
  pagination,
  loading,
  rowSelection,
  className,
  scroll,
  onChange,
  size,
  tableLayout,
}: DataTableProps<RecordType>) {
  return (
    <div className={`${styles.tableShell} ${className ?? ""}`}>
      <Table<RecordType>
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={pagination}
        rowKey={rowKey}
        rowSelection={rowSelection}
        scroll={scroll}
        onChange={onChange}
        size={size}
        tableLayout={tableLayout}
      />
    </div>
  );
}
