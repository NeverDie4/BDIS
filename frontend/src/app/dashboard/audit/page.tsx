"use client";

import { DashboardPage, DashboardPanel } from "@/components/dashboard/DashboardPage";
import { apiGet, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import type { PageResult } from "@/types/api";
import { App, Button, Form, Input, Select, Space, Table, Tag } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { RotateCcw, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

type AuditLog = {
  id: number;
  operatorName?: string;
  operationModule?: string;
  operationType?: string;
  bizType?: string;
  bizId?: number;
  operationResult?: string;
  errorMessage?: string;
  operationTime?: string;
};

type AuditFilter = {
  operationModule?: string;
  operationResult?: string;
  operatorId?: string;
};

export default function AuditPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<AuditFilter>();
  const [records, setRecords] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filters, setFilters] = useState<AuditFilter>({});

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await apiGet<PageResult<AuditLog>>("/audit-logs", {
        page,
        size,
        ...filters,
        operatorId: filters.operatorId ? Number(filters.operatorId) : undefined,
      });
      setRecords(result.records);
      setTotal(result.total);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, "审计日志加载失败"));
      }
    } finally {
      setLoading(false);
    }
  }, [filters, message, page, size]);

  useEffect(() => {
    void load();
  }, [load]);

  const columns: ColumnsType<AuditLog> = [
    { title: "操作人", dataIndex: "operatorName", width: 130, render: valueOrDash },
    { title: "模块", dataIndex: "operationModule", width: 190, render: valueOrDash },
    { title: "操作", dataIndex: "operationType", width: 190, render: valueOrDash },
    { title: "业务对象", dataIndex: "bizType", width: 140, render: valueOrDash },
    {
      title: "结果",
      dataIndex: "operationResult",
      width: 100,
      render: (value?: string) => (
        <Tag color={value === "SUCCESS" ? "green" : "red"}>{value || "-"}</Tag>
      ),
    },
    { title: "错误信息", dataIndex: "errorMessage", ellipsis: true, render: valueOrDash },
    {
      title: "时间",
      dataIndex: "operationTime",
      width: 180,
      render: (value?: string) => (value ? new Date(value).toLocaleString() : "-"),
    },
  ];

  function search(values: AuditFilter) {
    setPage(1);
    setFilters(values);
  }

  function reset() {
    form.resetFields();
    setPage(1);
    setFilters({});
  }

  function changePage(pagination: TablePaginationConfig) {
    setPage(pagination.current ?? 1);
    setSize(pagination.pageSize ?? 20);
  }

  return (
    <DashboardPage
      eyebrow="OPERATION AUDIT"
      title="操作审计"
      description="查询后台管理、个人设置与认证会话操作的执行结果。"
    >
      <DashboardPanel title="筛选条件">
        <Form form={form} layout="inline" onFinish={search}>
          <Form.Item label="模块" name="operationModule">
            <Select
              allowClear
              style={{ width: 210 }}
              options={[
                { label: "账号权限", value: "M02_M03_AUTH" },
                { label: "个人资料", value: "M02_USER_PROFILE" },
                { label: "账号安全", value: "M02_ACCOUNT_SECURITY" },
                { label: "个人设置", value: "M02_PERSONAL_SETTINGS" },
                { label: "认证会话", value: "M02_AUTH_SESSION" },
              ]}
            />
          </Form.Item>
          <Form.Item label="结果" name="operationResult">
            <Select
              allowClear
              style={{ width: 120 }}
              options={[
                { label: "成功", value: "SUCCESS" },
                { label: "失败", value: "FAILED" },
              ]}
            />
          </Form.Item>
          <Form.Item label="操作人ID" name="operatorId">
            <Input inputMode="numeric" style={{ width: 130 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button htmlType="submit" icon={<Search size={15} />} type="primary">
                查询
              </Button>
              <Button icon={<RotateCcw size={15} />} onClick={reset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </DashboardPanel>
      <DashboardPanel flush>
        <Table<AuditLog>
          columns={columns}
          dataSource={records}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1180 }}
          pagination={{ current: page, pageSize: size, total, showSizeChanger: true }}
          onChange={changePage}
        />
      </DashboardPanel>
    </DashboardPage>
  );
}

function valueOrDash(value?: string) {
  return value || "-";
}
