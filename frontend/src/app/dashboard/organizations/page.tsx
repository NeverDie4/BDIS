"use client";

import { apiDelete, apiGet, apiPost } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { Organization, PageResult } from "@/types/api";
import { App, Button, Form, Input, Modal, Popconfirm, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Plus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

type OrganizationForm = {
  organizationNo: string;
  organizationName: string;
  organizationType?: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
};

export default function OrganizationsPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [rows, setRows] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<OrganizationForm>();

  async function load() {
    setLoading(true);
    try {
      const page = await apiGet<PageResult<Organization>>("/organizations", { page: 1, size: 100 });
      setRows(page.records);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const columns = useMemo<ColumnsType<Organization>>(
    () => [
      { title: "编号", dataIndex: "organizationNo" },
      { title: "名称", dataIndex: "organizationName" },
      { title: "类型", dataIndex: "organizationType" },
      { title: "联系人", dataIndex: "contactName" },
      { title: "联系电话", dataIndex: "contactPhone" },
      {
        title: "操作",
        key: "action",
        width: 120,
        render: (_, record) =>
          hasPermission("auth:organization:delete") ? (
            <Popconfirm
              title="确认删除该机构？"
              onConfirm={async () => {
                await apiDelete<void>(`/organizations/${record.id}`);
                message.success("已删除");
                await load();
              }}
            >
              <Button danger size="small">
                删除
              </Button>
            </Popconfirm>
          ) : null,
      },
    ],
    [hasPermission, message],
  );

  async function submit(values: OrganizationForm) {
    await apiPost<number>("/organizations", values);
    message.success("机构已创建");
    setOpen(false);
    form.resetFields();
    await load();
  }

  return (
    <section className="page-section">
      <div className="page-title-row">
        <Typography.Title level={3}>组织机构</Typography.Title>
        {hasPermission("auth:organization:create") ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={() => setOpen(true)}>
            新增机构
          </Button>
        ) : null}
      </div>
      <Table<Organization> rowKey="id" loading={loading} columns={columns} dataSource={rows} />
      <Modal title="新增机构" open={open} onCancel={() => setOpen(false)} footer={null} destroyOnHidden>
        <Form<OrganizationForm> form={form} layout="vertical" onFinish={submit}>
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
          <Button type="primary" htmlType="submit" block>
            保存
          </Button>
        </Form>
      </Modal>
    </section>
  );
}
