import { Button, Space, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

export type { HerbTableRecord } from "./types";

type HerbTableProps = {
  onView: (record: HerbTableRecord) => void;
};

const tableData: HerbTableRecord[] = [
  {
    id: "herb-001",
    herbName: "黄连",
    aliasName: "川连",
    categoryName: "根及根茎类",
    medicinalPart: "根茎",
    status: "enabled",
    region: "重庆石柱",
  },
  {
    id: "herb-002",
    herbName: "金银花",
    aliasName: "忍冬花",
    categoryName: "花叶类",
    medicinalPart: "花蕾",
    status: "enabled",
    region: "重庆秀山",
  },
  {
    id: "herb-003",
    herbName: "鱼腥草",
    aliasName: "折耳根",
    categoryName: "全草类",
    medicinalPart: "全草",
    status: "disabled",
    region: "重庆万州",
  },
  {
    id: "herb-004",
    herbName: "杜仲",
    aliasName: "思仲",
    categoryName: "皮类",
    medicinalPart: "树皮",
    status: "enabled",
    region: "重庆南川",
  },
];

export function HerbTable({ onView }: HerbTableProps) {
  const columns: TableColumnsType<HerbTableRecord> = [
    {
      title: "序号",
      key: "index",
      render: (_value, _record, index) => index + 1,
      width: 64,
    },
    {
      title: "药材名称",
      dataIndex: "herbName",
      key: "herbName",
      width: 220,
    },
    {
      title: "别名",
      dataIndex: "aliasName",
      key: "aliasName",
      ellipsis: true,
      width: 180,
    },
    {
      title: "所属分类",
      dataIndex: "categoryName",
      key: "categoryName",
      width: 150,
    },
    {
      title: "药用部位",
      dataIndex: "medicinalPart",
      key: "medicinalPart",
      width: 130,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status: HerbTableRecord["status"]) => (
        <Tag color={status === "enabled" ? "success" : "default"}>
          {status === "enabled" ? "启用" : "停用"}
        </Tag>
      ),
    },
    {
      title: "分布地区",
      dataIndex: "region",
      key: "region",
      ellipsis: true,
      width: 220,
    },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 160,
      render: (_value, record) => (
        <Space size={4}>
          <Button type="link" onClick={() => onView(record)}>
            查看
          </Button>
          <Button type="link" onClick={() => undefined}>
            编辑
          </Button>
          <Button type="link" onClick={() => undefined}>
            更多
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
      dataSource={tableData}
      pagination={{ pageSize: 10 }}
      rowKey="id"
      rowSelection={{}}
      scroll={{ x: "max-content" }}
      size="middle"
      sticky
    />
  );
}
