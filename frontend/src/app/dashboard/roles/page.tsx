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
import type { PageResult, Permission, Role } from "@/types/api";
import { Alert, App, Button, Form, Input, Popconfirm, Select, Space, Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import { KeyRound, Pencil, Plus, ShieldCheck, Users } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

type RoleForm = {
  roleCode?: string;
  roleName?: string;
  dataScope?: string;
  description?: string;
  sortOrder?: number;
  status?: number;
};

const dataScopeOptions = [
  { label: "全部数据", value: "all" },
  { label: "所属机构", value: "organization" },
  { label: "所属部门", value: "department" },
  { label: "仅本人", value: "self" },
  { label: "自定义", value: "custom" },
];

const dataScopeLabels = Object.fromEntries(
  dataScopeOptions.map((option) => [option.value, option.label]),
);

export default function RolesPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<Role | null>(null);
  const [assignRole, setAssignRole] = useState<Role | null>(null);
  const [assignLoading, setAssignLoading] = useState(false);
  const [form] = Form.useForm<RoleForm>();
  const [assignForm] = Form.useForm<{ permissionIds: number[] }>();

  const canUpdate = hasPermission("auth:role:update");
  const canAssign = hasPermission("auth:role:assign-permission");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [rolePage, permissionPage] = await Promise.all([
        apiGet<PageResult<Role>>("/roles", { page: 1, size: 100 }),
        hasPermission("auth:permission:view")
          ? apiGet<PageResult<Permission>>("/permissions", { page: 1, size: 200 })
          : Promise.resolve({ records: [], page: 1, size: 0, total: 0 } as PageResult<Permission>),
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
  }, [hasPermission, message]);

  useEffect(() => {
    void load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ dataScope: "self", sortOrder: 0, status: 1 });
    setEditorOpen(true);
  }

  function openEdit(role: Role) {
    setEditing(role);
    form.setFieldsValue({
      roleCode: role.roleCode,
      roleName: role.roleName,
      dataScope: role.dataScope,
      description: role.description,
      sortOrder: role.sortOrder,
      status: role.status,
    });
    setEditorOpen(true);
  }

  async function openAssign(role: Role) {
    setAssignRole(role);
    setAssignLoading(true);
    assignForm.resetFields();
    try {
      const permissionIds = await apiGet<number[]>(`/roles/${role.id}/permission-ids`);
      assignForm.setFieldsValue({ permissionIds });
    } catch (error) {
      message.error(getApiErrorMessage(error, "角色现有权限加载失败"));
      setAssignRole(null);
    } finally {
      setAssignLoading(false);
    }
  }

  async function remove(role: Role) {
    try {
      await apiDelete<void>(`/roles/${role.id}`);
      message.success("角色已删除");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "角色删除失败"));
    }
  }

  const columns: ColumnsType<Role> = [
    { title: "编码", dataIndex: "roleCode", width: 180 },
    { title: "名称", dataIndex: "roleName", width: 160 },
    {
      title: "类型",
      dataIndex: "roleType",
      width: 110,
      render: (value?: string) => (
        <Tag color={value === "system" ? "red" : "green"}>
          {value === "system" ? "系统角色" : "业务角色"}
        </Tag>
      ),
    },
    {
      title: "数据范围",
      dataIndex: "dataScope",
      width: 130,
      render: (value?: string) => dataScopeLabels[value || "self"] || value,
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 90,
      render: (value?: number) => <DashboardStatus enabled={value === 1} />,
    },
    { title: "说明", dataIndex: "description", render: (value?: string) => value || "-" },
    {
      title: "操作",
      key: "action",
      width: 250,
      fixed: "right",
      render: (_, record) => (
        <Space>
          {canUpdate ? (
            <Button size="small" icon={<Pencil size={14} />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canAssign ? (
            <Button
              size="small"
              icon={<KeyRound size={14} />}
              onClick={() => void openAssign(record)}
            >
              分配权限
            </Button>
          ) : null}
          {hasPermission("auth:role:delete") && record.roleType !== "system" ? (
            <Popconfirm title="确认删除该角色？" onConfirm={() => remove(record)}>
              <Button danger size="small">
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  async function submit(values: RoleForm) {
    if (editing) {
      const payload = { ...values };
      delete payload.roleCode;
      await apiPut<void>(`/roles/${editing.id}`, payload);
      message.success("角色已更新");
    } else {
      await apiPost<number>("/roles", { ...values, roleType: "business" });
      message.success("角色已创建");
    }
    setEditorOpen(false);
    form.resetFields();
    await load();
  }

  async function assign(values: { permissionIds: number[] }) {
    if (!assignRole) return;
    await apiPut<void>(`/roles/${assignRole.id}/permissions`, values);
    message.success("角色权限已更新");
    setAssignRole(null);
    assignForm.resetFields();
  }

  const businessRoles = roles.filter((role) => role.roleType !== "system");

  return (
    <DashboardPage
      eyebrow="ROLE GOVERNANCE"
      title="角色管理"
      description="以业务职责组织权限和数据范围。系统角色保持受保护状态，业务角色可按需调整。"
      actions={
        hasPermission("auth:role:create") ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
            新增角色
          </Button>
        ) : undefined
      }
      metrics={
        <>
          <DashboardMetric icon={Users} label="角色总数" value={roles.length} />
          <DashboardMetric
            icon={ShieldCheck}
            label="系统角色"
            value={roles.length - businessRoles.length}
          />
          <DashboardMetric icon={KeyRound} label="业务角色" value={businessRoles.length} />
        </>
      }
    >
      <Alert
        showIcon
        type="info"
        message="系统角色保护"
        description="系统角色由初始化流程维护，不能停用、改变类型或数据范围，也不能删除。"
        style={{ marginBottom: 16 }}
      />
      <DashboardPanel flush>
        <Table<Role>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={roles}
          scroll={{ x: 1100 }}
        />
      </DashboardPanel>

      <DashboardFormModal<RoleForm>
        title={editing ? `编辑角色：${editing.roleName}` : "新增角色"}
        open={editorOpen}
        form={form}
        onCancel={() => setEditorOpen(false)}
        onFinish={submit}
        errorMessage={editing ? "角色更新失败" : "角色创建失败"}
      >
        <Form.Item
          name="roleCode"
          label="角色编码"
          rules={[{ required: true, message: "请输入角色编码" }]}
        >
          <Input disabled={Boolean(editing)} />
        </Form.Item>
        <Form.Item
          name="roleName"
          label="角色名称"
          rules={[{ required: true, message: "请输入角色名称" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="dataScope" label="数据范围">
          <Select options={dataScopeOptions} disabled={editing?.roleType === "system"} />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序">
          <Input type="number" />
        </Form.Item>
        {editing ? (
          <Form.Item name="status" label="状态">
            <Select
              disabled={editing.roleType === "system"}
              options={[
                { label: "启用", value: 1 },
                { label: "停用", value: 0 },
              ]}
            />
          </Form.Item>
        ) : null}
        <Form.Item name="description" label="说明">
          <Input.TextArea rows={3} />
        </Form.Item>
      </DashboardFormModal>

      <DashboardFormModal<{ permissionIds: number[] }>
        title={`分配权限${assignRole ? `：${assignRole.roleName}` : ""}`}
        open={Boolean(assignRole)}
        form={assignForm}
        onCancel={() => setAssignRole(null)}
        onFinish={assign}
        errorMessage="角色权限更新失败"
      >
        <Form.Item name="permissionIds" label="权限点">
          <Select
            mode="multiple"
            loading={assignLoading}
            optionFilterProp="label"
            options={permissions.map((permission) => ({
              label: `${permission.permissionName} (${permission.permissionCode})`,
              value: permission.id,
            }))}
          />
        </Form.Item>
      </DashboardFormModal>
    </DashboardPage>
  );
}
