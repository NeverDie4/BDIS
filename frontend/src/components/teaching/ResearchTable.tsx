import type { Key } from "react";
import { DownOutlined } from "@ant-design/icons";
import { Button, Dropdown, Empty, Space, Table } from "antd";
import type { MenuProps, TableColumnsType } from "antd";
import { TeachingStatusTag } from "./TeachingStatusTag";
import type { ResearchRecord } from "./types";
import styles from "./teaching.module.css";

type ResearchTableProps = { projects: ResearchRecord[]; loading?: boolean; canEdit?: boolean; canStatus?: boolean; selectedRowKeys: Key[]; onSelectionChange: (keys: Key[]) => void; onView: (project: ResearchRecord) => void; onEdit: (project: ResearchRecord) => void; onChangeStatus: (project: ResearchRecord, status: string) => void };

export function ResearchTable({ projects, loading, canEdit, canStatus, selectedRowKeys, onSelectionChange, onView, onEdit, onChangeStatus }: ResearchTableProps) {
  const columns: TableColumnsType<ResearchRecord> = [
    { title: "序号", key: "index", width: 56, render: (_value, _record, index) => index + 1 },
    { title: "课题名称", dataIndex: "projectName", key: "projectName", ellipsis: true, width: 250 },
    { title: "负责人", dataIndex: "leader", key: "leader", width: 100 },
    { title: "研究周期", dataIndex: "period", key: "period", ellipsis: true, width: 150 },
    { title: "状态", dataIndex: "status", key: "status", width: 92, render: (status: ResearchRecord["status"]) => <TeachingStatusTag status={status} /> },
    { title: "更新时间", dataIndex: "updatedAt", key: "updatedAt", ellipsis: true, width: 120 },
    { title: "操作", key: "actions", fixed: "right", width: 190, render: (_value, record) => {
      const items: MenuProps["items"] = [];
      if (canStatus && record.status === "planning") items.push({ key: "ongoing", label: "开始课题" });
      if (canStatus && record.status === "ongoing") { items.push({ key: "suspended", label: "暂停课题" }); items.push({ key: "completed", label: "完成课题" }); }
      if (canStatus && record.status === "suspended") items.push({ key: "ongoing", label: "恢复课题" });
      return <Space size={2}><Button className={styles.actionLink} type="link" onClick={() => onView(record)}>查看</Button>{canEdit ? <Button className={styles.actionLink} type="link" onClick={() => onEdit(record)}>编辑</Button> : null}{items.length ? <Dropdown menu={{ items, onClick: ({ key }) => onChangeStatus(record, key) }} trigger={["click"]}><Button className={styles.actionLink} type="link" onClick={(event) => event.preventDefault()}>更多 <DownOutlined /></Button></Dropdown> : null}</Space>;
    } },
  ];
  return <Table<ResearchRecord> className={styles.dataTable} columns={columns} dataSource={projects} locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无课题" /> }} loading={loading} pagination={{ pageSize: 5, showSizeChanger: false, showTotal: (total) => `共 ${total} 条` }} rowKey="id" rowSelection={{ selectedRowKeys, onChange: onSelectionChange }} scroll={{ x: "max-content" }} size="small" />;
}
