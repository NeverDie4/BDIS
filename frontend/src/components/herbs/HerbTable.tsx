import { Button, Space, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import type { Key } from "react";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

export type { HerbTableRecord } from "./types";

type HerbTableProps = {
  loading?: boolean;
  records?: HerbTableRecord[];
  page?: number;
  pageSize?: number;
  total?: number;
  selectedRowKeys?: Key[];
  onSelectionChange?: (keys: Key[]) => void;
  onPageChange?: (page: number, pageSize: number) => void;
  onView: (record: HerbTableRecord) => void;
  onEdit: (record: HerbTableRecord) => void;
};

function statusColor(status?: number) {
  return status === 1 ? "success" : "default";
}

export function HerbTable({
  loading = false,
  records = [],
  page = 1,
  pageSize = 10,
  total = 0,
  selectedRowKeys = [],
  onSelectionChange = () => undefined,
  onPageChange = () => undefined,
  onView,
  onEdit,
}: HerbTableProps) {
  const columns: TableColumnsType<HerbTableRecord> = [
    {
      title: "序号",
      key: "index",
      render: (_value, _record, index) => (page - 1) * pageSize + index + 1,
      width: 64,
    },
    {
      title: "药材编号",
      dataIndex: "herbCode",
      key: "herbCode",
      width: 150,
    },
    {
      title: "药材名称",
      dataIndex: "herbName",
      key: "herbName",
      width: 180,
    },
    {
      title: "别名",
      dataIndex: "aliasName",
      key: "aliasName",
      ellipsis: true,
      width: 160,
      render: (value?: string) => value || "-",
    },
    {
      title: "所属分类",
      dataIndex: "categoryName",
      key: "categoryName",
      width: 150,
      render: (_value, record) => record.categoryName || record.category || "-",
    },
    {
      title: "药用部位",
      dataIndex: "medicinalPart",
      key: "medicinalPart",
      width: 130,
      render: (value?: string) => value || "-",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (_value, record) => (
        <Tag color={statusColor(record.status)}>{record.statusText || (record.status === 1 ? "启用" : "停用")}</Tag>
      ),
    },
    {
      title: "分布地区",
      dataIndex: "distributionRegionText",
      key: "distributionRegionText",
      ellipsis: true,
      width: 220,
      render: (value?: string) => value || "-",
    },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 120,
      render: (_value, record) => (
        <Space size={4}>
          <Button type="link" onClick={() => onView(record)}>
            查看
          </Button>
          <Button type="link" onClick={() => onEdit(record)}>
            编辑
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Table<HerbTableRecord>
      bordered={false}
      className={styles.table}
      columns={columns}
      dataSource={records}
      loading={loading}
      pagination={{
        current: page,
        pageSize,
        total,
        showSizeChanger: true,
        onChange: onPageChange,
      }}
      rowKey="id"
      rowSelection={{ selectedRowKeys, onChange: onSelectionChange }}
      scroll={{ x: "max-content" }}
      size="middle"
      sticky
    />
  );
}