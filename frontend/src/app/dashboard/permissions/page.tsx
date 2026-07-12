"use client";

import { apiDelete, apiGet, apiPost, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { MenuItem, PageResult, Permission } from "@/types/api";
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { Plus } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

type MenuForm = {
  parentId?: number;
  menuCode: string;
  menuName: string;
  routePath?: string;
  icon?: string;
  sortOrder?: number;
};

type PermissionForm = {
  permissionCode: string;
  permissionName: string;
  permissionType: string;
  menuId?: number;
  apiPath?: string;
  requestMethod?: string;
  description?: string;
};

const permissionTypeOptions = [
  { label: "菜单", value: "menu" },
  { label: "按钮", value: "button" },
  { label: "接口", value: "api" },
];

export default function PermissionsPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [menus, setMenus] = useState<MenuItem[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [permissionOpen, setPermissionOpen] = useState(false);
  const [menuForm] = Form.useForm<MenuForm>();
  const [permissionForm] = Form.useForm<PermissionForm>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [menuTree, permissionPage] = await Promise.all([
        apiGet<MenuItem[]>("/menus"),
        apiGet<PageResult<Permission>>("/permissions", { page: 1, size: 200 }),
      ]);
      setMenus(menuTree);
      setPermissions(permissionPage.records);
    } catch (error) {
      if (!isAuthRedirectError(error)) {
        message.error(getApiErrorMessage(error, "菜单权限数据加载失败"));
      }
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    const task = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(task);
  }, [load]);

  const flatMenus = useMemo(() => flattenMenus(menus), [menus]);

  const menuColumns = useMemo<ColumnsType<MenuItem>>(
    () => [
      { title: "菜单名称", dataIndex: "menuName" },
      { title: "编码", dataIndex: "menuCode" },
      { title: "路由", dataIndex: "routePath" },
      { title: "图标", dataIndex: "icon" },
      {
        title: "操作",
        key: "action",
        width: 120,
        render: (_, record) =>
          hasPermission("auth:menu:delete") ? (
            <Popconfirm
              title="确认删除该菜单？"
              onConfirm={async () => {
                await apiDelete<void>(`/menus/${record.id}`);
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
    [hasPermission, load, message],
  );

  const permissionColumns = useMemo<ColumnsType<Permission>>(
    () => [
      { title: "权限名称", dataIndex: "permissionName" },
      { title: "编码", dataIndex: "permissionCode" },
      {
        title: "类型",
        dataIndex: "permissionType",
        render: (value: string) => <Tag>{value}</Tag>,
      },
      { title: "接口路径", dataIndex: "apiPath" },
      {
        title: "操作",
        key: "action",
        width: 120,
        render: (_, record) =>
          hasPermission("auth:permission:delete") ? (
            <Popconfirm
              title="确认删除该权限？"
              onConfirm={async () => {
                await apiDelete<void>(`/permissions/${record.id}`);
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
    [hasPermission, load, message],
  );

  async function submitMenu(values: MenuForm) {
    await apiPost<number>("/menus", values);
    message.success("菜单已创建");
    setMenuOpen(false);
    menuForm.resetFields();
    await load();
  }

  async function submitPermission(values: PermissionForm) {
    await apiPost<number>("/permissions", values);
    message.success("权限已创建");
    setPermissionOpen(false);
    permissionForm.resetFields();
    await load();
  }

  return (
    <section className="page-section">
      <Typography.Title level={3}>菜单权限</Typography.Title>
      <Tabs
        items={[
          {
            key: "menus",
            label: "菜单",
            children: (
              <Space direction="vertical" className="wide-space">
                {hasPermission("auth:menu:create") ? (
                  <Button type="primary" icon={<Plus size={16} />} onClick={() => setMenuOpen(true)}>
                    新增菜单
                  </Button>
                ) : null}
                <Table<MenuItem>
                  rowKey="id"
                  loading={loading}
                  columns={menuColumns}
                  dataSource={menus}
                  pagination={false}
                />
              </Space>
            ),
          },
          {
            key: "permissions",
            label: "权限点",
            children: (
              <Space direction="vertical" className="wide-space">
                {hasPermission("auth:permission:create") ? (
                  <Button
                    type="primary"
                    icon={<Plus size={16} />}
                    onClick={() => setPermissionOpen(true)}
                  >
                    新增权限
                  </Button>
                ) : null}
                <Table<Permission>
                  rowKey="id"
                  loading={loading}
                  columns={permissionColumns}
                  dataSource={permissions}
                />
              </Space>
            ),
          },
        ]}
      />
      <Modal title="新增菜单" open={menuOpen} onCancel={() => setMenuOpen(false)} footer={null} destroyOnHidden>
        <Form<MenuForm> form={menuForm} layout="vertical" onFinish={submitMenu}>
          <Form.Item name="parentId" label="上级菜单">
            <Select
              allowClear
              options={flatMenus.map((menu) => ({ label: menu.menuName, value: menu.id }))}
            />
          </Form.Item>
          <Form.Item name="menuCode" label="菜单编码" rules={[{ required: true, message: "请输入菜单编码" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="menuName" label="菜单名称" rules={[{ required: true, message: "请输入菜单名称" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="routePath" label="路由">
            <Input />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <Input type="number" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            保存
          </Button>
        </Form>
      </Modal>
      <Modal
        title="新增权限"
        open={permissionOpen}
        onCancel={() => setPermissionOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Form<PermissionForm> form={permissionForm} layout="vertical" onFinish={submitPermission}>
          <Form.Item
            name="permissionCode"
            label="权限编码"
            rules={[{ required: true, message: "请输入权限编码" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="permissionName"
            label="权限名称"
            rules={[{ required: true, message: "请输入权限名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="permissionType"
            label="权限类型"
            initialValue="button"
            rules={[{ required: true, message: "请选择权限类型" }]}
          >
            <Select options={permissionTypeOptions} />
          </Form.Item>
          <Form.Item name="menuId" label="所属菜单">
            <Select
              allowClear
              options={flatMenus.map((menu) => ({ label: menu.menuName, value: menu.id }))}
            />
          </Form.Item>
          <Form.Item name="apiPath" label="接口路径">
            <Input />
          </Form.Item>
          <Form.Item name="requestMethod" label="请求方法">
            <Input />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            保存
          </Button>
        </Form>
      </Modal>
    </section>
  );
}

function flattenMenus(menus: MenuItem[]): MenuItem[] {
  return menus.flatMap((menu) => [menu, ...flattenMenus(menu.children || [])]);
}
