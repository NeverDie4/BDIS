"use client";

import { apiDelete, apiGet, apiPost } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { Department, Organization, PageResult } from "@/types/api";
import { App, Button, Form, Input, Modal, Popconfirm, Select, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Plus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

type DepartmentForm = {
  departmentNo: string;
  departmentName: string;
  organizationId: number;
  parentId?: number;
  sortOrder?: number;
};

export default function DepartmentsPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<DepartmentForm>();

  async function load() {
    setLoading(true);
    try {
      const [departmentTree, organizationPage] = await Promise.all([
        apiGet<Department[]>("/departments"),
        apiGet<PageResult<Organization>>("/organizations", { page: 1, size: 100 }),
      ]);
      setDepartments(departmentTree);
      setOrganizations(organizationPage.records);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const flatDepartments = useMemo(() => flattenDepartments(departments), [departments]);

  const columns = useMemo<ColumnsType<Department>>(
    () => [
      { title: "部门编号", dataIndex: "departmentNo" },
      { title: "部门名称", dataIndex: "departmentName" },
      { title: "机构 ID", dataIndex: "organizationId" },
      { title: "排序", dataIndex: "sortOrder" },
      {
        title: "操作",
        key: "action",
        width: 120,
        render: (_, record) =>
          hasPermission("auth:department:delete") ? (
            <Popconfirm
              title="确认删除该部门？"
              onConfirm={async () => {
                await apiDelete<void>(`/departments/${record.id}`);
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

  async function submit(values: DepartmentForm) {
    await apiPost<number>("/departments", values);
    message.success("部门已创建");
    setOpen(false);
    form.resetFields();
    await load();
  }

  return (
    <section className="page-section">
      <div className="page-title-row">
        <Typography.Title level={3}>部门管理</Typography.Title>
        {hasPermission("auth:department:create") ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={() => setOpen(true)}>
            新增部门
          </Button>
        ) : null}
      </div>
      <Table<Department>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={departments}
        pagination={false}
      />
      <Modal title="新增部门" open={open} onCancel={() => setOpen(false)} footer={null} destroyOnHidden>
        <Form<DepartmentForm> form={form} layout="vertical" onFinish={submit}>
          <Form.Item
            name="departmentNo"
            label="部门编号"
            rules={[{ required: true, message: "请输入部门编号" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="departmentName"
            label="部门名称"
            rules={[{ required: true, message: "请输入部门名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="organizationId"
            label="所属机构"
            rules={[{ required: true, message: "请选择所属机构" }]}
          >
            <Select
              options={organizations.map((organization) => ({
                label: organization.organizationName,
                value: organization.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="parentId" label="上级部门">
            <Select
              allowClear
              options={flatDepartments.map((department) => ({
                label: department.departmentName,
                value: department.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <Input type="number" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            保存
          </Button>
        </Form>
      </Modal>
    </section>
  );
}

function flattenDepartments(departments: Department[]): Department[] {
  return departments.flatMap((department) => [
    department,
    ...flattenDepartments(department.children || []),
  ]);
}
