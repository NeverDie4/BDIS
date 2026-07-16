"use client";

import { App, Button, Form, Input, InputNumber, Select, Space, Table, Tag } from "antd";
import type { FormInstance } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { RotateCcw, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { DashboardPanel } from "@/components/dashboard/DashboardPage";
import { apiGet, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import type { PageResult } from "@/types/api";

type OperationLog = {
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

type LoginLog = {
  id: number;
  userId?: number;
  username?: string;
  loginResult?: string;
  failureReason?: string;
  ipAddress?: string;
  loggedInAt?: string;
};

type FileAccessLog = {
  id: number;
  fileId?: number;
  operatorName?: string;
  accessType?: string;
  accessResult?: string;
  ipAddress?: string;
  operationTime?: string;
};

type DataSyncLog = {
  id: number;
  syncType?: string;
  sourceType?: string;
  targetType?: string;
  taskId?: number;
  businessType?: string;
  businessId?: number;
  externalNo?: string;
  syncStatus?: string;
  successCount?: number;
  failureCount?: number;
  failureReason?: string;
  operationTime?: string;
};

type OperationFilter = {
  operationModule?: string;
  operationResult?: string;
  operatorId?: number;
};

type LoginFilter = {
  username?: string;
  loginResult?: string;
};

type FileAccessFilter = {
  fileId?: number;
  operatorId?: number;
  accessType?: string;
};

type DataSyncFilter = {
  syncType?: string;
  syncStatus?: string;
  taskId?: number;
  externalNo?: string;
};

export function OperationAuditPanel() {
  const [form] = Form.useForm<OperationFilter>();
  const query = useAuditPage<OperationLog, OperationFilter>("/audit-logs", "操作日志加载失败");
  const columns: ColumnsType<OperationLog> = [
    { title: "操作人", dataIndex: "operatorName", width: 140, render: valueOrDash },
    { title: "模块", dataIndex: "operationModule", width: 190, render: valueOrDash },
    { title: "动作", dataIndex: "operationType", width: 190, render: valueOrDash },
    { title: "业务对象", dataIndex: "bizType", width: 150, render: valueOrDash },
    { title: "业务 ID", dataIndex: "bizId", width: 110, render: valueOrDash },
    {
      title: "结果",
      dataIndex: "operationResult",
      width: 100,
      render: resultTag,
    },
    { title: "错误信息", dataIndex: "errorMessage", ellipsis: true, render: valueOrDash },
    { title: "时间", dataIndex: "operationTime", width: 180, render: dateTimeOrDash },
  ];

  return (
    <AuditPanelLayout
      columns={columns}
      form={form}
      query={query}
      scrollX={1240}
      filters={
        <>
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
                { label: "文件资源", value: "M05_FILE" },
                { label: "药材资源", value: "M07_M10_HERB" },
                { label: "图谱识别", value: "M11_SPECTRUM" },
                { label: "数据交换", value: "M19_SOAP" },
                { label: "系统其他", value: "SYSTEM" },
              ]}
            />
          </Form.Item>
          <Form.Item label="结果" name="operationResult">
            <ResultSelect />
          </Form.Item>
          <Form.Item label="操作人 ID" name="operatorId">
            <InputNumber min={1} precision={0} style={{ width: 130 }} />
          </Form.Item>
        </>
      }
    />
  );
}

export function LoginAuditPanel() {
  const [form] = Form.useForm<LoginFilter>();
  const query = useAuditPage<LoginLog, LoginFilter>("/login-logs", "登录日志加载失败");
  const columns: ColumnsType<LoginLog> = [
    { title: "用户 ID", dataIndex: "userId", width: 110, render: valueOrDash },
    { title: "账号", dataIndex: "username", width: 170, render: valueOrDash },
    { title: "结果", dataIndex: "loginResult", width: 100, render: resultTag },
    { title: "失败原因", dataIndex: "failureReason", ellipsis: true, render: valueOrDash },
    { title: "IP 地址", dataIndex: "ipAddress", width: 160, render: valueOrDash },
    { title: "时间", dataIndex: "loggedInAt", width: 180, render: dateTimeOrDash },
  ];

  return (
    <AuditPanelLayout
      columns={columns}
      form={form}
      query={query}
      scrollX={980}
      filters={
        <>
          <Form.Item label="账号" name="username">
            <Input allowClear style={{ width: 180 }} />
          </Form.Item>
          <Form.Item label="结果" name="loginResult">
            <ResultSelect />
          </Form.Item>
        </>
      }
    />
  );
}

export function FileAccessAuditPanel() {
  const [form] = Form.useForm<FileAccessFilter>();
  const query = useAuditPage<FileAccessLog, FileAccessFilter>(
    "/file-access-logs",
    "文件访问日志加载失败",
  );
  const columns: ColumnsType<FileAccessLog> = [
    { title: "操作人", dataIndex: "operatorName", width: 150, render: valueOrDash },
    { title: "文件 ID", dataIndex: "fileId", width: 110, render: valueOrDash },
    { title: "访问类型", dataIndex: "accessType", width: 130, render: valueOrDash },
    { title: "结果", dataIndex: "accessResult", width: 100, render: resultTag },
    { title: "IP 地址", dataIndex: "ipAddress", width: 160, render: valueOrDash },
    { title: "时间", dataIndex: "operationTime", width: 180, render: dateTimeOrDash },
  ];

  return (
    <AuditPanelLayout
      columns={columns}
      form={form}
      query={query}
      scrollX={920}
      filters={
        <>
          <Form.Item label="文件 ID" name="fileId">
            <InputNumber min={1} precision={0} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item label="操作人 ID" name="operatorId">
            <InputNumber min={1} precision={0} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item label="访问类型" name="accessType">
            <Select
              allowClear
              style={{ width: 140 }}
              options={[
                { label: "预览", value: "PREVIEW" },
                { label: "下载", value: "DOWNLOAD" },
              ]}
            />
          </Form.Item>
        </>
      }
    />
  );
}

export function DataSyncAuditPanel() {
  const [form] = Form.useForm<DataSyncFilter>();
  const query = useAuditPage<DataSyncLog, DataSyncFilter>(
    "/data-sync-logs",
    "数据同步日志加载失败",
  );
  const columns: ColumnsType<DataSyncLog> = [
    { title: "同步类型", dataIndex: "syncType", width: 140, render: valueOrDash },
    { title: "来源", dataIndex: "sourceType", width: 130, render: valueOrDash },
    { title: "目标", dataIndex: "targetType", width: 130, render: valueOrDash },
    { title: "任务 ID", dataIndex: "taskId", width: 110, render: valueOrDash },
    { title: "业务类型", dataIndex: "businessType", width: 150, render: valueOrDash },
    { title: "业务 ID", dataIndex: "businessId", width: 110, render: valueOrDash },
    { title: "外部编号", dataIndex: "externalNo", width: 150, render: valueOrDash },
    { title: "状态", dataIndex: "syncStatus", width: 100, render: resultTag },
    { title: "成功数", dataIndex: "successCount", width: 90, render: valueOrDash },
    { title: "失败数", dataIndex: "failureCount", width: 90, render: valueOrDash },
    { title: "失败原因", dataIndex: "failureReason", ellipsis: true, render: valueOrDash },
    { title: "时间", dataIndex: "operationTime", width: 180, render: dateTimeOrDash },
  ];

  return (
    <AuditPanelLayout
      columns={columns}
      form={form}
      query={query}
      scrollX={1480}
      filters={
        <>
          <Form.Item label="同步类型" name="syncType">
            <Input allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item label="状态" name="syncStatus">
            <ResultSelect />
          </Form.Item>
          <Form.Item label="任务 ID" name="taskId">
            <InputNumber min={1} precision={0} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item label="外部编号" name="externalNo">
            <Input allowClear style={{ width: 160 }} />
          </Form.Item>
        </>
      }
    />
  );
}

type AuditPageState<T, F> = {
  records: T[];
  loading: boolean;
  page: number;
  size: number;
  total: number;
  search: (values: F) => void;
  reset: () => void;
  changePage: (pagination: TablePaginationConfig) => void;
};

function useAuditPage<T, F extends object>(
  endpoint: string,
  errorMessage: string,
  normalize?: (filters: F) => Record<string, unknown>,
): AuditPageState<T, F> {
  const { message } = App.useApp();
  const [records, setRecords] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filters, setFilters] = useState<F>({} as F);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const normalized = normalize ? normalize(filters) : (filters as Record<string, unknown>);
      const result = await apiGet<PageResult<T>>(endpoint, { page, size, ...normalized });
      setRecords(result.records);
      setTotal(result.total);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, errorMessage));
      }
    } finally {
      setLoading(false);
    }
  }, [endpoint, errorMessage, filters, message, normalize, page, size]);

  useEffect(() => {
    void load();
  }, [load]);

  return {
    records,
    loading,
    page,
    size,
    total,
    search: (values) => {
      setPage(1);
      setFilters(values);
    },
    reset: () => {
      setPage(1);
      setFilters({} as F);
    },
    changePage: (pagination) => {
      setPage(pagination.current ?? 1);
      setSize(pagination.pageSize ?? 20);
    },
  };
}

function AuditPanelLayout<T extends object, F extends object>({
  columns,
  filters,
  form,
  query,
  scrollX,
}: {
  columns: ColumnsType<T>;
  filters: React.ReactNode;
  form: FormInstance<F>;
  query: AuditPageState<T, F>;
  scrollX: number;
}) {
  return (
    <Space direction="vertical" size={18} style={{ display: "flex" }}>
      <DashboardPanel title="筛选条件">
        <Form form={form} layout="inline" onFinish={query.search}>
          {filters}
          <Form.Item>
            <Space>
              <Button htmlType="submit" icon={<Search size={15} />} type="primary">
                查询
              </Button>
              <Button
                icon={<RotateCcw size={15} />}
                onClick={() => {
                  form.resetFields();
                  query.reset();
                }}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </DashboardPanel>
      <DashboardPanel flush>
        <Table<T>
          columns={columns}
          dataSource={query.records}
          loading={query.loading}
          rowKey={(record) => String((record as { id: number }).id)}
          scroll={{ x: scrollX }}
          pagination={{
            current: query.page,
            pageSize: query.size,
            total: query.total,
            showSizeChanger: true,
          }}
          onChange={query.changePage}
        />
      </DashboardPanel>
    </Space>
  );
}

function ResultSelect() {
  return (
    <Select
      allowClear
      style={{ width: 120 }}
      options={[
        { label: "成功", value: "SUCCESS" },
        { label: "失败", value: "FAILED" },
      ]}
    />
  );
}

function valueOrDash(value?: string | number) {
  return value ?? "-";
}

function dateTimeOrDash(value?: string) {
  return value ? new Date(value).toLocaleString() : "-";
}

function resultTag(value?: string) {
  const normalized = value?.toUpperCase();
  const success = normalized === "SUCCESS" || normalized === "COMPLETED";
  const failed = normalized === "FAILED" || normalized === "FAILURE";
  const label = success ? "成功" : failed ? "失败" : value || "-";
  return <Tag color={success ? "green" : failed ? "red" : "default"}>{label}</Tag>;
}
