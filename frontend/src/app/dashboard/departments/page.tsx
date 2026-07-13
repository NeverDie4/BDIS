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
import type { Department, Organization, PageResult } from "@/types/api";
import { App, Button, Form, Input, Popconfirm, Select, Space, Table } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Building2, Network, Pencil, Plus, Workflow } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

type DepartmentForm = {
  departmentNo?: string;
  departmentName?: string;
  organizationId?: number;
  parentId?: number;
  sortOrder?: number;
  status?: number;
};

export default function DepartmentsPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Department | null>(null);
  const [form] = Form.useForm<DepartmentForm>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [departmentTree, organizationPage] = await Promise.all([
        apiGet<Department[]>("/departments"),
        apiGet<PageResult<Organization>>("/organizations", { page: 1, size: 100 }),
      ]);
      setDepartments(departmentTree);
      setOrganizations(organizationPage.records);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, "部门数据加载失败"));
      }
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  const flatDepartments = useMemo(() => flattenDepartments(departments), [departments]);
  const organizationNames = useMemo(
    () =>
      new Map(
        organizations.map((organization) => [organization.id, organization.organizationName]),
      ),
    [organizations],
  );

  function openCreate() {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ sortOrder: 0, status: 1 });
    setOpen(true);
  }

  function openEdit(department: Department) {
    setEditing(department);
    form.setFieldsValue(department);
    setOpen(true);
  }

  async function remove(department: Department) {
    try {
      await apiDelete<void>(`/departments/${department.id}`);
      message.success("部门已删除");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "部门删除失败"));
    }
  }

  const columns: ColumnsType<Department> = [
    { title: "部门编号", dataIndex: "departmentNo", width: 160 },
    { title: "部门名称", dataIndex: "departmentName", width: 220 },
    {
      title: "所属机构",
      dataIndex: "organizationId",
      width: 220,
      render: (value: number) => organizationNames.get(value) || `机构 #${value}`,
    },
    { title: "排序", dataIndex: "sortOrder", width: 90 },
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
          {hasPermission("auth:department:update") ? (
            <Button size="small" icon={<Pencil size={14} />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          ) : null}
          {hasPermission("auth:department:delete") ? (
            <Popconfirm title="确认删除该部门？" onConfirm={() => remove(record)}>
              <Button danger size="small">
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  async function submit(values: DepartmentForm) {
    if (editing) {
      await apiPut<void>(`/departments/${editing.id}`, values);
      message.success("部门已更新");
    } else {
      await apiPost<number>("/departments", values);
      message.success("部门已创建");
    }
    setOpen(false);
    form.resetFields();
    await load();
  }

  return (
    <DashboardPage
      eyebrow="DEPARTMENT HIERARCHY"
      title="部门管理"
      description="按机构维护部门层级、排序和状态，为用户归属与数据范围判定提供组织依据。"
      actions={
        hasPermission("auth:department:create") ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
            新增部门
          </Button>
        ) : undefined
      }
      metrics={
        <>
          <DashboardMetric icon={Network} label="部门总数" value={flatDepartments.length} />
          <DashboardMetric icon={Building2} label="关联机构" value={organizations.length} />
          <DashboardMetric
            icon={Workflow}
            label="子级部门"
            value={flatDepartments.filter((department) => Boolean(department.parentId)).length}
          />
        </>
      }
    >
      <DashboardPanel flush>
        <Table<Department>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={departments}
          pagination={false}
          scroll={{ x: 950 }}
        />
      </DashboardPanel>

      <DashboardFormModal<DepartmentForm>
        title={editing ? `编辑部门：${editing.departmentName}` : "新增部门"}
        open={open}
        form={form}
        onCancel={() => setOpen(false)}
        onFinish={submit}
        errorMessage={editing ? "部门更新失败" : "部门创建失败"}
      >
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
            options={flatDepartments
              .filter((department) => department.id !== editing?.id)
              .map((department) => ({
                label: department.departmentName,
                value: department.id,
              }))}
          />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序">
          <Input type="number" />
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

function flattenDepartments(departments: Department[]): Department[] {
  return departments.flatMap((department) => [
    department,
    ...flattenDepartments(department.children || []),
  ]);
}
