import { Button, Image, Space, Table } from "antd";
import type { TableColumnsType } from "antd";
import type { Key } from "react";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

export type { HerbTableRecord } from "./types";

type HerbTableProps = {
  canEdit?: boolean;
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

function getPlaceholderCharacter(name?: string) {
  return name?.match(/[\u4e00-\u9fff]/)?.[0] ?? "药";
}

export function HerbTable({
  canEdit = false,
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
      width: "5%",
    },
    {
      title: "图片",
      dataIndex: "coverImageUrl",
      key: "coverImageUrl",
      width: "8%",
      render: (value: string | undefined, record) =>
        value ? (
          <Image
            alt={record.herbName}
            className={styles.tableThumb}
            preview={false}
            src={value}
          />
        ) : (
          <span className={styles.tableThumbPlaceholder}>
            {getPlaceholderCharacter(record.herbName)}
          </span>
        ),
    },
    {
      title: "药材编号",
      dataIndex: "herbCode",
      key: "herbCode",
      width: "12%",
    },
    {
      title: "药材名称",
      dataIndex: "herbName",
      key: "herbName",
      width: "12%",
    },
    {
      title: "别名",
      dataIndex: "aliasName",
      key: "aliasName",
      ellipsis: true,
      width: "10%",
      render: (value?: string) => value || "-",
    },
    {
      title: "所属分类",
      dataIndex: "categoryName",
      key: "categoryName",
      width: "10%",
      render: (_value, record) => record.categoryName || record.category || "-",
    },
    {
      title: "药用部位",
      dataIndex: "medicinalPart",
      key: "medicinalPart",
      width: "9%",
      render: (value?: string) => value || "-",
    },
    {
      title: "分布地区",
      dataIndex: "distributionRegionText",
      key: "distributionRegionText",
      ellipsis: true,
      width: "18%",
      render: (value?: string) => value || "-",
    },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: "12%",
      render: (_value, record) => (
        <Space size={4}>
          <Button type="link" onClick={() => onView(record)}>
            查看
          </Button>
          {canEdit ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
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
      rowSelection={{ columnWidth: "4%", selectedRowKeys, onChange: onSelectionChange }}
      scroll={{ x: 1120 }}
      size="middle"
      sticky
      tableLayout="fixed"
    />
  );
}
