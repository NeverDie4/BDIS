"use client";

import {
  DashboardFormModal,
  DashboardMetric,
  DashboardPage,
  DashboardPanel,
  DashboardStatus,
} from "@/components/dashboard/DashboardPage";
import {
  apiDelete,
  apiGet,
  apiPost,
  apiPut,
  getApiErrorMessage,
  isAuthRedirectError,
} from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { Organization, PageResult } from "@/types/api";
import { App, Button, Form, Input, Popconfirm, Select, Space, Table } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Building2, MapPin, Pencil, Plus, UserRoundCheck } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

type OrganizationForm = {
  organizationNo?: string;
  organizationName?: string;
  organizationType?: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  status?: number;
};

export default function OrganizationsPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [rows, setRows] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Organization | null>(null);
  const [form] = Form.useForm<OrganizationForm>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await apiGet<PageResult<Organization>>("/organizations", { page: 1, size: 100 });
      setRows(page.records);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, "机构数据加载失败"));
      }
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 1 });
    setOpen(true);
  }

  function openEdit(organization: Organization) {
    setEditing(organization);
    form.setFieldsValue(organization);
    setOpen(true);
  }

  async function remove(organization: Organization) {
    try {
      await apiDelete<void>(`/organizations/${organization.id}`);
      message.success("机构已删除");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "机构删除失败"));
    }
  }

  const columns: ColumnsType<Organization> = [
    { title: "编号", dataIndex: "organizationNo", width: 150 },
    { title: "名称", dataIndex: "organizationName", width: 220 },
    { title: "类型", dataIndex: "organizationType", width: 140, render: valueOrDash },
    { title: "联系人", dataIndex: "contactName", width: 130, render: valueOrDash },
    { title: "联系电话", dataIndex: "contactPhone", width: 150, render: valueOrDash },
    { title: "地址", dataIndex: "address", render: valueOrDash },
    {
      title: "状态",
      dataIndex: "status",
      width: 90,
      render: (value?: number) => <DashboardStatus enabled={value === 1} />,
    },
    {
      title: "操作",
      key: "action",
      width: 176,
      fixed: "right",
      render: (_, record) => (
        <Space>
          {hasPermission("auth:organization:update") ? (
            <Button size="small" icon={<Pencil size={14} />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          ) : null}
          {hasPermission("auth:organization:delete") ? (
            <Popconfirm title="确认删除该机构？" onConfirm={() => remove(record)}>
              <Button danger size="small">
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  async function submit(values: OrganizationForm) {
    if (editing) {
      await apiPut<void>(`/organizations/${editing.id}`, values);
      message.success("机构已更新");
    } else {
      await apiPost<number>("/organizations", values);
      message.success("机构已创建");
    }
    setOpen(false);
    form.resetFields();
    await load();
  }

  return (
    <DashboardPage
      eyebrow="ORGANIZATION DIRECTORY"
      title="组织机构"
      description="维护系统中的机构主体、联系信息和启停状态，为人员与部门归属提供基础。"
      actions={
        hasPermission("auth:organization:create") ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
            新增机构
          </Button>
        ) : undefined
      }
      metrics={
        <>
          <DashboardMetric icon={Building2} label="机构总数" value={rows.length} />
          <DashboardMetric
            icon={UserRoundCheck}
            label="启用机构"
            value={rows.filter((row) => row.status === 1).length}
          />
          <DashboardMetric
            icon={MapPin}
            label="已录入地址"
            value={rows.filter((row) => Boolean(row.address)).length}
          />
        </>
      }
    >
      <DashboardPanel flush>
        <Table<Organization>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 1200 }}
        />
      </DashboardPanel>

      <DashboardFormModal<OrganizationForm>
        title={editing ? `编辑机构：${editing.organizationName}` : "新增机构"}
        open={open}
        form={form}
        onCancel={() => setOpen(false)}
        onFinish={submit}
        errorMessage={editing ? "机构更新失败" : "机构创建失败"}
      >
        <Form.Item
          name="organizationNo"
          label="机构编号"
          rules={[{ required: true, message: "请输入机构编号" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="organizationName"
          label="机构名称"
          rules={[{ required: true, message: "请输入机构名称" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="organizationType" label="机构类型">
          <Input />
        </Form.Item>
        <Form.Item name="contactName" label="联系人">
          <Input />
        </Form.Item>
        <Form.Item name="contactPhone" label="联系电话">
          <Input />
        </Form.Item>
        <Form.Item name="address" label="地址">
          <Input />
        </Form.Item>
        <Form.Item name="status" label="状态">
          <Select
            options={[
              { label: "启用", value: 1 },
              { label: "停用", value: 0 },
            ]}
          />
        </Form.Item>
      </DashboardFormModal>
    </DashboardPage>
  );
}

function valueOrDash(value?: string) {
  return value || "-";
}
