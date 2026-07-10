import type { Key } from "react";
import { DownOutlined } from "@ant-design/icons";
import { Button, Dropdown, Space, Table } from "antd";
import type { MenuProps, TableColumnsType } from "antd";
import { TeachingStatusTag } from "./TeachingStatusTag";
import type { ResearchRecord } from "./types";
import styles from "./teaching.module.css";

type ResearchTableProps = {
  selectedRowKeys: Key[];
  onSelectionChange: (keys: Key[]) => void;
};

const researchData: ResearchRecord[] = [
  { id: "research-001", projectNo: "RP-001", projectName: "川渝道地药材资源研究", leader: "刘老师", period: "2025.09—2027.06", status: "ongoing", updatedAt: "2026-07-09" },
  { id: "research-002", projectNo: "RP-002", projectName: "黄连生长环境评价", leader: "孙老师", period: "2026.01—2027.12", status: "ongoing", updatedAt: "2026-07-05" },
  { id: "research-003", projectNo: "RP-003", projectName: "中药标本数字化规范", leader: "吴老师", period: "2024.06—2026.05", status: "completed", updatedAt: "2026-06-30" },
  { id: "research-004", projectNo: "RP-004", projectName: "药材质量追溯方法", leader: "郑老师", period: "2026.03—2028.02", status: "applying", updatedAt: "2026-06-27" },
  { id: "research-005", projectNo: "RP-005", projectName: "教学标本资源共享机制", leader: "冯老师", period: "2025.03—2026.12", status: "completed", updatedAt: "2026-06-22" },
];

const moreItems: MenuProps["items"] = [
  { key: "materials", label: "过程材料" },
  { key: "results", label: "阶段成果" },
];

export function ResearchTable({ selectedRowKeys, onSelectionChange }: ResearchTableProps) {
  const columns: TableColumnsType<ResearchRecord> = [
    {
      title: "序号",
      key: "index",
      width: 56,
      render: (_value, _record, index) => index + 1,
    },
    { title: "课题名称", dataIndex: "projectName", key: "projectName", ellipsis: true, width: 190 },
    { title: "负责人", dataIndex: "leader", key: "leader", width: 88 },
    { title: "研究周期", dataIndex: "period", key: "period", ellipsis: true, width: 142 },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 82,
      render: (status: ResearchRecord["status"]) => <TeachingStatusTag status={status} />,
    },
    { title: "更新时间", dataIndex: "updatedAt", key: "updatedAt", ellipsis: true, width: 108 },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 130,
      render: () => (
        <Space size={2}>
          <Button className={styles.actionLink} type="link" onClick={() => undefined}>查看</Button>
          <Dropdown menu={{ items: moreItems }} trigger={["click"]}>
            <Button className={styles.actionLink} type="link" onClick={(event) => event.preventDefault()}>
              更多 <DownOutlined />
            </Button>
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <Table<ResearchRecord>
      className={styles.dataTable}
      columns={columns}
      dataSource={researchData}
      pagination={{ pageSize: 5, showSizeChanger: false, showTotal: (total) => `共 ${total} 条` }}
      rowKey="id"
      rowSelection={{ selectedRowKeys, onChange: onSelectionChange }}
      scroll={{ x: "max-content" }}
      size="small"
    />
  );
}
