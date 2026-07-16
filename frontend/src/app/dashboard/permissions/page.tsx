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
import type { MenuItem, PageResult, Permission } from "@/types/api";
import { App, Button, Form, Input, Popconfirm, Select, Space, Table, Tabs, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import { KeyRound, Menu as MenuIcon, Pencil, Plus, ShieldCheck } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

type MenuForm = {
  parentId?: number;
  menuCode?: string;
  menuName?: string;
  routePath?: string;
  componentPath?: string;
  icon?: string;
  visible?: number;
  sortOrder?: number;
  status?: number;
};

type PermissionForm = {
  permissionCode?: string;
  permissionName?: string;
  permissionType?: string;
  menuId?: number;
  apiPath?: string;
  requestMethod?: string;
  description?: string;
  status?: number;
};

const permissionTypeOptions = [
  { label: "菜单", value: "menu" },
  { label: "按钮", value: "button" },
  { label: "接口", value: "api" },
];

const requestMethodOptions = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"].map((value) => ({
  label: value,
  value,
}));

export default function PermissionsPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [menus, setMenus] = useState<MenuItem[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [permissionOpen, setPermissionOpen] = useState(false);
  const [editingMenu, setEditingMenu] = useState<MenuItem | null>(null);
  const [editingPermission, setEditingPermission] = useState<Permission | null>(null);
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
    void load();
  }, [load]);

  const flatMenus = useMemo(() => flattenMenus(menus), [menus]);

  function openMenuCreate() {
    setEditingMenu(null);
    menuForm.resetFields();
    menuForm.setFieldsValue({ visible: 1, status: 1, sortOrder: 0 });
    setMenuOpen(true);
  }

  function openMenuEdit(menu: MenuItem) {
    setEditingMenu(menu);
    menuForm.setFieldsValue({
      parentId: menu.parentId,
      menuCode: menu.menuCode,
      menuName: menu.menuName,
      routePath: menu.routePath,
      componentPath: menu.componentPath,
      icon: menu.icon,
      visible: menu.visible,
      sortOrder: menu.sortOrder,
      status: menu.status,
    });
    setMenuOpen(true);
  }

  function openPermissionCreate() {
    setEditingPermission(null);
    permissionForm.resetFields();
    permissionForm.setFieldsValue({ permissionType: "button", status: 1 });
    setPermissionOpen(true);
  }

  function openPermissionEdit(permission: Permission) {
    setEditingPermission(permission);
    permissionForm.setFieldsValue(permission);
    setPermissionOpen(true);
  }

  async function removeMenu(menu: MenuItem) {
    try {
      await apiDelete<void>(`/menus/${menu.id}`);
      message.success("菜单已删除");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "菜单删除失败"));
    }
  }

  async function removePermission(permission: Permission) {
    try {
      await apiDelete<void>(`/permissions/${permission.id}`);
      message.success("权限已删除");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "权限删除失败"));
    }
  }

  const menuColumns: ColumnsType<MenuItem> = [
    { title: "菜单名称", dataIndex: "menuName", width: 180 },
    { title: "编码", dataIndex: "menuCode", width: 190 },
    { title: "路由", dataIndex: "routePath", width: 220, render: valueOrDash },
    { title: "图标", dataIndex: "icon", width: 110, render: valueOrDash },
    {
      title: "可见",
      dataIndex: "visible",
      width: 80,
      render: (value?: number) => (value === 0 ? "隐藏" : "显示"),
    },
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
          {hasPermission("auth:menu:update") ? (
            <Button size="small" icon={<Pencil size={14} />} onClick={() => openMenuEdit(record)}>
              编辑
            </Button>
          ) : null}
          {hasPermission("auth:menu:delete") ? (
            <Popconfirm title="确认删除该菜单？" onConfirm={() => removeMenu(record)}>
              <Button danger size="small">
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  const permissionColumns: ColumnsType<Permission> = [
    { title: "权限名称", dataIndex: "permissionName", width: 180 },
    { title: "编码", dataIndex: "permissionCode", width: 260 },
    {
      title: "类型",
      dataIndex: "permissionType",
      width: 90,
      render: (value: string) => <Tag>{value}</Tag>,
    },
    { title: "接口路径", dataIndex: "apiPath", width: 220, render: valueOrDash },
    { title: "方法", dataIndex: "requestMethod", width: 90, render: valueOrDash },
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
          {hasPermission("auth:permission:update") ? (
            <Button
              size="small"
              icon={<Pencil size={14} />}
              onClick={() => openPermissionEdit(record)}
            >
              编辑
            </Button>
          ) : null}
          {hasPermission("auth:permission:delete") ? (
            <Popconfirm title="确认删除该权限？" onConfirm={() => removePermission(record)}>
              <Button danger size="small">
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  async function submitMenu(values: MenuForm) {
    if (editingMenu) {
      await apiPut<void>(`/menus/${editingMenu.id}`, values);
      message.success("菜单已更新");
    } else {
      await apiPost<number>("/menus", values);
      message.success("菜单已创建");
    }
    setMenuOpen(false);
    menuForm.resetFields();
    await load();
  }

  async function submitPermission(values: PermissionForm) {
    if (editingPermission) {
      await apiPut<void>(`/permissions/${editingPermission.id}`, values);
      message.success("权限已更新");
    } else {
      await apiPost<number>("/permissions", values);
      message.success("权限已创建");
    }
    setPermissionOpen(false);
    permissionForm.resetFields();
    await load();
  }

  return (
    <DashboardPage
      eyebrow="MENU & PERMISSION MATRIX"
      title="菜单权限"
      description="统一维护后台导航结构与细粒度操作权限。菜单负责入口，权限点负责实际操作授权。"
      metrics={
        <>
          <DashboardMetric icon={MenuIcon} label="菜单节点" value={flatMenus.length} />
          <DashboardMetric icon={KeyRound} label="权限点" value={permissions.length} />
          <DashboardMetric
            icon={ShieldCheck}
            label="启用权限"
            value={permissions.filter((permission) => permission.status === 1).length}
          />
        </>
      }
    >
      <DashboardPanel flush>
        <Tabs
          tabBarStyle={{ padding: "0 18px", marginBottom: 0 }}
          items={[
            {
              key: "menus",
              label: "菜单结构",
              children: (
                <>
                  {hasPermission("auth:menu:create") ? (
                    <div style={{ padding: "14px 18px 0" }}>
                      <Button type="primary" icon={<Plus size={16} />} onClick={openMenuCreate}>
                        新增菜单
                      </Button>
                    </div>
                  ) : null}
                  <Table<MenuItem>
                    rowKey="id"
                    loading={loading}
                    columns={menuColumns}
                    dataSource={menus}
                    pagination={false}
                    scroll={{ x: 1080 }}
                  />
                </>
              ),
            },
            {
              key: "permissions",
              label: "权限点",
              children: (
                <>
                  {hasPermission("auth:permission:create") ? (
                    <div style={{ padding: "14px 18px 0" }}>
                      <Button
                        type="primary"
                        icon={<Plus size={16} />}
                        onClick={openPermissionCreate}
                      >
                        新增权限
                      </Button>
                    </div>
                  ) : null}
                  <Table<Permission>
                    rowKey="id"
                    loading={loading}
                    columns={permissionColumns}
                    dataSource={permissions}
                    scroll={{ x: 1250 }}
                  />
                </>
              ),
            },
          ]}
        />
      </DashboardPanel>

      <DashboardFormModal<MenuForm>
        title={editingMenu ? `编辑菜单：${editingMenu.menuName}` : "新增菜单"}
        open={menuOpen}
        form={menuForm}
        onCancel={() => setMenuOpen(false)}
        onFinish={submitMenu}
        errorMessage={editingMenu ? "菜单更新失败" : "菜单创建失败"}
      >
        <Form.Item name="parentId" label="上级菜单">
          <Select
            allowClear
            options={flatMenus
              .filter((menu) => menu.id !== editingMenu?.id)
              .map((menu) => ({ label: menu.menuName, value: menu.id }))}
          />
        </Form.Item>
        <Form.Item
          name="menuCode"
          label="菜单编码"
          rules={[{ required: true, message: "请输入菜单编码" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="menuName"
          label="菜单名称"
          rules={[{ required: true, message: "请输入菜单名称" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="routePath" label="前端路由">
          <Input placeholder="/dashboard/example" />
        </Form.Item>
        <Form.Item name="componentPath" label="组件路径">
          <Input />
        </Form.Item>
        <Form.Item name="icon" label="图标标识">
          <Input />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序">
          <Input type="number" />
        </Form.Item>
        <Form.Item name="visible" label="菜单可见性">
          <Select
            options={[
              { label: "显示", value: 1 },
              { label: "隐藏", value: 0 },
            ]}
          />
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

      <DashboardFormModal<PermissionForm>
        title={editingPermission ? `编辑权限：${editingPermission.permissionName}` : "新增权限"}
        open={permissionOpen}
        form={permissionForm}
        onCancel={() => setPermissionOpen(false)}
        onFinish={submitPermission}
        errorMessage={editingPermission ? "权限更新失败" : "权限创建失败"}
      >
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
          <Select allowClear options={requestMethodOptions} />
        </Form.Item>
        <Form.Item name="status" label="状态">
          <Select
            options={[
              { label: "启用", value: 1 },
              { label: "停用", value: 0 },
            ]}
          />
        </Form.Item>
        <Form.Item name="description" label="说明">
          <Input.TextArea rows={3} />
        </Form.Item>
      </DashboardFormModal>
    </DashboardPage>
  );
}

function flattenMenus(menus: MenuItem[]): MenuItem[] {
  return menus.flatMap((menu) => [menu, ...flattenMenus(menu.children || [])]);
}

function valueOrDash(value?: string) {
  return value || "-";
}
