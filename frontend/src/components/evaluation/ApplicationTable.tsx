import { Button, Space } from "antd";
import type { TablePaginationConfig, TableProps } from "antd";
import { DataTable } from "@/components/common/DataTable";
import { StatusTag } from "@/components/common/StatusTag";
import type { EvaluationApplication } from "./types";
import { applicationStatusMeta } from "./utils";

type ApplicationTableProps = {
  records: EvaluationApplication[];
  total: number;
  pageNo: number;
  pageSize: number;
  selectedRowKeys: React.Key[];
  onSelectionChange: (keys: React.Key[]) => void;
  onPageChange: (page: number, pageSize: number) => void;
  onView: (application: EvaluationApplication) => void;
};

export function ApplicationTable({ records, total, pageNo, pageSize, selectedRowKeys, onSelectionChange, onPageChange, onView }: ApplicationTableProps) {
  const columns: TableProps<EvaluationApplication>["columns"] = [
    { title: "序号", key: "index", render: (_value, _record, index) => (pageNo - 1) * pageSize + index + 1, width: 64 },
    { title: "申报编号", dataIndex: "applicationNo", key: "applicationNo", width: 180 },
    { title: "申报标题", dataIndex: "title", key: "title", width: 220, ellipsis: true },
    { title: "申报类型", dataIndex: "applicationType", key: "applicationType", width: 120 },
    { title: "关联任务", dataIndex: "taskName", key: "taskName", width: 180, ellipsis: true },
    { title: "申报人", dataIndex: "applicantName", key: "applicantName", width: 100 },
    { title: "材料数", dataIndex: "materialCount", key: "materialCount", width: 90 },
    {
      title: "审核状态",
      dataIndex: "status",
      key: "status",
      width: 110,
      render: (status: EvaluationApplication["status"]) => {
        const meta = applicationStatusMeta[status];
        return <StatusTag label={meta.label} status={meta.tagStatus} />;
      },
    },
    { title: "提交时间", dataIndex: "submittedAt", key: "submittedAt", width: 170, render: (value?: string) => value ?? "未提交" },
    { title: "当前审核人", dataIndex: "reviewerName", key: "reviewerName", width: 110, render: (value?: string) => value ?? "—" },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 170,
      render: (_value, record) => (
        <Space size={2}>
          <Button type="link" onClick={() => onView(record)}>查看</Button>
          <Button disabled type="link" title="后续实现">编辑</Button>
          <Button disabled type="link" title="后续实现">更多</Button>
        </Space>
      ),
    },
  ];

  const pagination: TablePaginationConfig = {
    current: pageNo,
    pageSize,
    pageSizeOptions: [10, 20, 50],
    showSizeChanger: true,
    showTotal: (count) => `共 ${count} 条`,
    total,
  };

  return (
    <DataTable<EvaluationApplication>
      columns={columns}
      dataSource={records}
      onChange={(nextPagination) => onPageChange(nextPagination.current ?? 1, nextPagination.pageSize ?? pageSize)}
      pagination={pagination}
      rowKey="id"
      rowSelection={{ selectedRowKeys, onChange: onSelectionChange }}
      size="small"
      tableLayout="fixed"
      className="compactTable"
      scroll={{ x: 1500 }}
    />
  );
}
