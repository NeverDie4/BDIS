"use client";

import { apiDelete, apiGet, apiPost, apiPut, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { PageResult, Permission, Role } from "@/types/api";
import { App, Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Plus } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

type RoleForm = {
  roleCode: string;
  roleName: string;
  roleType?: string;
  dataScope?: string;
  description?: string;
};

const dataScopeOptions = [
  { label: "全部", value: "all" },
  { label: "机构", value: "organization" },
  { label: "部门", value: "department" },
  { label: "本人", value: "self" },
  { label: "自定义", value: "custom" },
];

export default function RolesPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [assignRole, setAssignRole] = useState<Role | null>(null);
  const [form] = Form.useForm<RoleForm>();
  const [assignForm] = Form.useForm<{ permissionIds: number[] }>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [rolePage, permissionPage] = await Promise.all([
        apiGet<PageResult<Role>>("/roles", { page: 1, size: 100 }),
        apiGet<PageResult<Permission>>("/permissions", { page: 1, size: 200 }),
      ]);
      setRoles(rolePage.records);
      setPermissions(permissionPage.records);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, "角色数据加载失败"));
      }
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    const task = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(task);
  }, [load]);

  const columns = useMemo<ColumnsType<Role>>(
    () => [
      { title: "编码", dataIndex: "roleCode" },
      { title: "名称", dataIndex: "roleName" },
      { title: "类型", dataIndex: "roleType" },
      {
        title: "数据范围",
        dataIndex: "dataScope",
        render: (value?: string) => <Tag>{value || "self"}</Tag>,
      },
      {
        title: "操作",
        key: "action",
        width: 220,
        render: (_, record) => (
          <Space>
            {hasPermission("auth:role:assign-permission") ? (
              <Button size="small" onClick={() => setAssignRole(record)}>
                分配权限
              </Button>
            ) : null}
            {hasPermission("auth:role:delete") ? (
              <Popconfirm
                title="确认删除该角色？"
                onConfirm={async () => {
                  await apiDelete<void>(`/roles/${record.id}`);
                  message.success("已删除");
                  await load();
                }}
              >
                <Button danger size="small">
                  删除
                </Button>
              </Popconfirm>
            ) : null}
          </Space>
        ),
      },
    ],
    [hasPermission, load, message],
  );

  async function submit(values: RoleForm) {
    await apiPost<number>("/roles", values);
    message.success("角色已创建");
    setCreateOpen(false);
    form.resetFields();
    await load();
  }

  async function assign(values: { permissionIds: number[] }) {
    if (!assignRole) {
      return;
    }
    await apiPut<void>(`/roles/${assignRole.id}/permissions`, values);
    message.success("权限已分配");
    setAssignRole(null);
    assignForm.resetFields();
  }

  return (
    <section className="page-section">
      <div className="page-title-row">
        <Typography.Title level={3}>角色管理</Typography.Title>
        {hasPermission("auth:role:create") ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={() => setCreateOpen(true)}>
            新增角色
          </Button>
        ) : null}
      </div>
      <Table<Role> rowKey="id" loading={loading} columns={columns} dataSource={roles} />
      <Modal title="新增角色" open={createOpen} onCancel={() => setCreateOpen(false)} footer={null} destroyOnHidden>
        <Form<RoleForm> form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="roleCode" label="角色编码" rules={[{ required: true, message: "请输入角色编码" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: "请输入角色名称" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roleType" label="角色类型">
            <Input />
          </Form.Item>
          <Form.Item name="dataScope" label="默认数据范围" initialValue="self">
            <Select options={dataScopeOptions} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            保存
          </Button>
        </Form>
      </Modal>
      <Modal
        title={`分配权限${assignRole ? `：${assignRole.roleName}` : ""}`}
        open={Boolean(assignRole)}
        onCancel={() => setAssignRole(null)}
        footer={null}
        destroyOnHidden
      >
        <Form form={assignForm} layout="vertical" onFinish={assign}>
          <Form.Item name="permissionIds" label="权限点" rules={[{ required: true, message: "请选择权限" }]}>
            <Select
              mode="multiple"
              options={permissions.map((permission) => ({
                label: `${permission.permissionName} (${permission.permissionCode})`,
                value: permission.id,
              }))}
            />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            保存
          </Button>
        </Form>
      </Modal>
    </section>
  );
}
