"use client";

import { apiDelete, apiGet, apiPost, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { PageResult, Role, User } from "@/types/api";
import { App, Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Plus } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

type UserForm = {
  username: string;
  password: string;
  realName: string;
  phoneNumber?: string;
  email?: string;
  roleIds?: number[];
};

export default function UsersPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<UserForm>();

  const canCreate = hasPermission("auth:user:create");
  const canDelete = hasPermission("auth:user:delete");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [userPage, rolePage] = await Promise.all([
        apiGet<PageResult<User>>("/users", { page: 1, size: 100 }),
        apiGet<PageResult<Role>>("/roles", { page: 1, size: 100 }),
      ]);
      setUsers(userPage.records);
      setRoles(rolePage.records);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error("用户数据加载失败");
      }
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    const task = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(task);
  }, [load]);

  const columns = useMemo<ColumnsType<User>>(
    () => [
      { title: "账号", dataIndex: "username" },
      { title: "姓名", dataIndex: "realName" },
      {
        title: "角色",
        dataIndex: "roles",
        render: (value?: Role[]) => (
          <Space wrap>
            {(value || []).map((role) => (
              <Tag key={role.id}>{role.roleName}</Tag>
            ))}
          </Space>
        ),
      },
      {
        title: "状态",
        dataIndex: "status",
        render: (value?: number) => <Tag color={value === 1 ? "green" : "default"}>{value === 1 ? "启用" : "停用"}</Tag>,
      },
      {
        title: "操作",
        key: "action",
        width: 120,
        render: (_, record) =>
          canDelete ? (
            <Popconfirm
              title="确认删除该用户？"
              onConfirm={async () => {
                await apiDelete<void>(`/users/${record.id}`);
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
    [canDelete, load, message],
  );

  async function submit(values: UserForm) {
    await apiPost<number>("/users", values);
    message.success("用户已创建");
    setOpen(false);
    form.resetFields();
    await load();
  }

  return (
    <section className="page-section">
      <div className="page-title-row">
        <Typography.Title level={3}>用户管理</Typography.Title>
        {canCreate ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={() => setOpen(true)}>
            新增用户
          </Button>
        ) : null}
      </div>
      <Table<User> rowKey="id" loading={loading} columns={columns} dataSource={users} />
      <Modal title="新增用户" open={open} onCancel={() => setOpen(false)} footer={null} destroyOnHidden>
        <Form<UserForm> form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="username" label="账号" rules={[{ required: true, message: "请输入账号" }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[
              { required: true, message: "请输入密码" },
              { min: 8, message: "密码至少 8 位" },
            ]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item name="realName" label="姓名" rules={[{ required: true, message: "请输入姓名" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roleIds" label="角色" rules={[{ required: true, message: "请选择角色" }]}>
            <Select
              mode="multiple"
              options={roles.map((role) => ({ label: role.roleName, value: role.id }))}
            />
          </Form.Item>
          <Form.Item name="phoneNumber" label="手机号">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
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
