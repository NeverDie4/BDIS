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
import type { Department, Organization, PageResult, Role, User } from "@/types/api";
import { App, Button, Form, Input, Popconfirm, Select, Space, Switch, Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Pencil, Plus, ShieldCheck, UserCheck, Users } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

type UserForm = {
  username?: string;
  password?: string;
  realName?: string;
  phoneNumber?: string;
  email?: string;
  organizationId?: number;
  departmentId?: number;
  status?: number;
  roleIds?: number[];
  mustChangePassword?: boolean;
};

type PasswordResetForm = {
  newPassword: string;
  confirmPassword: string;
  mustChangePassword: boolean;
};

export default function UsersPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [passwordUser, setPasswordUser] = useState<User | null>(null);
  const [editing, setEditing] = useState<User | null>(null);
  const [form] = Form.useForm<UserForm>();
  const [passwordForm] = Form.useForm<PasswordResetForm>();

  const canUpdate = hasPermission("auth:user:update");
  const canAssignRole = hasPermission("auth:user:assign-role");
  const canCreate = hasPermission("auth:user:create") && canAssignRole;
  const canDelete = hasPermission("auth:user:delete");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [userPage, rolePage, organizationPage, departmentTree] = await Promise.all([
        apiGet<PageResult<User>>("/users", { page: 1, size: 100 }),
        hasPermission("auth:role:view")
          ? apiGet<PageResult<Role>>("/roles", { page: 1, size: 100 })
          : Promise.resolve({ records: [], page: 1, size: 0, total: 0 } as PageResult<Role>),
        hasPermission("auth:organization:view")
          ? apiGet<PageResult<Organization>>("/organizations", { page: 1, size: 100 })
          : Promise.resolve({
              records: [],
              page: 1,
              size: 0,
              total: 0,
            } as PageResult<Organization>),
        hasPermission("auth:department:view")
          ? apiGet<Department[]>("/departments")
          : Promise.resolve([]),
      ]);
      setUsers(userPage.records);
      setRoles(rolePage.records);
      setOrganizations(organizationPage.records);
      setDepartments(flattenDepartments(departmentTree));
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, "用户数据加载失败"));
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
    form.setFieldsValue({ status: 1, mustChangePassword: true });
    setOpen(true);
  }

  function openEdit(user: User) {
    setEditing(user);
    form.setFieldsValue({
      realName: user.realName,
      phoneNumber: user.phoneNumber,
      email: user.email,
      organizationId: user.organizationId,
      departmentId: user.departmentId,
      status: user.status,
      roleIds: user.roles?.map((role) => role.id),
    });
    setOpen(true);
  }

  async function remove(user: User) {
    try {
      await apiDelete<void>(`/users/${user.id}`);
      message.success("用户已删除");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "用户删除失败"));
    }
  }

  const columns: ColumnsType<User> = [
    { title: "账号", dataIndex: "username", width: 150 },
    { title: "姓名", dataIndex: "realName", width: 140 },
    {
      title: "角色",
      dataIndex: "roles",
      render: (value?: Role[]) => (
        <Space wrap size={[4, 4]}>
          {(value || []).length
            ? (value || []).map((role) => <Tag key={role.id}>{role.roleName}</Tag>)
            : "-"}
        </Space>
      ),
    },
    { title: "手机号", dataIndex: "phoneNumber", width: 140, render: valueOrDash },
    { title: "邮箱", dataIndex: "email", width: 210, render: valueOrDash },
    {
      title: "状态",
      dataIndex: "status",
      width: 90,
      render: (value?: number) => <DashboardStatus enabled={value === 1} />,
    },
    {
      title: "首次改密",
      dataIndex: "mustChangePassword",
      width: 100,
      render: (value?: boolean) =>
        value ? <Tag color="orange">待修改</Tag> : <Tag>已完成</Tag>,
    },
    {
      title: "操作",
      key: "action",
      width: 176,
      fixed: "right",
      render: (_, record) => (
        <Space>
          {canUpdate || canAssignRole ? (
            <Button size="small" icon={<Pencil size={14} />} onClick={() => openEdit(record)}>
              {canUpdate ? "编辑" : "分配角色"}
            </Button>
          ) : null}
          {canUpdate ? (
            <Button
              size="small"
              onClick={() => {
                setPasswordUser(record);
                passwordForm.resetFields();
                passwordForm.setFieldsValue({ mustChangePassword: true });
              }}
            >
              重置密码
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该用户？" onConfirm={() => remove(record)}>
              <Button danger size="small">
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  async function submit(values: UserForm) {
    if (editing) {
      const payload: UserForm = {};
      if (canUpdate) {
        Object.assign(payload, {
          realName: values.realName,
          phoneNumber: values.phoneNumber,
          email: values.email,
          organizationId: values.organizationId,
          departmentId: values.departmentId,
          status: values.status,
        });
      }
      if (canAssignRole) {
        payload.roleIds = values.roleIds || [];
      }
      await apiPut<void>(`/users/${editing.id}`, payload);
      message.success("用户信息已更新");
    } else {
      await apiPost<number>("/users", values);
      message.success("用户已创建");
    }
    setOpen(false);
    form.resetFields();
    await load();
  }

  return (
    <DashboardPage
      eyebrow="IDENTITY & ACCESS"
      title="用户管理"
      description="维护后台账号、人员归属、启停状态与角色关系。角色变更受独立权限控制。"
      actions={
        canCreate ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
            新增用户
          </Button>
        ) : undefined
      }
      metrics={
        <>
          <DashboardMetric icon={Users} label="用户总数" value={users.length} />
          <DashboardMetric
            icon={UserCheck}
            label="启用账号"
            value={users.filter((user) => user.status === 1).length}
          />
          <DashboardMetric icon={ShieldCheck} label="可分配角色" value={roles.length} />
        </>
      }
    >
      <DashboardPanel flush>
        <Table<User>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={users}
          scroll={{ x: 1080 }}
        />
      </DashboardPanel>

      <DashboardFormModal<UserForm>
        title={editing ? `编辑用户：${editing.username}` : "新增用户"}
        open={open}
        form={form}
        onCancel={() => setOpen(false)}
        onFinish={submit}
        errorMessage={editing ? "用户更新失败" : "用户创建失败"}
      >
        {!editing ? (
          <>
            <Form.Item
              name="username"
              label="账号"
              rules={[{ required: true, message: "请输入账号" }]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="password"
              label="初始密码"
              rules={[
                { required: true, message: "请输入密码" },
                { min: 8, message: "密码至少 8 位" },
              ]}
            >
              <Input.Password />
            </Form.Item>
            <Form.Item
              name="mustChangePassword"
              label="首次登录修改密码"
              valuePropName="checked"
            >
              <Switch />
            </Form.Item>
          </>
        ) : null}
        {canUpdate || !editing ? (
          <>
            <Form.Item
              name="realName"
              label="姓名"
              rules={[{ required: true, message: "请输入姓名" }]}
            >
              <Input />
            </Form.Item>
            <Form.Item name="organizationId" label="所属机构">
              <Select
                allowClear
                options={organizations.map((organization) => ({
                  label: organization.organizationName,
                  value: organization.id,
                }))}
              />
            </Form.Item>
            <Form.Item name="departmentId" label="所属部门">
              <Select
                allowClear
                options={departments.map((department) => ({
                  label: department.departmentName,
                  value: department.id,
                }))}
              />
            </Form.Item>
            <Form.Item name="phoneNumber" label="手机号">
              <Input />
            </Form.Item>
            <Form.Item
              name="email"
              label="邮箱"
              rules={[{ type: "email", message: "邮箱格式不正确" }]}
            >
              <Input />
            </Form.Item>
            {editing ? (
              <Form.Item name="status" label="账号状态">
                <Select
                  options={[
                    { label: "启用", value: 1 },
                    { label: "停用", value: 0 },
                  ]}
                />
              </Form.Item>
            ) : null}
          </>
        ) : null}
        {canAssignRole || !editing ? (
          <Form.Item
            name="roleIds"
            label="角色"
            rules={editing ? undefined : [{ required: true, message: "请选择角色" }]}
          >
            <Select
              mode="multiple"
              options={roles.map((role) => ({ label: role.roleName, value: role.id }))}
            />
          </Form.Item>
        ) : null}
      </DashboardFormModal>
      <DashboardFormModal<PasswordResetForm>
        title={`重置密码${passwordUser ? `：${passwordUser.username}` : ""}`}
        open={Boolean(passwordUser)}
        form={passwordForm}
        submitText="确认重置"
        errorMessage="密码重置失败"
        onCancel={() => setPasswordUser(null)}
        onFinish={async (values) => {
          if (!passwordUser) return;
          await apiPut<void>(`/users/${passwordUser.id}/password`, {
            newPassword: values.newPassword,
            mustChangePassword: values.mustChangePassword,
          });
          message.success("密码已重置，原有会话已撤销");
          setPasswordUser(null);
          passwordForm.resetFields();
          await load();
        }}
      >
        <Form.Item
          label="新密码"
          name="newPassword"
          rules={[{ required: true }, { min: 8, max: 72 }]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          dependencies={["newPassword"]}
          label="确认新密码"
          name="confirmPassword"
          rules={[
            { required: true },
            ({ getFieldValue }) => ({
              validator(_, value) {
                return !value || getFieldValue("newPassword") === value
                  ? Promise.resolve()
                  : Promise.reject(new Error("两次输入的密码不一致"));
              },
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          label="下次登录强制修改密码"
          name="mustChangePassword"
          valuePropName="checked"
        >
          <Switch />
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

function valueOrDash(value?: string) {
  return value || "-";
}
